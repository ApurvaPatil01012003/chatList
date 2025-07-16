package com.callapp.chatapplication.model

data class Chat(
    val contact_name: String?,
    val wa_id_or_sender: String?,
    val message_count: Int,
    val total_pages: Int,
    val first_message_date: String?,
    val last_message_date: String?,
    val active_last_24_hours: Boolean,
    val remaining_time_seconds: Int,
    val User_ID: Int?,
    val user_name: String?,
    val remaining_time: String?
)
