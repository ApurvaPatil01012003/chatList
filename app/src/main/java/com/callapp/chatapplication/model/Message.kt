package com.callapp.chatapplication.model

data class Message(
    val sender: String?,
    val messageBody: String?,
    val messageType: String,
    val timestamp: Long,
    val url: String?
) {
    fun isSentByMe(): Boolean {
        return !sender.isNullOrEmpty()
    }


}
