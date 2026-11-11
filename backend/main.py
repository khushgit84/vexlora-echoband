import os
from typing import Optional
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import httpx
from dotenv import load_dotenv

load_dotenv()

# Initialize FastAPI application
app = FastAPI(
    title="VEXLORA EchoBand Backend",
    version="1.0.0",
    description="Secure backend proxy interfacing with Google Gemini for VEXLORA EchoBand AI Voice Assistant",
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

# System prompt configured specifically for headphone voice interaction
SYSTEM_PROMPT = (
    "You are EchoBand, the personal AI voice assistant by VEXLORA for Sony WH-CH510 Bluetooth headphones. "
    "Rules for voice responses: "
    "1. Speak naturally in Indian Hinglish (a friendly, everyday conversational blend of Hindi and English written in Latin script, e.g. 'Haan bilkul, main abhi karta hoon!', 'Aapka message samajh aa gaya', 'Bolo bhai, kya help chahiye?') or adapt to whichever language the user speaks in. "
    "2. Avoid markdown formatting like bullet points, code blocks, or asterisks, because your words are read aloud by Text-To-Speech (TTS). "
    "3. Keep replies between 1 to 3 spoken sentences unless the user explicitly requests more details. "
    "4. Maintain a warm, futuristic, and human companion tone."
)


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, description="Recognized speech from Android client")
    conversationId: Optional[str] = Field(None, description="Optional conversation session ID")


class ChatResponse(BaseModel):
    reply: str
    conversationId: Optional[str] = None
    error: Optional[str] = None


@app.get("/")
@app.get("/api")
@app.get("/api/index")
@app.get("/api/index.py")
def root(request: Request):
    return {
        "service": "VEXLORA EchoBand AI Voice Gateway",
        "status": "online",
        "version": "1.0.0",
        "gemini_configured": bool(GEMINI_API_KEY),
        "requested_path": request.url.path,
        "headers": dict(request.headers),
    }


@app.get("/health")
@app.get("/api/health")
@app.get("/api/index/health")
def health_check():
    return {"status": "healthy"}


@app.post("/", response_model=ChatResponse)
@app.post("/api/index.py", response_model=ChatResponse)
@app.post("/api/v1/chat", response_model=ChatResponse)
@app.post("/chat", response_model=ChatResponse)
@app.post("/api/chat", response_model=ChatResponse)
@app.post("/api/index/api/v1/chat", response_model=ChatResponse)
async def chat_with_gemini(request: ChatRequest):
    if not request.message.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Message cannot be blank",
        )

    if not GEMINI_API_KEY:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="GEMINI_API_KEY is not configured on the backend server. Set it in backend/.env file.",
        )

    endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={GEMINI_API_KEY}"

    payload = {
        "system_instruction": {
            "parts": [{"text": SYSTEM_PROMPT}]
        },
        "contents": [
            {
                "role": "user",
                "parts": [{"text": request.message}]
            }
        ],
        "generationConfig": {
            "temperature": 0.7,
            "maxOutputTokens": 500,
        }
    }

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(endpoint, json=payload)

        if resp.status_code != 200:
            error_data = resp.json().get("error", {})
            err_msg = error_data.get("message", resp.text)
            raise HTTPException(
                status_code=resp.status_code,
                detail=f"Gemini API error ({resp.status_code}): {err_msg}"
            )

        data = resp.json()
        candidates = data.get("candidates", [])
        if not candidates:
            return ChatResponse(
                reply="I'm listening, but couldn't generate a response.",
                conversationId=request.conversationId,
            )

        parts = candidates[0].get("content", {}).get("parts", [])
        reply_text = parts[0].get("text", "").strip() if parts else "No reply received."

        return ChatResponse(
            reply=reply_text,
            conversationId=request.conversationId,
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Gateway error: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
