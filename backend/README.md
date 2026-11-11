# VEXLORA EchoBand — Secure FastAPI Backend

This backend acts as the secure interface between the **VEXLORA EchoBand** Android application and the **Google Gemini API**.

## Security Architecture
- The Gemini API key is stored **exclusively** on this backend server via environment variables (`.env`).
- The Android APK contains **zero** hardcoded API keys and does not leak keys via decompilation or network inspection.
- Android sends speech text to `POST /api/v1/chat` and receives sanitized, speech-optimized text.

## Setup & Running

1. **Navigate to the backend directory**:
   ```bash
   cd backend
   ```

2. **Create a Python virtual environment & activate**:
   ```bash
   python -m venv venv
   # On Windows:
   .\venv\Scripts\activate
   # On macOS/Linux:
   source venv/bin/activate
   ```

3. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

4. **Set your Gemini API key**:
   Copy `.env.example` to `.env`:
   ```bash
   copy .env.example .env
   ```
   Add your API key from [Google AI Studio](https://aistudio.google.com/app/apikey):
   ```env
   GEMINI_API_KEY=AIzaSy...
   ```

5. **Run the server**:
   ```bash
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```
   - Swagger Documentation: `http://localhost:8000/docs`
   - Health endpoint: `http://localhost:8000/health`
