package com.callapp.chatapplication.view


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.callapp.chatapplication.R
import com.callapp.chatapplication.controller.DisplayFullImg
import com.callapp.chatapplication.controller.DisplayFullVedio
import com.callapp.chatapplication.model.Message
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private var messages: List<Message>,
    private val parentView: ViewGroup,
    private var searchQuery: String = ""

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_TEXT_SENT = 0
        const val TYPE_TEXT_RECEIVED = 1
        const val TYPE_IMAGE_SENT = 2
        const val TYPE_IMAGE_RECEIVED = 3
        const val TYPE_TEMPLATE_IMAGE_SENT = 4
        const val TYPE_TEMPLATE_IMAGE_RECEIVED = 5
        const val TYPE_VIDEO_SENT = 6
        const val TYPE_VIDEO_RECEIVED = 7
        const val TYPE_DOCUMENT_SENT = 8
        const val TYPE_DOCUMENT_RECEIVED = 9


    }

    fun setMessages(newList: List<Message>) {
        messages = newList
        notifyDataSetChanged()
    }
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        Log.d("BINDING", "Type: ${message.messageType}, SentByMe: ${message.isSentByMe()}, URL: ${message.url}")

        return when {
            message.messageType == "image" && message.isSentByMe() -> TYPE_IMAGE_SENT
            message.messageType == "image" && !message.isSentByMe() -> TYPE_IMAGE_RECEIVED
            message.messageType == "template" && message.isSentByMe() && !message.url.isNullOrEmpty() -> TYPE_TEMPLATE_IMAGE_SENT
            message.messageType == "template" && !message.isSentByMe() && !message.url.isNullOrEmpty() -> TYPE_TEMPLATE_IMAGE_RECEIVED
            (message.messageType == "text" || message.messageType == "template") && message.isSentByMe() -> TYPE_TEXT_SENT
            message.messageType == "video" && message.isSentByMe() -> TYPE_VIDEO_SENT
            message.messageType == "video" && !message.isSentByMe() -> TYPE_VIDEO_RECEIVED
            message.messageType == "document" && message.isSentByMe() -> TYPE_DOCUMENT_SENT
            message.messageType == "document" && !message.isSentByMe() -> TYPE_DOCUMENT_RECEIVED


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
            TYPE_VIDEO_SENT -> R.layout.item_video_sent
            TYPE_VIDEO_RECEIVED -> R.layout.item_video_received
            TYPE_DOCUMENT_SENT -> R.layout.item_document_sent
            TYPE_DOCUMENT_RECEIVED -> R.layout.item_document_received

            else -> R.layout.item_text_received
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)

        return when (viewType) {
            TYPE_TEXT_SENT, TYPE_TEXT_RECEIVED -> TextViewHolder(view)
            TYPE_VIDEO_SENT, TYPE_VIDEO_RECEIVED -> VideoViewHolder(view)
            TYPE_DOCUMENT_SENT, TYPE_DOCUMENT_RECEIVED -> DocumentViewHolder(view)
            else -> ImageViewHolder(view)
        }
    }


    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder is TextViewHolder) {
            bindTextViewHolder(holder, message)
        } else if (holder is ImageViewHolder) {
            bindImageViewHolder(holder, message)
        }else if(holder is VideoViewHolder)
        {
            bindVideoViewHolder(holder, message)
        }
        else if (holder is DocumentViewHolder) {
            bindDocumentViewHolder(holder, message)
        }


    }

    private fun bindTextViewHolder(holder: TextViewHolder, message: Message) {
        holder.textView.text = formatMessageText(message.messageBody ?: "")
        holder.buttonContainer?.removeAllViews()
        holder.timestampTextView.text = formatTimestamp(message.timestamp)
        renderStatusIcon(holder.tickImageView, message, holder.itemView)

        if (message.isSentByMe()) {
            val status = getMessageStatus(message.sent, message.delivered, message.read)

            if (status == "Failed") {
                holder.tickImageView?.visibility = View.GONE
                holder.failedLabel.visibility = View.VISIBLE
                holder.failedLabel.text = status
                holder.failedLabel.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
                )
            } else {
                holder.failedLabel.visibility = View.GONE
                holder.tickImageView?.visibility = View.VISIBLE
                renderStatusIcon(holder.tickImageView, message, holder.itemView)
            }
        } else {
            holder.failedLabel.visibility = View.GONE
            holder.tickImageView?.visibility = View.GONE
        }


        renderButtons(holder.buttonContainer as LinearLayout?, message.extraInfo, message.componentData, holder.itemView.context)
    }


    private fun bindImageViewHolder(holder: ImageViewHolder, message: Message) {
        holder.timestampTextView.text = formatTimestamp(message.timestamp)
        holder.buttonContainer?.removeAllViews()
        renderStatusIcon(holder.tickImageView, message, holder.itemView)

        val context = holder.itemView.context
        val imageUrl = resolveImageUrl(message)?.also { message.url = it }

        val templateTextView = holder.itemView.findViewById<TextView?>(R.id.imgMessage)
        templateTextView?.text = formatMessageText(message.messageBody ?: "")


        if (!message.caption.isNullOrEmpty() && message.caption != "null") {
            holder.captionTextView?.text = message.caption
            holder.captionTextView?.visibility = View.VISIBLE
            holder.captionTextView?.text = formatMessageText(message.caption ?: "")
            Log.d("ADAPTER_CAPTION", "Showing caption: ${message.caption}")
        } else {
            holder.captionTextView?.visibility = View.GONE
            Log.d("ADAPTER_CAPTION", "No caption to show")
        }
        if (!imageUrl.isNullOrEmpty()) {
            holder.imageView.visibility = View.VISIBLE
            Glide.with(context).load(imageUrl).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.imageView)
        } else {
            holder.imageView.setImageDrawable(null)
            holder.imageView.visibility = View.GONE
        }
        renderButtons(holder.buttonContainer as LinearLayout?, message.extraInfo, message.componentData, holder.itemView.context)


        holder.imageView.setOnClickListener {
            if (!imageUrl.isNullOrEmpty()) {
                val intent = Intent(holder.itemView.context, DisplayFullImg::class.java).apply {
                    putExtra("image_url", imageUrl)
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        if (message.isSentByMe()) {
            val status = getMessageStatus(message.sent, message.delivered, message.read)

            if (status == "Failed") {
                holder.tickImageView?.visibility = View.GONE
                holder.failedLabel.visibility = View.VISIBLE
                holder.failedLabel.text = status
                holder.failedLabel.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
                )
            } else {
                holder.failedLabel.visibility = View.GONE
                holder.tickImageView?.visibility = View.VISIBLE
                renderStatusIcon(holder.tickImageView, message, holder.itemView)
            }
        } else {
            holder.failedLabel.visibility = View.GONE
            holder.tickImageView?.visibility = View.GONE
        }

    }

    private fun bindVideoViewHolder(holder: VideoViewHolder, message: Message) {
        holder.timestampTextView.text = formatTimestamp(message.timestamp)
        renderStatusIcon(holder.tickImageView, message, holder.itemView)

        val context = holder.itemView.context
        val videoUrl = message.url ?: return

        holder.player?.release()


        val player = ExoPlayer.Builder(context).build().also {
            holder.playerView.player = it
            val mediaItem = MediaItem.fromUri(videoUrl)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.playWhenReady = false
        }

        holder.player = player

        if (!message.caption.isNullOrEmpty() && message.caption != "null") {
            holder.captionTextView?.text = message.caption
            holder.captionTextView?.visibility = View.VISIBLE
            holder.captionTextView?.text = formatMessageText(message.caption ?: "")
            Log.d("ADAPTER_CAPTION", " caption shown: ${message.caption}")
        } else {
            holder.captionTextView?.visibility = View.GONE
        }

        holder.playerView.setOnClickListener {
            val intent = Intent(context, DisplayFullVedio::class.java).apply {
                putExtra("video_url", videoUrl)
            }
            context.startActivity(intent)
        }

        if (message.isSentByMe()) {
            val status = getMessageStatus(message.sent, message.delivered, message.read)

            if (status == "Failed") {
                holder.tickImageView?.visibility = View.GONE
                holder.failedLabel.visibility = View.VISIBLE
                holder.failedLabel.text = status
                holder.failedLabel.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
                )
            } else {
                holder.failedLabel.visibility = View.GONE
                holder.tickImageView?.visibility = View.VISIBLE
                renderStatusIcon(holder.tickImageView, message, holder.itemView)
            }
        } else {
            holder.failedLabel.visibility = View.GONE
            holder.tickImageView?.visibility = View.GONE
        }

    }
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoViewHolder) {
            holder.player?.release()
            holder.player = null
            holder.playerView.player = null
        }
    }


    private fun bindDocumentViewHolder(holder: DocumentViewHolder, message: Message) {
        holder.timestampTextView.text = formatTimestamp(message.timestamp)
        renderStatusIcon(holder.tickImageView, message, holder.itemView)

        val context = holder.itemView.context
        val docUrl = message.url ?: return

        val filename = docUrl.substringAfterLast('/')
        holder.fileName.text = filename

        if (!message.caption.isNullOrEmpty() && message.caption != "null") {
            holder.captionTextView?.text = message.caption
            holder.captionTextView?.visibility = View.VISIBLE
            holder.captionTextView?.text = formatMessageText(message.caption ?: "")
            Log.d("ADAPTER_CAPTION", "Document caption shown: ${message.caption}")
        } else {
            holder.captionTextView?.visibility = View.GONE
        }

        holder.open.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(docUrl), "application/pdf")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No app found to open document", Toast.LENGTH_SHORT).show()
            }
        }

        if (message.isSentByMe()) {
            val status = getMessageStatus(message.sent, message.delivered, message.read)

            if (status == "Failed") {
                holder.tickImageView?.visibility = View.GONE
                holder.failedLabel.visibility = View.VISIBLE
                holder.failedLabel.text = status
                holder.failedLabel.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
                )
            } else {
                holder.failedLabel.visibility = View.GONE
                holder.tickImageView?.visibility = View.VISIBLE
                renderStatusIcon(holder.tickImageView, message, holder.itemView)
            }
        } else {
            holder.failedLabel.visibility = View.GONE
            holder.tickImageView?.visibility = View.GONE
        }
    }

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.exoPlayerView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampText)
        val tickImageView: ImageView? = view.findViewById(R.id.statusTick)
        var player: ExoPlayer? = null
        val captionTextView: TextView? = view.findViewById(R.id.vedioCaption)
        val failedLabel: TextView = view.findViewById(R.id.failedLabel)
    }

    inner class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileName)
        val open : LinearLayout = view.findViewById(R.id.open)
        val timestampTextView: TextView = view.findViewById(R.id.timestampText)
        val tickImageView: ImageView? = view.findViewById(R.id.statusTick)
        val captionTextView: TextView? = view.findViewById(R.id.documentCaption)
        val failedLabel: TextView = view.findViewById(R.id.failedLabel)
    }


    private fun resolveImageUrl(message: Message): String? {
        if (!message.url.isNullOrBlank()) return message.url

        try {
            val json = JSONObject(message.extraInfo ?: "{}")
            val mediaId = json.optString("media_id")
            if (!mediaId.isNullOrBlank()) {
                val prefs = parentView.context.getSharedPreferences("image_url_cache", Context.MODE_PRIVATE)
                val cachedUrl = prefs.getString(mediaId, null)
                if (!cachedUrl.isNullOrBlank()) {
                    Log.d("IMAGE_CACHE", "Restored for template mediaId=$mediaId → $cachedUrl")
                    return cachedUrl
                }
            }

            val components = json.optJSONArray("components") ?: return null
            for (i in 0 until components.length()) {
                val component = components.getJSONObject(i)
                if (component.optString("type") == "header") {
                    val parameters = component.optJSONArray("parameters")
                    val imgObj = parameters?.optJSONObject(0)?.optJSONObject("image")
                    return imgObj?.optString("link")
                }
            }

        } catch (e: Exception) {
            Log.e("IMAGE_RESOLVE", "Failed to resolve image for template", e)
        }

        return null
    }

    private fun renderStatusIcon(tickImageView: ImageView?, message: Message, itemView: View) {
        if (message.isSentByMe()) {
            val statusIcon = when {

                message.read == 1 -> R.drawable.baseline_done_readall_24
                message.delivered == 1 -> R.drawable.baseline_done_all_24
                message.sent == 1 -> R.drawable.baseline_done_24
                else -> R.drawable.baseline_done_24
            }
            val color = if (message.read == 1) R.color.blue else R.color.black
            tickImageView?.setColorFilter(ContextCompat.getColor(itemView.context, color))
            tickImageView?.setImageResource(statusIcon)
        }
    }

    inner class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textMessage)
        val timestampTextView: TextView = view.findViewById(R.id.timestampText)
        val tickImageView: ImageView? = view.findViewById(R.id.statusTick)
        val buttonContainer: ViewGroup? = view.findViewById(R.id.buttonContainer)
        val failedLabel: TextView = view.findViewById(R.id.failedLabel)
    }

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageMessage)
            ?: view.findViewById(R.id.imageTemplate)
            ?: throw IllegalStateException("ImageView not found!")
        val timestampTextView: TextView = view.findViewById(R.id.timestampText)
        val tickImageView: ImageView? = view.findViewById(R.id.statusTick)
        var buttonContainer: ViewGroup? = view.findViewById(R.id.buttonContainer)
        val captionTextView: TextView? = view.findViewById(R.id.imageCaption)
        val failedLabel: TextView = view.findViewById(R.id.failedLabel)

    }

    fun addMessage(message: Message) {
        if (message.messageType == "template" && message.url.isNullOrEmpty()) {
            message.url = extractImageFromExtraInfo(message.extraInfo)
        }
        val newList = messages.toMutableList()
        newList.add(message)
        setMessages(newList)
    }
    fun formatMessageText(rawText: String): Spanned {
        var html = rawText
            .replace(Regex("\\*(.*?)\\*"), "<b>$1</b>")
            .replace(Regex("_(.*?)_"), "<i>$1</i>")
            .replace(Regex("~(.*?)~"), "<del>$1</del>")
            .replace("\n", "<br>")

        if (searchQuery.isNotEmpty()) {
            val escapedQuery = Regex.escape(searchQuery)
            val highlightRegex = Regex("(?i)($escapedQuery)")
            html = html.replace(highlightRegex, "<span style=\"background-color:yellow\">$1</span>")
        }

        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    }


    fun formatTimestamp(timestamp: Long): String {
        val messageDate = Date(timestamp * 1000)
        val now = Date()

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val messageDay = dateFormat.format(messageDate)
        val currentDay = dateFormat.format(now)

        val calendar = Calendar.getInstance().apply {
            time = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayDay = dateFormat.format(calendar.time)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = timeFormat.format(messageDate)

        val dayLabel = when (messageDay) {
            currentDay -> "Today"
            yesterdayDay -> "Yesterday"
            else -> SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(messageDate)
        }

        return "$time  |  $dayLabel"
    }

    fun extractImageFromExtraInfo(extraInfo: String?): String? {
        if (extraInfo.isNullOrEmpty() || extraInfo == "null") return null

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
            return null
        }
    }

    private fun createChatButton(context: Context, text: String, isLast: Boolean = false): Button {
        val button = Button(context)
        button.text = text
        button.textSize = 14f
        button.isAllCaps = false
        button.setPadding(12, 8, 12, 8)
        button.setTextColor(Color.WHITE)
        button.gravity = Gravity.CENTER
        button.background = ContextCompat.getDrawable(context, R.drawable.bg_fab_circle)

        val displayMetrics = context.resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val buttonWidth = (screenWidthPx * 0.8).toInt()
        val heightInDp = 48f
        val buttonHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            heightInDp,
            displayMetrics
        ).toInt()

        val params = LinearLayout.LayoutParams(buttonWidth, buttonHeight)
        params.gravity = Gravity.CENTER

        if (!isLast) {
            val marginBottomInDp = 8f
            params.bottomMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                marginBottomInDp,
                displayMetrics
            ).toInt()
        }

        button.layoutParams = params
        return button
    }
    private fun handleButtonClick(
        subType: String,
        parameters: JSONArray?,
        text: String,
        context: Context
    ) {
        when (subType.lowercase()) {
            "url" -> {
                val url = parameters?.optJSONObject(0)?.optString("text")
                if (!url.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            }
            "phone_number" -> {
                val phone = parameters?.optJSONObject(0)?.optString("text")
                if (!phone.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                }
            }
            "quick_reply" -> {
                Toast.makeText(context, "Quick reply: $text", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "Unsupported action", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun renderButtons(
        container: LinearLayout?,
        extraInfo: String?,
        componentData: String?,
        context: Context
    ) {
        if (container == null) return

        container.removeAllViews()

        try {
            val extraJson = if (!extraInfo.isNullOrEmpty()) JSONObject(extraInfo) else null
            val buttonsArray = extraJson?.optJSONArray("buttons")

            if (buttonsArray != null && buttonsArray.length() > 0) {
                for (i in 0 until buttonsArray.length()) {
                    val btn = buttonsArray.getJSONObject(i)
                    val type = btn.optString("type")
                    val label = btn.optString("text")
                    val param = btn.optString("param")

                    val button = createChatButton(context, label)

                    button.setOnClickListener {
                        when (type.uppercase()) {
                            "URL" -> {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(param))
                                context.startActivity(intent)
                            }
                            "PHONE_NUMBER" -> {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$param"))
                                context.startActivity(intent)
                            }
                            "QUICK_REPLY" -> {
                                Toast.makeText(context, "Quick reply: $label", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(context, "Unknown button type: $type", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    container.addView(button)
                }
                return
            }
            val components = if (!componentData.isNullOrEmpty()) JSONArray(componentData)
            else JSONObject(extraInfo ?: "{}").optJSONArray("components") ?: return


            for (i in 0 until components.length()) {
                val comp = components.getJSONObject(i)
                if (comp.optString("type") == "button") {
                    val parameters = comp.optJSONArray("parameters")
                    val subType = comp.optString("sub_type")
                    val index = comp.optInt("index", i)

                    val param = parameters?.optJSONObject(0)?.optString("text") ?: continue
                    val label = "Action ${index + 1}"

                    val button = createChatButton(context, label)

                    button.setOnClickListener {
                        handleButtonClick(subType, parameters, label, context)
                    }

                    container.addView(button)
                }

            }


        } catch (e: Exception) {
            Log.e("RENDER_BUTTONS", "Failed to render template buttons", e)
        }
    }


    private fun getMessageStatus(sent: Int?, delivered: Int?, read: Int?): String {
        return when {
            read == 1 -> "Read"
            delivered == 1 -> "Delivered"
            sent == 1 -> "Sent"
            else -> "Failed"
        }
    }


    fun getMessages(): List<Message> = messages

    private var highlightedPositions = mutableListOf<Int>()



    fun setSearchQuery(query: String) {
        searchQuery = query
        highlightedPositions.clear()
        if (query.isBlank()) { notifyDataSetChanged(); return }

        val q = query.lowercase(Locale.getDefault())
        messages.forEachIndexed { index, m ->
            val body = m.messageBody.orEmpty()
            val cap  = m.caption.orEmpty()
            val file = if (m.messageType == "document") (m.url?.substringAfterLast('/') ?: "") else ""
            val haystack = listOf(body, cap, file).filter { it.isNotBlank() }
                .joinToString("\n").lowercase(Locale.getDefault())
            if (haystack.contains(q)) highlightedPositions.add(index)
        }
        notifyDataSetChanged()
    }


    fun getHighlightedPositions(): List<Int> = highlightedPositions




    fun clearHighlights() {
        searchQuery = ""
        highlightedPositions.clear()
        notifyDataSetChanged()
    }




}
