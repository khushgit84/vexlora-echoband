package com.vexlora.echoband.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.vexlora.echoband.model.BluetoothState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothService(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val _connectionState = MutableStateFlow(BluetoothState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothState> = _connectionState.asStateFlow()

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkBluetoothAudio()
        }
    }

    fun register() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
                addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
                addAction(AudioManager.ACTION_HEADSET_PLUG)
            }
            context.registerReceiver(receiver, filter)
            isReceiverRegistered = true
        }
        checkBluetoothAudio()
    }

    fun unregister() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Ignore if not registered
            }
            isReceiverRegistered = false
        }
    }

    fun checkBluetoothAudio() {
        try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val btDevice = outputs.firstOrNull { info ->
                info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        info.type == AudioDeviceInfo.TYPE_HEARING_AID ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                info.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
            }

            if (btDevice != null) {
                _connectionState.value = BluetoothState.CONNECTED
                val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val rawName = btDevice.productName?.toString()?.trim()
                    if (!rawName.isNullOrEmpty()) rawName else "Sony WH-CH510"
                } else {
                    "Sony WH-CH510"
                }
                _deviceName.value = name
            } else {
                // If bluetooth adapter is disabled or no audio device active
                _connectionState.value = BluetoothState.DISCONNECTED
                _deviceName.value = null
            }
        } catch (_: SecurityException) {
            _connectionState.value = BluetoothState.DISCONNECTED
            _deviceName.value = null
        } catch (_: Exception) {
            _connectionState.value = BluetoothState.DISCONNECTED
            _deviceName.value = null
        }
    }
}
