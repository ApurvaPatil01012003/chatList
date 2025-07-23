package com.callapp.chatapplication.model

data class Message(
    val sender: String?,
    val messageBody: String?,
    val messageType: String,
    val timestamp: Long,
    val url: String?,
    val recipientId: String?,
    val waId: String?,
    val status: String? = null,
    val sent: Int = 0,
    val delivered: Int = 0,
    val read: Int = 0,
    val extraInfo: String? = null


) {
    fun isSentByMe(): Boolean {
        return recipientId != null && waId != null && recipientId == waId
    }
}
