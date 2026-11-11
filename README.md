# VEXLORA EchoBand — AI Voice Assistant (Android MVP)

A software prototype of **VEXLORA EchoBand**, an AI voice assistant designed to pair with **Sony WH-CH510 Bluetooth headphones**, featuring a futuristic, dark interface powered by **Jetpack Compose**, **Material 3**, native **Android Speech Recognition**, **Android TextToSpeech**, and a secure **FastAPI + Google Gemini** backend.

---

## 🎧 Hardware Target: Sony WH-CH510
- **Audio Routing**: EchoBand listens for Bluetooth audio output devices (`TYPE_BLUETOOTH_A2DP`, `TYPE_BLUETOOTH_SCO`, `TYPE_BLE_HEADSET`).
- **Hands-Free Voice Flow**: Once connected, audio playback routes directly to the Sony WH-CH510 headset drivers and microphone input.
- **Dynamic Connection Display**: Live connection status card shows connected headphone status with color coding (Green for Connected, Cyan for Searching, Rose for Disconnected).

---

## 🎨 Design & Aesthetic
- **Brand**: VEXLORA
- **Product**: EchoBand
- **Background**: Deep Black (`#050508`) & Obsidian Surface (`#12121A`)
- **Primary Accent**: Electric Blue (`#00B4FF`)
- **Secondary Accent**: Violet / Cyber Purple (`#8B5CF6`)
- **Status Accents**:
  - Ready/Idle: Obsidian Slate (`#4A4A6A`)
  - Listening: Pulsing Electric Blue (`#00B4FF`)
  - Thinking: Shimmering Violet (`#8B5CF6`)
  - Speaking: Neo-Mint Wave (`#00E5A0`)
  - Error: Coral Rose (`#FF4B6E`)
- **Cards**: Rounded cards (`18dp` - `20dp`) with glowing borders and subtle gradient fills.
- **Microphone**: Large animated button with multi-layer radial ripple and halo rings that pulse in real-time during speech capture and playback.

---

## 🏛 Clean Architecture

The application is structured into strict architectural layers:

```
vexlora-echoband/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/vexlora/echoband/
│       │   ├── VexloraApp.kt                # Application entry
│       │   ├── MainActivity.kt              # Edge-to-edge Compose host
│       │   ├── model/
│       │   │   ├── AssistantState.kt        # State enums (IDLE, LISTENING, THINKING, SPEAKING, ERROR)
│       │   │   └── ConversationMessage.kt   # Chat session history model
│       │   ├── service/
│       │   │   ├── SpeechRecognitionService.kt  # Native Android SpeechRecognizer wrapper with StateFlow
│       │   │   ├── TextToSpeechService.kt       # Native Android TTS engine with queue management
│       │   │   ├── BluetoothService.kt          # BroadcastReceiver & AudioDeviceInfo detector
│       │   │   ├── AiService.kt                 # Retrofit interface & DTOs
│       │   │   └── AiServiceManager.kt          # OkHttp client for secure backend
│       │   ├── viewmodel/
│       │   │   └── HomeViewModel.kt         # Reactive state orchestrator
│       │   └── ui/
│       │       ├── theme/
│       │       │   ├── Color.kt             # Brand color system
│       │       │   ├── Type.kt              # Futuristic typography scales
│       │       │   └── Theme.kt             # Material 3 Dark Color Scheme & System bar styling
│       │       ├── components/
│       │       │   ├── VexloraLogo.kt           # Animated glowing brand mark
│       │       │   ├── BluetoothStatusCard.kt   # Headphone connection card
│       │       │   ├── MicrophoneButton.kt      # Interactive pulsing mic control
│       │       │   ├── StatusIndicator.kt       # Listening/Thinking/Speaking badges
│       │       │   └── ConversationBubble.kt    # Styled conversation history bubbles
│       │       └── screens/
│       │           └── HomeScreen.kt            # Complete integrated dashboard
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           └── drawable/ & mipmap-anydpi-v26/   # Adaptive icons
└── backend/
    ├── main.py                              # FastAPI proxy for Google Gemini
    ├── requirements.txt
    ├── .env.example
    └── README.md
```

---

## 🔒 Security Guarantee
- **Zero API Keys in APK**: No Gemini keys are hardcoded in Kotlin or Android resource files.
- **Server-Side Key Isolation**: All Gemini requests go through the FastAPI proxy (`backend/`), which reads `GEMINI_API_KEY` from its private `.env` environment.
- **Decompilation Protection**: Even if the APK is decompiled with JADX or Apktool, no secrets can be extracted.

---

## 🚀 Running the Project

### 1. Start the Gemini Backend
```bash
cd backend
python -m venv venv
.\venv\Scripts\activate   # On Windows
pip install -r requirements.txt
copy .env.example .env     # Add your GEMINI_API_KEY
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### 2. Build and Run the Android App
```bash
# In the project root (vexlora-echoband/)
.\gradlew.bat assembleDebug

# Install on connected device or emulator:
.\gradlew.bat installDebug
```

---

## 📱 Features Included
- [x] **Animated Brand Logo**: Glowing VEXLORA EchoBand emblem.
- [x] **Sony WH-CH510 Bluetooth Status**: Real-time detection of connected Bluetooth audio peripherals.
- [x] **Interactive Microphone**: Radar-glow pulsing button supporting speech recognition start/stop.
- [x] **Assistant State Machine**: Transitions smoothly between `IDLE` -> `LISTENING` -> `THINKING` -> `SPEAKING` -> `ERROR`.
- [x] **Native Speech-to-Text**: Free-form speech input with real-time partial transcription.
- [x] **Native Text-to-Speech**: Crystal clear audio playback via Android TTS engine.
- [x] **Conversation Stream**: Distinct user & Gemini message cards with auto-scroll and clear functionality.
- [x] **Settings Modal**: Quick access to Bluetooth system settings, audio routing guide, and security architecture summary.
