package com.callapp.chatapplication.view

import android.graphics.Bitmap
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.callapp.chatapplication.R
import com.callapp.chatapplication.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private var messages: List<Message>,
    private val parentView: ViewGroup // Needed to get context for Volley
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_TEXT_SENT = 0
        const val TYPE_TEXT_RECEIVED = 1
        const val TYPE_IMAGE_SENT = 2
        const val TYPE_IMAGE_RECEIVED = 3
    }


    fun setMessages(newList: List<Message>) {
        messages = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.messageType == "image" && message.isSentByMe() -> TYPE_IMAGE_SENT
            message.messageType == "image" && !message.isSentByMe() -> TYPE_IMAGE_RECEIVED
            message.messageType == "text" && message.isSentByMe() -> TYPE_TEXT_SENT

            else -> TYPE_TEXT_RECEIVED
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = when (viewType) {
            TYPE_TEXT_SENT -> R.layout.item_text_sent
            TYPE_TEXT_RECEIVED -> R.layout.item_text_received
            TYPE_IMAGE_SENT -> R.layout.item_image_sent
            TYPE_IMAGE_RECEIVED -> R.layout.item_image_received
            else -> R.layout.item_text_sent
        }


        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return if (viewType == TYPE_TEXT_SENT || viewType == TYPE_TEXT_RECEIVED) {
            TextViewHolder(view)
        } else {
            ImageViewHolder(view)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder is TextViewHolder) {
            holder.textView.text = formatMessageText(message.messageBody ?: "")
            holder.timestampTextView.text = formatTimestamp(message.timestamp)

        } else if (holder is ImageViewHolder) {
            val imageUrl = message.url
            val requestQueue = Volley.newRequestQueue(parentView.context)

            // Set the timestamp
            holder.timestampTextView.text = formatTimestamp(message.timestamp)

            val imageRequest = ImageRequest(
                imageUrl,
                { bitmap ->
                    holder.imageView.setImageBitmap(bitmap)
                },
                0, 0,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                { error ->
                    holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)
                }
            )

            requestQueue.add(imageRequest)
        }

    }

    inner class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textMessage)
        val timestampTextView: TextView = view.findViewById(R.id.timestampText)
    }

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageMessage)
        val timestampTextView: TextView = view.findViewById(R.id.timestampText)
    }

    fun addMessage(message: Message) {
        val newList = messages.toMutableList()
        newList.add(message)
        setMessages(newList)
    }
    fun formatMessageText(rawText: String): Spanned {
        val html = rawText
            .replace(Regex("\\*(.*?)\\*"), "<b>$1</b>")            // *bold*
            .replace(Regex("_(.*?)_"), "<i>$1</i>")                // _italic_
            .replace(Regex("~(.*?)~"), "<del>$1</del>")            // ~strikethrough~
            .replace("\n", "<br>")                                 // new lines
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp * 1000) // convert seconds to milliseconds
        val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return format.format(date)
    }

}

