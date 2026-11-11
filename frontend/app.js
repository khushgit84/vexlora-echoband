// VEXLORA EchoBand - Enhanced Logic with Apple iOS, Custom Gemini Key & Hardware Simulator

const DIRECT_APK_URL = "https://github.com/khushgit84/vexlora-echoband/releases/download/v1.0.0/VEXLORA-EchoBand-v1.0.0.apk";
const BACKEND_API_URL = "https://backend-liard-six-23.vercel.app";

// State
let customApiKey = localStorage.getItem("echoband_gemini_api_key") || "";
let memoryVault = JSON.parse(localStorage.getItem("echoband_memories") || '["Sony WH-CH510", "Hinglish Preferred"]');
let currentVolume = 65;
let isTorchOn = false;
let activeTimerInterval = null;

document.addEventListener("DOMContentLoaded", () => {
  registerServiceWorker();
  initPlatformTabs();
  initQRCode();
  initCopyButtons();
  initApiKeyVault();
  initIosModals();
  initBackendHealthCheck();
  initMemoryVaultUI();
  initChatSimulator();
});

// 0. Register PWA Service Worker
function registerServiceWorker() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("/sw.js").catch((err) => {
      console.log("Service Worker register notice:", err);
    });
  }
}

// 1. Platform Tabs (Android vs Apple iOS)
function initPlatformTabs() {
  const tabs = document.querySelectorAll(".platform-tab");
  const contents = document.querySelectorAll(".platform-content");

  // Auto-detect Apple iOS / iPadOS / macOS
  const isApple = /iPhone|iPad|iPod|Macintosh/i.test(navigator.userAgent) && !window.MSStream;
  if (isApple) {
    switchTab("appleTab");
  }

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      const targetId = tab.getAttribute("data-tab");
      switchTab(targetId);
    });
  });

  function switchTab(targetId) {
    tabs.forEach((t) => {
      t.classList.toggle("active", t.getAttribute("data-tab") === targetId);
    });
    contents.forEach((c) => {
      c.classList.toggle("active", c.id === targetId);
    });
  }
}

// 2. QR Code Generation
function initQRCode() {
  const qrImg = document.getElementById("qrCodeImg");
  if (!qrImg) return;

  const targetUrl = window.location.href.includes("vexlora") ? window.location.href : DIRECT_APK_URL;
  const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=380x380&format=svg&margin=10&data=${encodeURIComponent(targetUrl)}`;
  qrImg.src = qrUrl;

  qrImg.onerror = () => {
    qrImg.src = `https://chart.googleapis.com/chart?cht=qr&chs=380x380&chl=${encodeURIComponent(targetUrl)}`;
  };
}

// 3. Copy Link Action
function initCopyButtons() {
  const copyBtn = document.getElementById("copyLinkBtn");
  const copyText = document.getElementById("copyLinkText");
  const copyCurrentUrlBtn = document.getElementById("copyCurrentUrlBtn");
  const copySafariUrlBtn = document.getElementById("copySafariUrlBtn");

  const copyUrl = async (url, label = "Link Copied!") => {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(url);
      } else {
        const temp = document.createElement("input");
        temp.value = url;
        document.body.appendChild(temp);
        temp.select();
        document.execCommand("copy");
        document.body.removeChild(temp);
      }
      showToast("✓ " + label);
    } catch (e) {
      showToast("Link: " + url);
    }
  };

  if (copyBtn) {
    copyBtn.addEventListener("click", () => {
      copyUrl(DIRECT_APK_URL, "Direct APK Link Copied!");
      if (copyText) copyText.textContent = "Copied!";
      setTimeout(() => {
        if (copyText) copyText.textContent = "Copy Direct Link";
      }, 2500);
    });
  }

  if (copyCurrentUrlBtn) {
    copyCurrentUrlBtn.addEventListener("click", () => {
      copyUrl(window.location.href, "Web App Link Copied! Open Safari and paste.");
    });
  }

  if (copySafariUrlBtn) {
    copySafariUrlBtn.addEventListener("click", () => {
      copyUrl(window.location.href, "Safari Link Copied!");
      closeModal("iosPwaModal");
    });
  }
}

