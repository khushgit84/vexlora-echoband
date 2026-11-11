// VEXLORA EchoBand - Frontend Logic & Interactive Simulator

const DIRECT_APK_URL = "https://github.com/khushgit84/vexlora-echoband/releases/download/v1.0.0/VEXLORA-EchoBand-v1.0.0.apk";
const BACKEND_API_URL = "https://backend-liard-six-23.vercel.app";

document.addEventListener("DOMContentLoaded", () => {
  initQRCode();
  initCopyLink();
  initBackendHealthCheck();
  initChatSimulator();
});

// 1. QR Code Generation
function initQRCode() {
  const qrImg = document.getElementById("qrCodeImg");
  if (!qrImg) return;

  // Use high-resolution QR service with high error correction
  const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=380x380&format=svg&margin=10&data=${encodeURIComponent(DIRECT_APK_URL)}`;
  qrImg.src = qrUrl;

  qrImg.onerror = () => {
    // Fallback to Google Chart QR API if primary fails
    qrImg.src = `https://chart.googleapis.com/chart?cht=qr&chs=380x380&chl=${encodeURIComponent(DIRECT_APK_URL)}`;
  };
}

// 2. Copy Download Link with Toast
function initCopyLink() {
  const copyBtn = document.getElementById("copyLinkBtn");
  const copyText = document.getElementById("copyLinkText");

  if (!copyBtn) return;

  copyBtn.addEventListener("click", async () => {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(DIRECT_APK_URL);
      } else {
        const tempInput = document.createElement("input");
        tempInput.value = DIRECT_APK_URL;
        document.body.appendChild(tempInput);
        tempInput.select();
        document.execCommand("copy");
        document.body.removeChild(tempInput);
      }

      showToast("✓ APK Download Link Copied to Clipboard!");
      if (copyText) copyText.textContent = "Copied!";
      setTimeout(() => {
        if (copyText) copyText.textContent = "Copy Direct APK Link";
      }, 2500);
    } catch (err) {
      console.error("Copy failed", err);
      showToast("Link: " + DIRECT_APK_URL);
    }
  });
}

// Toast notification helper
function showToast(message) {
  const toast = document.getElementById("toast");
  if (!toast) return;

  toast.textContent = message;
  toast.classList.add("show");
  setTimeout(() => {
    toast.classList.remove("show");
  }, 3000);
}

// 3. Backend Health Check
async function initBackendHealthCheck() {
  const statusPill = document.getElementById("backendStatusPill");
  if (!statusPill) return;

  try {
    const res = await fetch(`${BACKEND_API_URL}/`, { method: "GET" });
    if (res.ok) {
      const data = await res.json();
      if (data.status === "online") {
        statusPill.innerHTML = `
          <span class="status-dot"></span>
          <span class="status-label">Cloud AI Online (${data.service || "Gemini 3.5"})</span>
        `;
        statusPill.style.borderColor = "rgba(16, 185, 129, 0.4)";
      }
    }
  } catch (err) {
    console.warn("Backend ping notice:", err);
    // Silent fallback keeps the default pill
  }
}

