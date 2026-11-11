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
    message: Optional[str] = Field(None, description="Recognized speech from Android client")
    conversationId: Optional[str] = Field(None, description="Optional conversation session ID")
    apiKey: Optional[str] = Field(None, description="Optional custom Gemini API Key from user")


class ValidateKeyRequest(BaseModel):
    apiKey: str = Field(..., min_length=1, description="Gemini API Key to test")


class ChatResponse(BaseModel):
    reply: str
    conversationId: Optional[str] = None
    error: Optional[str] = None


@app.get("/")
@app.head("/")
@app.get("/api")
@app.head("/api")
@app.get("/api/index")
@app.head("/api/index")
@app.get("/api/index.py")
@app.head("/api/index.py")
def root(request: Request):
    return {
        "service": "VEXLORA EchoBand AI Voice Gateway",
        "status": "online",
        "version": "1.0.0",
        "gemini_configured": bool(GEMINI_API_KEY),
        "frontend_installer": "https://vexlora-echoband.vercel.app",
        "apk_download": "https://github.com/khushgit84/vexlora-echoband/releases/download/v1.0.0/VEXLORA-EchoBand-v1.0.0.apk",
        "github_repo": "https://github.com/khushgit84/vexlora-echoband",
        "requested_path": request.url.path,
        "headers": dict(request.headers),
    }


@app.get("/health")
@app.head("/health")
@app.get("/api/health")
@app.head("/api/health")
@app.get("/api/index/health")
@app.head("/api/index/health")
def health_check():
    return {"status": "healthy"}


@app.post("/api/v1/validate-key")
@app.post("/validate-key")
async def validate_key(req: ValidateKeyRequest):
    key = (req.apiKey or "").strip()
    if not key:
        raise HTTPException(status_code=400, detail="Key cannot be empty")
    test_url = f"https://generativelanguage.googleapis.com/v1beta/models?key={key}"
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(test_url)
        if resp.status_code == 200:
            return {"valid": True, "message": "Key successfully verified with Google Gemini!"}
        return {"valid": False, "error": f"Google returned code {resp.status_code}"}
    except Exception as e:
        return {"valid": False, "error": str(e)}


@app.post("/")
@app.post("/api/index.py")
@app.post("/api/v1/chat")
@app.post("/chat")
@app.post("/api/chat")
@app.post("/api/index/api/v1/chat")
async def chat_with_gemini(request: ChatRequest, raw_req: Request):
    path = (raw_req.headers.get("x-matched-path", "") or raw_req.url.path).lower()
    
    # Check if this request is intended for key validation
    if "validate-key" in path or (request.apiKey and not request.message):
        key = (request.apiKey or "").strip()
        if not key:
            return {"valid": False, "error": "API key cannot be empty"}
        test_url = f"https://generativelanguage.googleapis.com/v1beta/models?key={key}"
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(test_url)
            if resp.status_code == 200:
                return {"valid": True, "message": "Key successfully verified with Google Gemini!"}
            return {"valid": False, "error": f"Google returned code {resp.status_code}"}
        except Exception as e:
            return {"valid": False, "error": str(e)}

    # Standard Chat Execution
    if not request.message or not request.message.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Message cannot be blank",
        )

    active_key = (request.apiKey or "").strip() or raw_req.headers.get("x-gemini-api-key") or GEMINI_API_KEY
    if not active_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="GEMINI_API_KEY is not configured. Please supply your Gemini API key.",
        )

    endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={active_key}"

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