// 4. Custom Gemini API Key Vault
function initApiKeyVault() {
  const triggerBtn = document.getElementById("apiKeyModalTrigger");
  const openBannerBtn = document.getElementById("openApiKeyModalBtn");
  const modal = document.getElementById("apiKeyModal");
  const closeBtn = document.getElementById("closeApiKeyModalBtn");
  const saveBtn = document.getElementById("saveApiKeyBtn");
  const clearBtn = document.getElementById("clearApiKeyBtn");
  const input = document.getElementById("customApiKeyInput");
  const toggleVisBtn = document.getElementById("toggleKeyVisibilityBtn");
  const statusMsg = document.getElementById("keyStatusMsg");
  const pillLabel = document.getElementById("keyPillLabel");
  const subhead = document.getElementById("simulatorSubhead");

  updatePillState();

  if (triggerBtn) triggerBtn.addEventListener("click", openModal);
  if (openBannerBtn) openBannerBtn.addEventListener("click", openModal);
  if (closeBtn) closeBtn.addEventListener("click", () => closeModal("apiKeyModal"));

  if (modal) {
    modal.addEventListener("click", (e) => {
      if (e.target === modal) closeModal("apiKeyModal");
    });
  }

  if (toggleVisBtn) {
    toggleVisBtn.addEventListener("click", () => {
      input.type = input.type === "password" ? "text" : "password";
      toggleVisBtn.textContent = input.type === "password" ? "👁️" : "🙈";
    });
  }

  function openModal() {
    input.value = customApiKey;
    statusMsg.textContent = "";
    statusMsg.className = "key-status-msg";
    modal.classList.add("active");
  }

  if (saveBtn) {
    saveBtn.addEventListener("click", async () => {
      const key = input.value.trim();
      if (!key) {
        statusMsg.textContent = "Please paste a valid Gemini API key.";
        statusMsg.className = "key-status-msg error";
        return;
      }

      statusMsg.textContent = "Verifying with Google Gemini...";
      statusMsg.className = "key-status-msg";

      try {
        const res = await fetch(`${BACKEND_API_URL}/api/v1/validate-key`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ apiKey: key })
        });
        const data = await res.json();

        if (data.valid) {
          customApiKey = key;
          localStorage.setItem("echoband_gemini_api_key", key);
          statusMsg.textContent = "✓ Key Verified! Unlimited VIP quota enabled.";
          statusMsg.className = "key-status-msg success";
          updatePillState();
          showToast("⚡ Custom Gemini Key Activated!");
          setTimeout(() => closeModal("apiKeyModal"), 1200);
        } else {
          statusMsg.textContent = "✕ Verification failed: " + (data.error || "Invalid key");
          statusMsg.className = "key-status-msg error";
        }
      } catch (err) {
        // Fallback: accept key locally if network ping to validator times out
        customApiKey = key;
        localStorage.setItem("echoband_gemini_api_key", key);
        updatePillState();
        showToast("✓ Custom Key Saved Locally!");
        closeModal("apiKeyModal");
      }
    });
  }

  if (clearBtn) {
    clearBtn.addEventListener("click", () => {
      customApiKey = "";
      localStorage.removeItem("echoband_gemini_api_key");
      input.value = "";
      statusMsg.textContent = "Reset to default shared Cloud AI Gateway.";
      statusMsg.className = "key-status-msg";
      updatePillState();
      showToast("Reset to Free Cloud Gateway");
      setTimeout(() => closeModal("apiKeyModal"), 1000);
    });
  }

  function updatePillState() {
    if (customApiKey) {
      if (triggerBtn) triggerBtn.classList.add("custom-active");
      if (pillLabel) pillLabel.textContent = "VIP Custom Key";
      if (subhead) subhead.textContent = "Running on Your Personal Gemini API Key (Unlimited)";
    } else {
      if (triggerBtn) triggerBtn.classList.remove("custom-active");
      if (pillLabel) pillLabel.textContent = "Cloud Mode";
      if (subhead) subhead.textContent = "Connected to Google Gemini 3.5 Flash Gateway";
    }
  }
}

// 5. iOS Safari Modal
function initIosModals() {
  const trigger = document.getElementById("iosPwaGuideTrigger");
  const modal = document.getElementById("iosPwaModal");
  const closeBtn = document.getElementById("closeIosPwaModalBtn");

  if (trigger) trigger.addEventListener("click", () => modal.classList.add("active"));
  if (closeBtn) closeBtn.addEventListener("click", () => modal.classList.remove("active"));
  if (modal) {
    modal.addEventListener("click", (e) => {
      if (e.target === modal) modal.classList.remove("active");
    });
  }
}

function closeModal(modalId) {
  const m = document.getElementById(modalId);
  if (m) m.classList.remove("active");
}

