package com.callapp.chatapplication.view

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.callapp.chatapplication.R
import com.callapp.chatapplication.model.Chat

class ChatAdapter(
    private var chats: List<Chat>,
    private val onItemClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var originalList = chats.toMutableList()
    private var filteredList = chats.toMutableList()

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtInitial: TextView = itemView.findViewById(R.id.txtInitial)
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtContactNumber: TextView = itemView.findViewById(R.id.txtContactNumber)
        val txtMsgCnt: TextView = itemView.findViewById(R.id.txtMsgCnt)
        val activeDot : View = itemView.findViewById(R.id.activeDot)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(filteredList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_log, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = filteredList[position]

        val name = chat.contact_name?.trim().orEmpty()
        holder.txtName.text = name
        holder.txtContactNumber.text = chat.wa_id_or_sender ?: "-"
        holder.txtMsgCnt.text = "${chat.message_count} Messages"
        holder.txtInitial.text = getInitialsFromName(name)
        holder.activeDot.visibility = if (chat.active_last_24_hours) View.VISIBLE else View.GONE
        if (chat.active_last_24_hours){
            holder.txtInitial.setBackgroundResource(R.drawable.bg_circle_initial)
        }
        else
        {
            holder.txtInitial.setBackgroundResource(R.drawable.bg_circle_initial_gray)
        }


    }

    override fun getItemCount(): Int = filteredList.size

    fun updateList(newList: List<Chat>) {
        chats = newList
        originalList = newList.toMutableList()
        filteredList = newList.toMutableList()
        newList.forEach {
            Log.d("ChatAdapter", "Chat item: ${it.contact_name} | ${it.wa_id_or_sender}")
        }

        notifyDataSetChanged()
    }


    fun filterList(query: String) {
        val trimmedQuery = query.trim().lowercase()

        filteredList = if (trimmedQuery.isBlank()) {
            originalList.toMutableList()
        } else {
            originalList.filter {
                val name = it.contact_name?.trim()?.lowercase().orEmpty()
                val number = it.wa_id_or_sender?.trim()?.lowercase().orEmpty()
                name.contains(trimmedQuery) || number.contains(trimmedQuery)
            }.toMutableList()
        }

        notifyDataSetChanged()
    }


    private fun getInitialsFromName(name: String): String {
        val initials = name.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.first().uppercaseChar() }
            .joinToString("")
        return if (initials.isNotEmpty()) initials else "?"
    }
}
