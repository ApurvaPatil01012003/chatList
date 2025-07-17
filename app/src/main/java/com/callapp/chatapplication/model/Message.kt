package com.callapp.chatapplication.model

data class Message(
    val sender: String?,         // May be null for sent messages
    val messageBody: String?,
    val messageType: String,
    val timestamp: Long,
    val url: String?,
    val recipientId: String?,    // New field
    val waId: String?            // New field (user's number)
) {
    fun isSentByMe(): Boolean {
        // If recipient ID and wa_id are equal → it's sent by me
        return recipientId != null && waId != null && recipientId == waId
    }
}
