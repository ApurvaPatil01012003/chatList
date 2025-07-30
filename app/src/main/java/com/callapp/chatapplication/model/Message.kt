package com.callapp.chatapplication.model
data class Message(
    val sender: String?,
    val messageBody: String?,
    val messageType: String,
    val timestamp: Long,
    var url: String?,
    val recipientId: String?,
    val waId: String?,
    val status: String? = null,
    var sent: Int = 0,
    var delivered: Int = 0,
    var read: Int = 0,
    val extraInfo: String? = null,
    val componentData: String? = null
)
{
    fun isSentByMe(): Boolean {
        return recipientId != null && waId != null && recipientId == waId
    }
}
