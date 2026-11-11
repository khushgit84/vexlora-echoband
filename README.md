# VEXLORA EchoBand — AI Voice Assistant (Android & Wearable Platform)

[![Install App](https://img.shields.io/badge/Web_Installer-Live_on_Vercel-00B4FF?style=for-the-badge&logo=vercel)](https://vexlora-echoband.vercel.app)
[![Download APK](https://img.shields.io/badge/Download_APK-v1.0.0_(16.4_MB)-8B5CF6?style=for-the-badge&logo=android)](https://github.com/khushgit84/vexlora-echoband/releases/download/v1.0.0/VEXLORA-EchoBand-v1.0.0.apk)
[![GitHub Release](https://img.shields.io/github/v/release/khushgit84/vexlora-echoband?style=for-the-badge&color=10B981)](https://github.com/khushgit84/vexlora-echoband/releases/tag/v1.0.0)
[![Backend Status](https://img.shields.io/badge/API_Gateway-Online-00E5A0?style=for-the-badge&logo=fastapi)](https://backend-liard-six-23.vercel.app/)

A production software release of **VEXLORA EchoBand**, an AI voice assistant designed to pair with **Sony WH-CH510 Bluetooth headphones** and any Android device. Features a futuristic cyber interface built with **Jetpack Compose**, **Material 3**, continuous **Speech Recognition**, calibrated **Indian Hinglish TextToSpeech**, native **Phone Hardware Control** (flashlight/volume/timers), a **Persistent Memory Vault**, and a serverless **Google Gemini 3.5 Flash** backend.

🌐 **Web Installer & Live Demo**: [https://vexlora-echoband.vercel.app](https://vexlora-echoband.vercel.app)  
📥 **Direct APK Download**: [VEXLORA-EchoBand-v1.0.0.apk (16.4 MB)](https://github.com/khushgit84/vexlora-echoband/releases/download/v1.0.0/VEXLORA-EchoBand-v1.0.0.apk)

---

## ⚡ Highlights & Key Capabilities

1. 🌐 **Web Installer & In-Browser Voice Simulator**
   - Live web app at [`https://vexlora-echoband.vercel.app`](https://vexlora-echoband.vercel.app) with QR code camera scanning for instant mobile install.
   - Interactive web test bench using browser Speech Recognition and Speech Synthesis talking directly to the live Google Gemini backend.
2. 🎙️ **Continuous Auto-Listening Loop**
   - Truly hands-free voice experience. Once you start speaking, EchoBand replies and immediately resumes listening so you never have to tap buttons.
3. 🇮🇳 **Real Indian Hinglish & Multi-Language Voice**
   - Speaks friendly, natural spoken Indian Hinglish (e.g. *"Haan bilkul bhai, main torch on kar raha hoon!"*) with calibrated pitch and speed.
   - Dynamic language switching: Indian English, Hindi, Tamil, Telugu, Global English, Spanish, French, and German.
4. 🔦 **Background Phone Hardware Control**
   - Command your phone naturally even while locked or running in the background:
     - Flashlight: *"Turn on torch"*, *"Flashlight band karo"* (CameraManager integration).
     - Volume: *"Volume 80% kardo"*, *"Increase volume"* (AudioManager integration).
     - Timers & Alarms: *"Set a timer for 10 minutes"* (AlarmClock integration).
5. 🧠 **Persistent Memory Vault**
   - Permanent local memory storage (`MemoryManager.kt`) that remembers your preferences, names, and notes across app restarts.
6. 🎧 **Sony WH-CH510 Optimized**
   - Native Bluetooth SCO & A2DP routing, headphone media button hook support, and real-time connection status indicators.
7. 🛡️ **Zero Hardcoded Secrets**
   - Dual-mode architecture: Use the included high-speed Vercel serverless proxy or paste your own Gemini API key directly into the app settings for unlimited requests.

---

## 📲 Quick Installation Guide

### Method A: Install via Web App (Recommended)
1. Open [**https://vexlora-echoband.vercel.app**](https://vexlora-echoband.vercel.app) on your phone or scan the QR code on your computer.
2. Tap **Download APK (v1.0.0)**.
3. When prompted, tap **Allow from this source** or **Install Anyway**.
4. Open EchoBand, grant Microphone and Camera (for flashlight) permissions, and enjoy!

### Method B: Direct APK Download
- [Download VEXLORA-EchoBand-v1.0.0.apk (16.4 MB)](https://github.com/khushgit84/vexlora-echoband/releases/download/v1.0.0/VEXLORA-EchoBand-v1.0.0.apk)

---

## 🏛 Clean Repository Architecture

```
vexlora-echoband/
├── app/                                 # Android Application (Jetpack Compose + Kotlin)
│   └── src/main/
│       ├── AndroidManifest.xml          # Background services, audio, camera, bluetooth permissions
│       ├── java/com/vexlora/echoband/
│       │   ├── VexloraApp.kt            # Application initialization
│       │   ├── MainActivity.kt          # Edge-to-edge UI entrypoint
│       │   ├── model/                   # Data models (Languages, Memories, AssistantState)
│       │   ├── service/                 # Hardware & AI Services
│       │   │   ├── SpeechRecognitionService.kt  # Continuous listening engine
│       │   │   ├── TextToSpeechService.kt       # Indian Hinglish TTS engine
│       │   │   ├── BluetoothService.kt          # Bluetooth headset connection monitor
│       │   │   ├── DeviceControlService.kt      # Native torch, volume & timer controls
│       │   │   ├── EchoBandForegroundService.kt # Background microphone service
│       │   │   ├── MemoryManager.kt             # Local JSON memory vault
│       │   │   └── AiServiceManager.kt          # Cloud backend & direct Gemini gateway
│       │   ├── ui/
│       │   │   ├── components/          # Cyber visualizer, mic button, memory drawer
│       │   │   └── screens/HomeScreen.kt# Main interactive dashboard
│       │   └── viewmodel/HomeViewModel.kt
├── backend/                             # Serverless FastAPI AI Proxy
│   ├── main.py                          # Gemini 3.5 Flash chat endpoint
│   ├── vercel.json                      # Vercel serverless deployment config
│   ├── api/index.py                     # Vercel serverless gateway wrapper
│   └── requirements.txt
└── frontend/                            # Web Landing Page & APK Installer
    ├── index.html                       # Futuristic installer & QR scanning page
    ├── styles.css                       # Cyber glassmorphic theme & neon visuals
    ├── app.js                           # Interactive online voice simulator
    ├── favicon.svg                      # Vector brand icon
    └── vercel.json                      # Vercel static edge hosting config
```

---

## 🛠️ Local Development & Build

### Android App Build
```powershell
# Build Debug APK
.\gradlew.bat assembleDebug

# Install directly to USB/Wi-Fi connected Android device
.\gradlew.bat installDebug
```

### Python Backend
```powershell
cd backend
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### Frontend Installer
```powershell
cd frontend
# Run local live-server or deploy to Vercel
vercel.cmd --prod
```

---

## 📄 License & Credits
Crafted with ❤️ for **VEXLORA** by [khushgit84](https://github.com/khushgit84). Released under the MIT License.