// Toast helper
function showToast(message) {
  const toast = document.getElementById("toast");
  if (!toast) return;

  toast.textContent = message;
  toast.classList.add("show");
  setTimeout(() => {
    toast.classList.remove("show");
  }, 3200);
}

// 6. Backend Health Check
async function initBackendHealthCheck() {
  const statusPill = document.getElementById("backendStatusPill");
  if (!statusPill) return;

  try {
    const res = await fetch(`${BACKEND_API_URL}/health`);
    if (res.ok) {
      statusPill.innerHTML = `
        <span class="status-dot"></span>
        <span class="status-label">AI Gateway Online</span>
      `;
    }
  } catch (e) {
    // Silent fallback
  }
}

// 7. Memory Vault UI
function initMemoryVaultUI() {
  const container = document.getElementById("memoryChipsContainer");
  const resetBtn = document.getElementById("clearMemoryBtn");

  renderMemories();

  if (resetBtn) {
    resetBtn.addEventListener("click", () => {
      memoryVault = ["Sony WH-CH510", "Hinglish Preferred"];
      localStorage.setItem("echoband_memories", JSON.stringify(memoryVault));
      renderMemories();
      showToast("Memory Vault Reset");
    });
  }

  function renderMemories() {
    if (!container) return;
    container.innerHTML = "";
    memoryVault.forEach((mem) => {
      const chip = document.createElement("span");
      chip.className = "memory-chip";
      chip.textContent = mem;
      container.appendChild(chip);
    });
  }
}

function addMemory(fact) {
  if (!fact || memoryVault.includes(fact)) return;
  memoryVault.push(fact);
  if (memoryVault.length > 8) memoryVault.shift();
  localStorage.setItem("echoband_memories", JSON.stringify(memoryVault));
  initMemoryVaultUI();
  showToast(`🧠 Saved to Memory Vault: "${fact}"`);
}

// 8. Hardware Mockup Simulation
function triggerHardwareMockup(text) {
  const lower = text.toLowerCase();

  // Torch simulation
  if (lower.includes("torch on") || lower.includes("flashlight on") || lower.includes("torch chalao") || lower.includes("batti jalao") || lower.includes("turn on torch")) {
    setTorchState(true);
  } else if (lower.includes("torch off") || lower.includes("flashlight off") || lower.includes("torch band") || lower.includes("batti bujhao") || lower.includes("turn off torch")) {
    setTorchState(false);
  }

  // Volume simulation
  const volMatch = lower.match(/volume\s+(\d+)/i) || lower.match(/(\d+)\s*%/);
  if (volMatch && volMatch[1]) {
    setVolumeState(parseInt(volMatch[1], 10));
  } else if (lower.includes("volume badhao") || lower.includes("increase volume")) {
    setVolumeState(Math.min(100, currentVolume + 15));
  } else if (lower.includes("volume kam") || lower.includes("decrease volume")) {
    setVolumeState(Math.max(0, currentVolume - 15));
  }

  // Timer simulation
  const timerMatch = lower.match(/timer\s+(?:for\s+)?(\d+)\s*(min|minute|sec|second)/i);
  if (timerMatch && timerMatch[1]) {
    const mins = parseInt(timerMatch[1], 10);
    startVirtualTimer(mins * 60);
  }

  // Memory detection
  if (lower.includes("yaad rakhna") || lower.includes("remember that") || lower.includes("note kar")) {
    const cleanMemory = text.replace(/(yaad rakhna|remember that|note kar|please|ki)/gi, "").trim();
    if (cleanMemory.length > 4) {
      addMemory(cleanMemory);
    }
  }
}

function setTorchState(on) {
  isTorchOn = on;
  const beam = document.getElementById("virtualTorchBeam");
  const tag = document.getElementById("torchStatusTag");

  if (beam) beam.classList.toggle("active", on);
  if (tag) {
    tag.textContent = on ? "ON" : "OFF";
    tag.className = `status-tag ${on ? "on" : "off"}`;
  }
}

function setVolumeState(vol) {
  currentVolume = Math.min(100, Math.max(0, vol));
  const fill = document.getElementById("virtualVolumeFill");
  const val = document.getElementById("volumeStatusValue");

  if (fill) fill.style.width = `${currentVolume}%`;
  if (val) val.textContent = `${currentVolume}%`;
}

