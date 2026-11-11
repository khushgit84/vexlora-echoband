package com.vexlora.echoband.model

import java.util.UUID

data class ConversationMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
