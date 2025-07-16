package com.callapp.chatapplication.controller

import com.android.volley.Request

import android.content.Context
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.callapp.chatapplication.model.Chat

class ChatController(private val context: Context) {

    fun fetchChats(
        phoneNumberId: String,
        accessToken: String,
        onSuccess: (List<Chat>) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "https://waba.mpocket.in/api/phone/get/chats/$phoneNumberId?accessToken=$accessToken"

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                val chatList = mutableListOf<Chat>()
                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)
                    val chat = Chat(
                        contact_name = obj.getString("contact_name"),
                        wa_id_or_sender = obj.getString("wa_id_or_sender"),
                        message_count = obj.getInt("message_count"),
                        total_pages = obj.getInt("total_pages"),
                        first_message_date = obj.getString("first_message_date"),
                        last_message_date = obj.getString("last_message_date"),
                        active_last_24_hours = obj.getBoolean("active_last_24_hours"),
                        remaining_time_seconds = obj.getInt("remaining_time_seconds"),
                        User_ID = if (obj.isNull("User_ID")) null else obj.getInt("User_ID"),
                        user_name = if (obj.isNull("user_name")) null else obj.getString("user_name"),
                        remaining_time = obj.getString("remaining_time")
                    )
                    chatList.add(chat)
                }
                onSuccess(chatList)
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                val errBody = error.networkResponse?.data?.let { String(it) }

                val detailedMessage = if (!errBody.isNullOrEmpty()) {
                    "HTTP $statusCode → $errBody"
                } else {
                    error.message ?: "Unknown error"
                }

                error.printStackTrace()
                onError(detailedMessage)
            })

        Volley.newRequestQueue(context).add(request)
    }

}