// 4. Interactive Chat & Voice Simulator
function initChatSimulator() {
  const form = document.getElementById("demoChatForm");
  const input = document.getElementById("demoMessageInput");
  const chatViewport = document.getElementById("chatViewport");
  const audioWave = document.getElementById("audioWave");
  const micBtn = document.getElementById("micToggleBtn");
  const chips = document.querySelectorAll(".prompt-chips .chip");

  let isListening = false;
  let recognition = null;

  // Initialize Web Speech Recognition if available
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.lang = "en-IN"; // Default to Indian English / Hinglish speech recognition

    recognition.onstart = () => {
      isListening = true;
      micBtn.classList.add("listening");
      showToast("🎙️ Listening... speak now");
      if (audioWave) audioWave.classList.add("active");
    };

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      if (transcript && transcript.trim()) {
        input.value = transcript;
        sendMessage(transcript.trim());
      }
    };

    recognition.onerror = (e) => {
      console.warn("Speech recognition error:", e);
      stopListening();
      showToast("Could not capture audio. Please type your message.");
    };

    recognition.onend = () => {
      stopListening();
    };

    micBtn.addEventListener("click", () => {
      if (isListening) {
        recognition.stop();
        stopListening();
      } else {
        try {
          recognition.start();
        } catch (e) {
          console.warn("Recognition start error", e);
        }
      }
    });
  } else {
    micBtn.addEventListener("click", () => {
      showToast("Speech Recognition not supported in this browser. Please type!");
      input.focus();
    });
  }

  function stopListening() {
    isListening = false;
    micBtn.classList.remove("listening");
    if (audioWave) audioWave.classList.remove("active");
  }

  // Handle Quick Chips
  chips.forEach((chip) => {
    chip.addEventListener("click", () => {
      const prompt = chip.getAttribute("data-prompt");
      if (prompt) {
        input.value = prompt;
        sendMessage(prompt);
      }
    });
  });

  // Handle Form Submission
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const query = input.value.trim();
    if (!query) return;
    sendMessage(query);
  });

  async function sendMessage(text) {
    input.value = "";
    appendMessage("user", text);

    if (audioWave) audioWave.classList.add("active");

    // Add typing placeholder
    const placeholder = appendMessage("ai", "EchoBand soch raha hai...");

    try {
      const response = await fetch(`${BACKEND_API_URL}/api/v1/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: text })
      });

      if (!response.ok) {
        throw new Error(`HTTP Error ${response.status}`);
      }

      const data = await response.json();
      const reply = data.reply || "Kuch samajh nahi aaya, please dobara bolein.";

      // Update placeholder
      const contentEl = placeholder.querySelector(".bubble-content p");
      if (contentEl) contentEl.textContent = reply;

      // Speak aloud with TextToSpeech
      speakReply(reply);

    } catch (err) {
      console.error("Chat error:", err);
      const contentEl = placeholder.querySelector(".bubble-content p");
      if (contentEl) {
        contentEl.textContent = "Bhai connection mein thoda issue aaya. Vercel backend online hai, ek baar refresh karke try karo.";
      }
    } finally {
      if (audioWave && !window.speechSynthesis?.speaking) {
        audioWave.classList.remove("active");
      }
    }
  }

  function appendMessage(sender, text) {
    const bubble = document.createElement("div");
    bubble.className = `chat-bubble ${sender === "user" ? "user-bubble" : "ai-bubble"}`;

    const avatar = document.createElement("div");
    avatar.className = "bubble-avatar";
    avatar.textContent = sender === "user" ? "YOU" : "EB";

    const content = document.createElement("div");
    content.className = "bubble-content";
    const p = document.createElement("p");
    p.textContent = text;
    content.appendChild(p);

    bubble.appendChild(avatar);
    bubble.appendChild(content);

    chatViewport.appendChild(bubble);
    chatViewport.scrollTop = chatViewport.scrollHeight;

    return bubble;
  }

  // Text-To-Speech Output
  function speakReply(text) {
    if (!("speechSynthesis" in window)) return;

    window.speechSynthesis.cancel(); // Stop any pending speech

    const utterance = new SpeechSynthesisUtterance(text);
    const voices = window.speechSynthesis.getVoices();

    // Prefer Indian English (en-IN) or Hindi (hi-IN) voice
    const indianVoice = voices.find(v => v.lang.includes("en-IN") || v.lang.includes("hi-IN") || v.name.includes("India"));
    if (indianVoice) {
      utterance.voice = indianVoice;
    }

    utterance.pitch = 1.05;
    utterance.rate = 1.0;

    utterance.onstart = () => {
      if (audioWave) audioWave.classList.add("active");
    };

    utterance.onend = () => {
      if (audioWave) audioWave.classList.remove("active");
    };

    utterance.onerror = () => {
      if (audioWave) audioWave.classList.remove("active");
    };

    window.speechSynthesis.speak(utterance);
  }

  // Pre-load voices for Chrome
  if ("speechSynthesis" in window) {
    window.speechSynthesis.onvoiceschanged = () => {
      window.speechSynthesis.getVoices();
    };
  }
}