function startVirtualTimer(totalSeconds) {
  const row = document.getElementById("timerWidgetRow");
  const countdown = document.getElementById("virtualTimerCountdown");

  if (activeTimerInterval) clearInterval(activeTimerInterval);
  if (row) row.style.display = "flex";

  let remaining = totalSeconds;
  updateTimerText();

  activeTimerInterval = setInterval(() => {
    remaining--;
    if (remaining <= 0) {
      clearInterval(activeTimerInterval);
      if (countdown) countdown.textContent = "00:00 (Done!)";
      showToast("⏰ Timer finished!");
      return;
    }
    updateTimerText();
  }, 1000);

  function updateTimerText() {
    const m = Math.floor(remaining / 60);
    const s = remaining % 60;
    if (countdown) {
      countdown.textContent = `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
    }
  }
}

// 9. Interactive Chat & Voice Simulator
function initChatSimulator() {
  const form = document.getElementById("demoChatForm");
  const input = document.getElementById("demoMessageInput");
  const chatViewport = document.getElementById("chatViewport");
  const audioWave = document.getElementById("audioWave");
  const micBtn = document.getElementById("micToggleBtn");
  const chips = document.querySelectorAll(".prompt-chips .chip");
  const langSelect = document.getElementById("simLanguageSelect");

  let isListening = false;
  let recognition = null;

  // Initialize Speech Recognition
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.lang = langSelect ? langSelect.value : "en-IN";

    if (langSelect) {
      langSelect.addEventListener("change", () => {
        recognition.lang = langSelect.value;
      });
    }

    recognition.onstart = () => {
      isListening = true;
      micBtn.classList.add("listening");
      showToast("🎙️ Listening... Speak into your headphones/mic");
      if (audioWave) audioWave.classList.add("active");
    };

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      if (transcript && transcript.trim()) {
        input.value = transcript;
        sendMessage(transcript.trim());
      }
    };

    recognition.onerror = () => {
      stopListening();
      showToast("Speech recognition paused. Try speaking again or typing.");
    };

    recognition.onend = stopListening;

    micBtn.addEventListener("click", () => {
      if (isListening) {
        recognition.stop();
        stopListening();
      } else {
        try {
          recognition.start();
        } catch (e) {
          console.warn("Recognition error", e);
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

  // Handle Form Submit
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const query = input.value.trim();
    if (!query) return;
    sendMessage(query);
  });

  async function sendMessage(text) {
    input.value = "";
    appendMessage("user", text);

    // Reflect hardware simulation immediately on client
    triggerHardwareMockup(text);

    if (audioWave) audioWave.classList.add("active");

    const placeholder = appendMessage("ai", "EchoBand soch raha hai...");

    try {
      // Build memory context
      const memoryContext = memoryVault.length ? `[User Memories: ${memoryVault.join(", ")}] ` : "";
      const fullPrompt = `${memoryContext}${text}`;

      const requestPayload = {
        message: fullPrompt,
        apiKey: customApiKey || undefined
      };

      const response = await fetch(`${BACKEND_API_URL}/api/v1/chat`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(customApiKey ? { "x-gemini-api-key": customApiKey } : {})
        },
        body: JSON.stringify(requestPayload)
      });

      if (!response.ok) {
        throw new Error(`HTTP Error ${response.status}`);
      }

      const data = await response.json();
      const reply = data.reply || "Kuch samajh nahi aaya, please dobara bolein.";

      const contentEl = placeholder.querySelector(".bubble-content p");
      if (contentEl) contentEl.textContent = reply;

      // Also trigger hardware changes if the reply mentions it
      triggerHardwareMockup(reply);

      // Speak aloud
      speakReply(reply, langSelect ? langSelect.value : "en-IN");

    } catch (err) {
      console.error("Chat error:", err);
      const contentEl = placeholder.querySelector(".bubble-content p");
      if (contentEl) {
        contentEl.textContent = "Bhai thoda connection issue lag raha hai. Settings mein jakar apni Gemini API key daal sakte ho for unlimited quota!";
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
  function speakReply(text, langCode = "en-IN") {
    if (!("speechSynthesis" in window)) return;

    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    const voices = window.speechSynthesis.getVoices();

    const targetVoice = voices.find(v => v.lang.includes(langCode) || v.name.includes("India"));
    if (targetVoice) {
      utterance.voice = targetVoice;
    }

    utterance.pitch = 1.05;
    utterance.rate = 1.02;

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

  if ("speechSynthesis" in window) {
    window.speechSynthesis.onvoiceschanged = () => {
      window.speechSynthesis.getVoices();
    };
  }
}
