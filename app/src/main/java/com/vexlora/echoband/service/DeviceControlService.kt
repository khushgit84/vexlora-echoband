package com.vexlora.echoband.service

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.AlarmClock
import com.vexlora.echoband.model.AppLanguage
import java.util.Locale
import java.util.regex.Pattern

data class CommandResult(
    val handled: Boolean,
    val spokenResponse: String,
    val languageShift: AppLanguage? = null
)

class DeviceControlService(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    var isTorchActive: Boolean = false
        private set

    // ── Torch / Flashlight Control ──

    fun setTorch(enable: Boolean): Boolean {
        if (cameraManager == null) return false
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return false

            cameraManager.setTorchMode(cameraId, enable)
            isTorchActive = enable
            true
        } catch (_: Exception) {
            false
        }
    }

    // ── Volume Control ──

    fun increaseVolume(): Int {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
        return getVolumePercent()
    }

    fun decreaseVolume(): Int {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        return getVolumePercent()
    }

    fun setVolumePercent(percent: Int): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val clamped = percent.coerceIn(0, 100)
        val target = (max * (clamped / 100.0f)).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return getVolumePercent()
    }

    fun getVolumePercent(): Int {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max == 0) return 0
        return ((current.toFloat() / max.toFloat()) * 100).toInt()
    }

    // ── Timers & Alarms ──

    fun setTimer(seconds: Int, label: String = "EchoBand Timer"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String = "EchoBand Alarm"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    // ── Natural Language Voice Command Classifier & Executor ──

    fun processCommand(
        input: String,
        memoryManager: MemoryManager,
        currentLanguage: AppLanguage
    ): CommandResult? {
        val lower = input.lowercase(Locale.ROOT).trim()
        val isHinglish = currentLanguage == AppLanguage.HINGLISH || currentLanguage == AppLanguage.HINDI

        // 0. Language Shift Commands
        if (lower.contains("speak in hinglish") || lower.contains("hinglish mein bolo") ||
            lower.contains("switch to hinglish") || lower.contains("talk in hinglish")
        ) {
            return CommandResult(
                handled = true,
                spokenResponse = "Haan bilkul! Ab main aapse Indian Hinglish mein baat karunga. Kahiye, kya madad karoon?",
                languageShift = AppLanguage.HINGLISH
            )
        }

        if (lower.contains("speak in hindi") || lower.contains("hindi mein bolo") ||
            lower.contains("switch to hindi") || lower.contains("talk in hindi")
        ) {
            return CommandResult(
                handled = true,
                spokenResponse = "हाँ बिल्कुल, अब मैं आपसे हिंदी में बात करूँगा। बताइए, मैं आपकी क्या सहायता कर सकता हूँ?",
                languageShift = AppLanguage.HINDI
            )
        }

        if (lower.contains("speak in english") || lower.contains("english mein bolo") ||
            lower.contains("switch to english") || lower.contains("talk in english")
        ) {
            return CommandResult(
                handled = true,
                spokenResponse = "Switched to Indian English. How can I assist you today?",
                languageShift = AppLanguage.ENGLISH_IN
            )
        }

        if (lower.contains("speak in tamil") || lower.contains("tamil mein bolo") || lower.contains("switch to tamil")) {
            return CommandResult(
                handled = true,
                spokenResponse = "தமிழுக்கு மாற்றப்பட்டது. நான் உங்களுக்கு எவ்வாறு உதவ முடியும்?",
                languageShift = AppLanguage.TAMIL
            )
        }

        if (lower.contains("speak in telugu") || lower.contains("telugu mein bolo") || lower.contains("switch to telugu")) {
            return CommandResult(
                handled = true,
                spokenResponse = "తెలుగులోకి మార్చబడింది. నేను మీకు ఎలా సహాయం చేయగలను?",
                languageShift = AppLanguage.TELUGU
            )
        }

        // 1. Torch / Flashlight Commands (English + Hinglish/Hindi)
        if (lower.contains("torch on") || lower.contains("turn on torch") ||
            lower.contains("flashlight on") || lower.contains("turn on flashlight") ||
            lower.contains("torch jalao") || lower.contains("light on karo") ||
            lower.contains("torch chalu karo") || lower.contains("flashlight jala do") ||
            lower.contains("turn on the torch") || lower.contains("turn on the flashlight")
        ) {
            val ok = setTorch(true)
            val reply = if (isHinglish) {
                if (ok) "Torch on kar diya hai." else "Torch on nahi ho paya."
            } else {
                if (ok) "Torch turned on." else "Unable to turn on the torch."
            }
            return CommandResult(handled = true, spokenResponse = reply)
        }

        if (lower.contains("torch off") || lower.contains("turn off torch") ||
            lower.contains("flashlight off") || lower.contains("turn off flashlight") ||
            lower.contains("torch band karo") || lower.contains("light band karo") ||
            lower.contains("torch bujhao") || lower.contains("flashlight band kar do") ||
            lower.contains("turn off the torch") || lower.contains("turn off the flashlight")
        ) {
            val ok = setTorch(false)
            val reply = if (isHinglish) {
                if (ok) "Torch band kar diya hai." else "Torch band nahi ho paya."
            } else {
                if (ok) "Torch turned off." else "Unable to turn off the torch."
            }
            return CommandResult(handled = true, spokenResponse = reply)
        }

        // 2. Volume Commands (English + Hinglish/Hindi)
        if (lower.contains("volume up") || lower.contains("increase volume") ||
            lower.contains("turn up volume") || lower.contains("louder") ||
            lower.contains("volume badhao") || lower.contains("aawaz badhao") ||
            lower.contains("volume tej karo") || lower.contains("aawaz badha do")
        ) {
            val pct = increaseVolume()
            val reply = if (isHinglish) {
                "Volume badha diya hai, abhi $pct percent hai."
            } else {
                "Volume increased to $pct percent."
            }
            return CommandResult(handled = true, spokenResponse = reply)
        }

        if (lower.contains("volume down") || lower.contains("decrease volume") ||
            lower.contains("turn down volume") || lower.contains("quieter") || lower.contains("lower volume") ||
            lower.contains("volume kam karo") || lower.contains("aawaz kam karo") ||
            lower.contains("volume ghatao") || lower.contains("aawaz dheere karo")
        ) {
            val pct = decreaseVolume()
            val reply = if (isHinglish) {
                "Volume kam kar diya hai, abhi $pct percent hai."
            } else {
                "Volume decreased to $pct percent."
            }
            return CommandResult(handled = true, spokenResponse = reply)
        }

        // Set volume percentage: "set volume to 80" or "volume 70 percent" or "volume 80 karo"
        val volPattern = Pattern.compile(".*(?:set volume to|volume to|set volume|volume)\\s+(\\d+)(?:\\s*percent|\\s*%|\\s*karo)?.*")
        val volMatcher = volPattern.matcher(lower)
        if (volMatcher.matches()) {
            val num = volMatcher.group(1)?.toIntOrNull()
            if (num != null && num in 0..100) {
                val newPct = setVolumePercent(num)
                val reply = if (isHinglish) {
                    "Volume $newPct percent par set kar diya hai."
                } else {
                    "Volume set to $newPct percent."
                }
                return CommandResult(handled = true, spokenResponse = reply)
            }
        }

        // 3. Remember Things / Facts (English + Hinglish/Hindi)
        val isRememberCmd = lower.startsWith("remember that ") || lower.startsWith("remember my ") ||
                lower.startsWith("remember to ") || lower.startsWith("remember ") ||
                lower.startsWith("please remember ") || lower.startsWith("yaad rakhna ki ") ||
                lower.startsWith("yaad rakhna ") || lower.startsWith("yaad rakho ki ") ||
                lower.startsWith("yaad rakho ")

        if (isRememberCmd) {
            val factText = input
                .replaceFirst("(?i)^please\\s+remember\\s+that\\s+", "")
                .replaceFirst("(?i)^please\\s+remember\\s+", "")
                .replaceFirst("(?i)^remember\\s+that\\s+", "")
                .replaceFirst("(?i)^remember\\s+", "")
                .replaceFirst("(?i)^yaad\\s+rakhna\\s+ki\\s+", "")
                .replaceFirst("(?i)^yaad\\s+rakhna\\s+", "")
                .replaceFirst("(?i)^yaad\\s+rakho\\s+ki\\s+", "")
                .replaceFirst("(?i)^yaad\\s+rakho\\s+", "")
                .trim()

            if (factText.isNotBlank()) {
                memoryManager.saveFact(factText)
                val reply = if (isHinglish) {
                    "Maine yaad rakh liya: $factText."
                } else {
                    "I've remembered: $factText."
                }
                return CommandResult(handled = true, spokenResponse = reply)
            }
        }

        if (lower.contains("what do you remember") || lower.contains("what do you know about me") ||
            lower.contains("list my notes") || lower.contains("kya yaad hai") ||
            lower.contains("meri notes batao") || lower.contains("kya kya yaad hai")
        ) {
            val facts = memoryManager.getFacts()
            return if (facts.isEmpty()) {
                val emptyReply = if (isHinglish) {
                    "Aapne abhi tak kuch yaad rakhne ko nahi bola hai. Boliye: 'Yaad rakhna ki...', aur main save kar loonga."
                } else {
                    "You haven't asked me to remember anything yet. Say, 'Remember that...' and I'll keep track of it."
                }
                CommandResult(handled = true, spokenResponse = emptyReply)
            } else {
                val bulletPoints = facts.take(5).joinToString(". ") { it.text }
                val reply = if (isHinglish) {
                    "Mujhe yeh baatein yaad hain: $bulletPoints."
                } else {
                    "Here is what I've remembered: $bulletPoints."
                }
                CommandResult(handled = true, spokenResponse = reply)
            }
        }

        if (lower.contains("forget all notes") || lower.contains("clear remembered things") ||
            lower.contains("forget everything i told you") || lower.contains("sab bhool jao")
        ) {
            memoryManager.clearAllFacts()
            val reply = if (isHinglish) {
                "Maine saari puraani yaadein aur notes clear kar diye hain."
            } else {
                "I've cleared all remembered notes and facts."
            }
            return CommandResult(handled = true, spokenResponse = reply)
        }

        // 4. Timer & Timing Commands (English + Hinglish/Hindi)
        // Hinglish: "5 minute ka timer lagao" / "10 second ka timer"
        val hinglishTimerPattern = Pattern.compile(".*(\\d+)\\s*(minute|second|ghante|hour)?\\s*ka timer.*")
        val hTimerMatcher = hinglishTimerPattern.matcher(lower)
        if (hTimerMatcher.matches()) {
            val qty = hTimerMatcher.group(1)?.toIntOrNull() ?: 1
            val unitStr = hTimerMatcher.group(2) ?: "minute"
            val totalSeconds = when {
                unitStr.startsWith("second") -> qty
                unitStr.startsWith("ghante") || unitStr.startsWith("hour") -> qty * 3600
                else -> qty * 60
            }
            val ok = setTimer(totalSeconds, "EchoBand Timer ($qty $unitStr)")
            val reply = if (ok) "Theek hai, $qty $unitStr ka timer set kar diya hai." else "Timer set nahi ho paya."
            return CommandResult(handled = true, spokenResponse = reply)
        }

        // English: "set a timer for 5 minutes", "timer 10 seconds"
        val timerPattern = Pattern.compile(".*timer (?:for )?(\\d+)\\s*(second|seconds|minute|minutes|hour|hours).*")
        val timerMatcher = timerPattern.matcher(lower)
        if (timerMatcher.matches()) {
            val qty = timerMatcher.group(1)?.toIntOrNull() ?: 1
            val unit = timerMatcher.group(2) ?: "minutes"
            val totalSeconds = when {
                unit.startsWith("second") -> qty
                unit.startsWith("hour") -> qty * 3600
                else -> qty * 60
            }

            val ok = setTimer(totalSeconds, "EchoBand Timer ($qty $unit)")
            return CommandResult(
                handled = true,
                spokenResponse = if (ok) "Timer set for $qty $unit." else "Unable to launch system timer."
            )
        }

        // Alarms: "set an alarm for 7 30 am" or "7 baje ka alarm lagao"
        if (lower.contains("alarm lagao") || lower.contains("alarm set karo")) {
            val numPattern = Pattern.compile(".*?(\\d{1,2})(?::(\\d{2}))?\\s*(baje|am|pm)?.*")
            val m = numPattern.matcher(lower)
            if (m.matches()) {
                var hour = m.group(1)?.toIntOrNull() ?: 7
                val min = m.group(2)?.toIntOrNull() ?: 0
                val ok = setAlarm(hour, min, "EchoBand Alarm")
                val displayMin = if (min < 10) "0$min" else "$min"
                val reply = if (isHinglish) {
                    "Subah $hour:$displayMin ka alarm set kar diya hai."
                } else {
                    "Alarm set for $hour:$displayMin."
                }
                return CommandResult(handled = true, spokenResponse = reply)
            }
        }

        val alarmPattern = Pattern.compile(".*alarm (?:for )?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?.*")
        val alarmMatcher = alarmPattern.matcher(lower)
        if (alarmMatcher.matches()) {
            var hour = alarmMatcher.group(1)?.toIntOrNull() ?: 7
            val min = alarmMatcher.group(2)?.toIntOrNull() ?: 0
            val amPm = alarmMatcher.group(3)

            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0

            val ok = setAlarm(hour, min, "EchoBand Alarm")
            val displayMin = if (min < 10) "0$min" else "$min"
            return CommandResult(
                handled = true,
                spokenResponse = if (ok) "Alarm set for $hour:$displayMin." else "Unable to launch system alarm."
            )
        }

        return null
    }
}
