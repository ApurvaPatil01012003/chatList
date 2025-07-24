package com.callapp.chatapplication.view

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.callapp.chatapplication.R
import com.callapp.chatapplication.model.Message
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private var messages: List<Message>,
    private val parentView: ViewGroup
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_TEXT_SENT = 0
        const val TYPE_TEXT_RECEIVED = 1
        const val TYPE_IMAGE_SENT = 2
        const val TYPE_IMAGE_RECEIVED = 3
        const val TYPE_TEMPLATE_IMAGE_SENT = 4
        const val TYPE_TEMPLATE_IMAGE_RECEIVED = 5
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
            message.messageType == "template" && message.isSentByMe() && !message.url.isNullOrEmpty() -> TYPE_TEMPLATE_IMAGE_SENT
            message.messageType == "template" && !message.isSentByMe() && !message.url.isNullOrEmpty() -> TYPE_TEMPLATE_IMAGE_RECEIVED
            (message.messageType == "text" || message.messageType == "template") && message.isSentByMe() -> TYPE_TEXT_SENT
            else -> TYPE_TEXT_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = when (viewType) {
            TYPE_TEXT_SENT -> R.layout.item_text_sent
            TYPE_TEXT_RECEIVED -> R.layout.item_text_received
            TYPE_IMAGE_SENT -> R.layout.item_image_sent
            TYPE_IMAGE_RECEIVED -> R.layout.item_image_received
            TYPE_TEMPLATE_IMAGE_SENT -> R.layout.item_template_image_sent
            TYPE_TEMPLATE_IMAGE_RECEIVED -> R.layout.item_template_image_received
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

            if (message.isSentByMe()) {
                val statusIcon = when {
                    message.read == 1 -> {
                        holder.tickImageView?.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.blue))
                        R.drawable.baseline_done_readall_24
                    }
                    message.delivered == 1 -> {
                        holder.tickImageView?.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.black))
                        R.drawable.baseline_done_all_24
                    }
                    message.sent == 1 -> {
                        holder.tickImageView?.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.black))
                        R.drawable.baseline_done_24
                    }
                    else -> {
                        holder.tickImageView?.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.black))
                        R.drawable.baseline_done_24
                    }
                }
                holder.tickImageView?.setImageResource(statusIcon)
            }


            Log.d("MSG_STATUS", "read=${message.read}, delivered=${message.delivered}, sent=${message.sent}")
        }

        else if (holder is ImageViewHolder) {
            holder.timestampTextView.text = formatTimestamp(message.timestamp)

            if (message.isSentByMe()) {
                val statusIcon = when {
                    message.read == 1 -> {
                        holder.tickImageView?.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.blue))
                        R.drawable.baseline_done_readall_24
                    }
                    message.delivered == 1 -> {
                        holder.tickImageView?.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.black))
                        R.drawable.baseline_done_all_24
                    }
                    message.sent == 1 -> {
                        holder.tickImageView?.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.black))
                        R.drawable.baseline_done_24
                    }
                    else -> {
                        holder.tickImageView?.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.black))
                        R.drawable.baseline_done_24
                    }
                }
                holder.tickImageView?.setImageResource(statusIcon)
            }



            val urlFromExtra = extractImageFromExtraInfo(message.extraInfo)
            val imageUrl = message.url.takeIf { !it.isNullOrEmpty() } ?: urlFromExtra.also { message.url = it }

            val templateTextView = holder.itemView.findViewById<TextView?>(R.id.imgMessage)
            val imageContainer = holder.itemView.findViewById<ViewGroup?>(R.id.templateContainer)

            if (!urlFromExtra.isNullOrEmpty()) {
                holder.imageView.visibility = View.VISIBLE
                imageContainer?.visibility = View.VISIBLE

                Glide.with(holder.itemView.context)
                    .load(urlFromExtra)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(object : com.bumptech.glide.request.target.CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: com.bumptech.glide.request.transition.Transition<in Drawable>?) {
                            holder.imageView.setImageDrawable(resource)
                            holder.imageView.visibility = View.VISIBLE
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            holder.imageView.setImageDrawable(placeholder)
                            holder.imageView.visibility = View.GONE
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            holder.imageView.setImageDrawable(errorDrawable)
                            holder.imageView.visibility = View.GONE
                        }
                    })
            } else {
                holder.imageView.setImageDrawable(null)
                holder.imageView.visibility = View.GONE
                imageContainer?.visibility = View.VISIBLE
            }

            templateTextView?.text = formatMessageText(message.messageBody ?: "")
        }
    }

    inner class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textMessage)
        val timestampTextView: TextView = view.findViewById(R.id.timestampText)
        val tickImageView: ImageView? = view.findViewById(R.id.statusTick)
    }

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView
        val timestampTextView: TextView = view.findViewById(R.id.timestampText)
        val tickImageView: ImageView? = view.findViewById(R.id.statusTick)

        init {
            imageView = view.findViewById(R.id.imageMessage)
                ?: view.findViewById(R.id.imageTemplate)
                        ?: throw IllegalStateException("ImageView not found!")
        }
    }

    fun addMessage(message: Message) {
        val newList = messages.toMutableList()
        newList.add(message)
        setMessages(newList)
    }

    fun formatMessageText(rawText: String): Spanned {
        val html = rawText
            .replace(Regex("\\*(.*?)\\*"), "<b>$1</b>")
            .replace(Regex("_(.*?)_"), "<i>$1</i>")
            .replace(Regex("~(.*?)~"), "<del>$1</del>")
            .replace("\n", "<br>")
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }

    fun formatTimestamp(timestamp: Long): String {
        val messageDate = Date(timestamp * 1000)
        val now = Date()

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val messageDay = dateFormat.format(messageDate)
        val currentDay = dateFormat.format(now)

        val calendar = java.util.Calendar.getInstance()
        calendar.time = now
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterdayDay = dateFormat.format(calendar.time)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = timeFormat.format(messageDate)

        val dayLabel = when (messageDay) {
            currentDay -> "Today"
            yesterdayDay -> "Yesterday"
            else -> {
                val fullDateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                fullDateFormat.format(messageDate)
            }
        }

        return "$time  |  $dayLabel"
    }


    fun extractImageFromExtraInfo(extraInfo: String?): String? {
        if (extraInfo.isNullOrEmpty()) return null

        return try {
            val json = JSONObject(extraInfo)
            val components = json.optJSONArray("components") ?: return null

            for (i in 0 until components.length()) {
                val component = components.getJSONObject(i)
                if (component.optString("type") == "header") {
                    val parameters = component.optJSONArray("parameters") ?: continue
                    for (j in 0 until parameters.length()) {
                        val param = parameters.getJSONObject(j)
                        if (param.optString("type") == "image") {
                            val imageObj = param.optJSONObject("image")
                            return imageObj?.optString("link")
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("EXTRACT_IMG", "Parsing error: ${e.message}")
            null
        }
    }
}
