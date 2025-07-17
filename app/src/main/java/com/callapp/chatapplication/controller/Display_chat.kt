package com.callapp.chatapplication.controller

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.callapp.chatapplication.R
import com.callapp.chatapplication.databinding.ActivityDisplayChatBinding
import com.callapp.chatapplication.model.Message
import com.callapp.chatapplication.view.MessageAdapter
import org.json.JSONArray
import org.json.JSONObject

class Display_chat : AppCompatActivity() {
    private val templateBodyMap = mutableMapOf<String, String>()
    private lateinit var binding: ActivityDisplayChatBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDisplayChatBinding.inflate(layoutInflater)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val name = intent.getStringExtra("contact_name")
        var number = intent.getStringExtra("wa_id_or_sender")
        setSupportActionBar(binding.toolbar)


        supportActionBar?.title = "$name"
        supportActionBar?.subtitle = "$number"

        recyclerView = binding.recyclerViewMessages
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(emptyList(), binding.recyclerViewMessages)
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMessages.adapter = adapter


        binding.buttonSend.setOnClickListener {
            val text = binding.editTextMessage.text.toString().trim()
            val currentContactWaId = intent.getStringExtra("wa_id_or_sender") ?: ""

            val phoneNumberId = "361462453714220"
            val accessToken = "Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"

            if (text.isNotEmpty()) {
                sendTextMessage(
                    to = currentContactWaId,
                    text = text,
                    phoneNumberId = phoneNumberId,
                    accessToken = accessToken,
                    onSuccess = {
                        Toast.makeText(this, "Message Sent", Toast.LENGTH_SHORT).show()
                        binding.editTextMessage.setText("")
                        val sentMessage = Message(
                            sender = "you",
                            messageBody = text,
                            messageType = "text",
                            timestamp = System.currentTimeMillis() / 1000,
                            url = null,
                            recipientId = currentContactWaId,
                            waId = currentContactWaId
                        )

                        adapter.addMessage(sentMessage)
                        binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)

                    },
                    onError = {
                        Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
        number = intent.getStringExtra("wa_id_or_sender") ?: ""

        if (number.isNotEmpty()) {
            val url = "https://waba.mpocket.in/api/phone/get/361462453714220/$number/1"

            val request = JsonArrayRequest(Request.Method.GET, url, null,
                { response ->
                    val messages = mutableListOf<Message>()
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)

                        val messageType = obj.optString("message_type") ?: "text"
                        //val messageBody = obj.optString("message_body")

                        val messageBody = when (messageType) {
                            "template" -> {
                                val extraInfoStr = obj.optString("extra_info")
                                if (extraInfoStr.isNotEmpty()) {
                                    try {
                                        val extraInfo = JSONObject(extraInfoStr)
                                        val name = extraInfo.optString("name")
                                        val components = extraInfo.optJSONArray("components")

                                        // Get the body component
                                        val body = (0 until (components?.length() ?: 0))
                                            .mapNotNull { components?.optJSONObject(it) }
                                            .firstOrNull { it.optString("type") == "body" }

                                        val params = body?.optJSONArray("parameters")
                                        val values = (0 until (params?.length() ?: 0)).map { i ->
                                            params!!.getJSONObject(i).optString("text")
                                        }

                                        // Now get original body text from templates map
                                        val templateText = templateBodyMap[name] ?: name

                                        // Replace {{1}}, {{2}}, ... with values
                                        var rendered = templateText
                                        values.forEachIndexed { index, v ->
                                            rendered = rendered.replace("{{${index + 1}}}", v)
                                        }
                                        rendered
                                    } catch (e: Exception) {
                                        "Template Message"
                                    }
                                } else {
                                    "Template Message"
                                }
                            }

                            else -> obj.optString("message_body", "")
                        }

                        val imageUrl = obj.optString("url").ifEmpty { obj.optString("file_url") }
                        val timestamp = obj.optLong("timestamp", System.currentTimeMillis() / 1000)

                        if (messageBody.isNullOrEmpty() && messageType != "image") {
                            continue
                        }
                        val sender = obj.optString("sender")
                        val recipientId = obj.optString("recipient_id")
                        val waId = obj.optString("wa_id")

                        val message = Message(
                            sender = if (sender.isNullOrEmpty()) null else sender,
                            messageBody = messageBody,
                            messageType = messageType,
                            timestamp = timestamp,
                            url = imageUrl,
                            recipientId = recipientId,
                            waId = waId
                        )


                        messages.add(message)
                    }

                    // Show latest messages at bottom
                    messages.reverse()
                    adapter.setMessages(messages)

                    // Smooth scroll
                    binding.recyclerViewMessages.post {
                        binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                    }
                },
                { error ->
                    Log.e("Volley", "Error loading messages", error)
                    Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show()
                })

            Volley.newRequestQueue(this).add(request)
        } else {
            Toast.makeText(this, "Invalid contact selected.", Toast.LENGTH_SHORT).show()
            finish()
        }

        fetchTemplates(
            onResult = {
                loadMessages()
            },
            onError = {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                loadMessages()
            }
        )
        binding.btnTemplateAdd.setOnClickListener {
            fetchTemplates(onResult = { templateList ->
                showTemplateDialog(templateList)

            }, onError = {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()

            })
        }




    }


    fun sendTextMessage(
        to: String,
        text: String,
        phoneNumberId: String,
        accessToken: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "https://waba.mpocket.in/api/$phoneNumberId/messages"

        val jsonBody = JSONObject().apply {
            put("messaging_product", "whatsapp")
            put("to", to)
            put("type", "text")
            put("text", JSONObject().put("body", text))
        }

        val request = object : JsonObjectRequest(
            Method.POST, url, jsonBody,
            { response ->
                Log.d("SEND_MSG", "Message sent: $response")
                onSuccess()
            },
            { error ->
                val errData = error.networkResponse?.data?.let { String(it) }
                Log.e("SEND_MSG", "Failed: $errData")

                if (errData?.contains("131047") == true) {
                    onError("24-hour window expired. Use a template message.")
                } else {
                    onError("Failed: $errData")
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $accessToken"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)

    }

    fun fetchTemplates(onResult: (List<JSONObject>) -> Unit, onError: (String) -> Unit) {
        val url =
            "https://waba.mpocket.in/api/phone/get/message_templates/361462453714220?accessToken=Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val templates = response.optJSONArray("data") ?: response.optJSONArray("templates")

                templateBodyMap.clear()

                val result = mutableListOf<JSONObject>()

                val templatesLength = templates?.length() ?: 0
                for (i in 0 until templatesLength) {
                    val template = templates?.getJSONObject(i) ?: continue
                    val name = template.optString("name")
                    val componentsStr = template.optString("components")

                    try {
                        val components = JSONArray(componentsStr)
                        for (j in 0 until components.length()) {
                            val comp = components.getJSONObject(j)
                            if (comp.optString("type") == "BODY") {
                                val bodyText = comp.optString("text")
                                templateBodyMap[name] = bodyText
                            }
                        }

                        result.add(template)
                    } catch (e: Exception) {
                        Log.e("TEMPLATE_PARSE", "Error parsing components for $name", e)
                    }
                }

                onResult(result) // result can be used for template dialog
                loadMessages()
            },
            { error ->
                onError(error.message ?: "Template fetch failed")
                loadMessages() // fallback load even if templates failed
            }
        )

        Volley.newRequestQueue(this).add(request)
    }


    fun showTemplateDialog(templateList: List<JSONObject>) {
        val dialogView = layoutInflater.inflate(R.layout.message_template, null)
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val spinnerTemplates = dialogView.findViewById<Spinner>(R.id.spinnerTemplates)
        val buttonSend = dialogView.findViewById<Button>(R.id.buttonSend)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val inputFields = dialogView.findViewById<LinearLayout>(R.id.inputFieldsLayout)

        val templateNames = templateList.map { it.optString("name") }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, templateNames)
        spinnerTemplates.adapter = adapter

        var selectedTemplate: JSONObject? = null
        var userInputs = mutableListOf<String>()

        spinnerTemplates.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedTemplate = templateList[pos]
                inputFields.removeAllViews()
                userInputs.clear()

                selectedTemplate?.let { selected ->
                    renderTemplateUI(selected, inputFields, this@Display_chat, userInputs)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        buttonCancel.setOnClickListener { alertDialog.dismiss() }

        buttonSend.setOnClickListener {
            selectedTemplate?.let {
                sendTemplateMessage(it, userInputs)
            }
            alertDialog.dismiss()
        }

        alertDialog.show()

        // This must come after .show()
        alertDialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        alertDialog.window?.decorView?.post {
            val firstInput = (0 until inputFields.childCount)
                .mapNotNull { inputFields.getChildAt(it) as? EditText }
                .firstOrNull()

            firstInput?.let {
                it.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    fun sendTemplateMessage(template: JSONObject, inputs: List<String>) {
        val name = template.optString("name")
        val to = intent.getStringExtra("wa_id_or_sender") ?: return

        if (inputs.any { it.isBlank() }) {
            Toast.makeText(this, "Please fill all template fields", Toast.LENGTH_SHORT).show()
            return
        }

        val componentsArray = JSONArray()

        val hasImageHeader = template.optString("components").contains("\"type\":\"HEADER\"") &&
                template.optString("components").contains("\"format\":\"IMAGE\"")

        // ✅ Add HEADER with image if required
        val componentsStr = template.optString("components")
        val componentsArr = JSONArray(componentsStr)
        var headerNeedsImageUpload = false

        for (i in 0 until componentsArr.length()) {
            val comp = componentsArr.optJSONObject(i)
            if (comp.optString("type") == "HEADER" && comp.optString("format") == "IMAGE") {
                val example = comp.optJSONObject("example")
                val exampleUrls = example?.optJSONArray("header_handle")
                if (exampleUrls == null || exampleUrls.length() == 0) {
                    headerNeedsImageUpload = true
                }
                break
            }
        }

        if (headerNeedsImageUpload) {
            val headerParams = JSONArray()
            val imageUrl = inputs.getOrNull(0) ?: ""

            if (!imageUrl.startsWith("http")) {
                Toast.makeText(this, "Image URL required (valid HTTPS)", Toast.LENGTH_SHORT).show()
                return
            }

            headerParams.put(JSONObject().apply {
                put("type", "image")
                put("image", JSONObject().put("link", imageUrl))
            })

            componentsArray.put(JSONObject().apply {
                put("type", "header")
                put("parameters", headerParams)
            })
        }


        // ✅ Add BODY parameters (adjust for image header shift)
        val bodyParams = JSONArray()
        val bodyStartIndex = if (hasImageHeader) 1 else 0

        for (i in bodyStartIndex until inputs.size) {
            bodyParams.put(JSONObject().apply {
                put("type", "text")
                put("text", inputs[i])
            })
        }

        componentsArray.put(JSONObject().apply {
            put("type", "body")
            put("parameters", bodyParams)
        })

        // ✅ Final Payload
        val payload = JSONObject().apply {
            put("messaging_product", "whatsapp")
            put("to", to)
            put("type", "template")
            put("template", JSONObject().apply {
                put("name", name)
                put("language", JSONObject().put("code", "en"))
                put("components", componentsArray)
            })
        }

        Log.d("TEMPLATE_SEND", "Payload: $payload")

        // ✅ Rendered preview message (for local UI)
        val templateText = templateBodyMap[name] ?: name
        var rendered = templateText
        val bodyInputs = inputs.drop(if (hasImageHeader) 1 else 0)
        bodyInputs.forEachIndexed { index, value ->
            rendered = rendered.replace("{{${index + 1}}}", value)
        }

        val sentMessage = Message(
            sender = null,
            messageBody = rendered,
            messageType = "template",
            timestamp = System.currentTimeMillis() / 1000,
            url = if (hasImageHeader) inputs.firstOrNull() else null,
            recipientId = to,
            waId = to
        )

        loadMessages() // Optionally show preview immediately

        val url = "https://waba.mpocket.in/api/361462453714220/messages"
        val request = object : JsonObjectRequest(Method.POST, url, payload,
            { response ->
                Log.d("TEMPLATE_SEND", "Success: $response")
                Toast.makeText(this, "Template sent", Toast.LENGTH_SHORT).show()
            },
            { error ->
                val errData = error.networkResponse?.data?.let { String(it) }
                Log.e("TEMPLATE_SEND", "Error: $errData")
                Toast.makeText(this, "Send failed: $errData", Toast.LENGTH_LONG).show()
                loadMessages()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "Authorization" to "Bearer Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }




    fun renderTemplateUI(
        template: JSONObject,
        layout: LinearLayout,
        context: Context,
        userInputs: MutableList<String>
    ) {
        layout.removeAllViews()
        userInputs.clear()

        val componentsString = template.optString("components")
        val components = try {
            JSONArray(componentsString)
        } catch (e: Exception) {
            Log.e("TEMPLATE_UI", "Failed to parse components", e)
            JSONArray()
        }

        Log.d("TEMPLATE_UI", "Total components: ${components.length()}")

        for (i in 0 until components.length()) {
            val comp = components.optJSONObject(i)
            val type = comp.optString("type")
            Log.d("TEMPLATE_UI", "Rendering component #$i of type: $type")

            when (type) {
                "HEADER" -> {
                    val format = comp.optString("format")
                    when (format) {
                        "TEXT" -> {
                            val header = TextView(context).apply {
                                text = comp.optString("text")
                                textSize = 16f
                                setTypeface(null, Typeface.BOLD)
                                setPadding(0, 8, 0, 8)
                            }
                            layout.addView(header)
                        }

                        "IMAGE" -> {
                            val imageLayout = LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            }

                            val imageView = ImageView(context).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 400
                                )
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }

                            val urlEditText = EditText(context).apply {
                                hint = "Image URL"
                                inputType = InputType.TYPE_TEXT_VARIATION_URI
                                setPadding(16, 8, 16, 8)
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            }

                            try {
                                val example = comp.optJSONObject("example")
                                val handles = example?.optJSONArray("header_handle")
                                val imageUrl = handles?.optString(0)

                                if (!imageUrl.isNullOrEmpty()) {
                                    urlEditText.setText(imageUrl)

                                    Glide.with(context)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.baseline_attach_file_24)
                                        .into(imageView)
                                } else {
                                    imageView.setImageResource(R.drawable.baseline_attach_file_24)
                                }
                            } catch (e: Exception) {
                                imageView.setImageResource(R.drawable.baseline_attach_file_24)
                                Log.e("TEMPLATE_UI", "Image load failed", e)
                            }

                            urlEditText.addTextChangedListener(object : TextWatcher {
                                override fun afterTextChanged(s: Editable?) {}
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    Glide.with(context)
                                        .load(s.toString())
                                        .placeholder(R.drawable.baseline_attach_file_24)
                                        .into(imageView)
                                }
                            })

                            imageLayout.addView(imageView)
                            imageLayout.addView(urlEditText)
                            layout.addView(imageLayout)
                        }

                    }
                }
                    "BODY" -> {
                        val bodyText = comp.optString("text")
                        val placeholderRegex = Regex("\\{\\{(\\d+)\\}\\}")
                        val matches = placeholderRegex.findAll(bodyText).toList()

                        val defaultValues = try {
                            val example = comp.optJSONObject("example")
                            val bodyArray = example?.optJSONArray("body_text")
                            val firstSet = bodyArray?.optJSONArray(0)
                            (0 until (firstSet?.length() ?: 0)).map { i ->
                                firstSet?.optString(i) ?: ""
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }

                        val preview = TextView(context).apply {
                            text = bodyText
                            setPadding(0, 10, 0, 10)
                            setTextColor(Color.DKGRAY)
                        }
                        layout.addView(preview)

                        userInputs.clear()

                        fun updatePreview() {
                            var updatedText = bodyText
                            for ((index, match) in matches.withIndex()) {
                                val original = match.value
                                val input = userInputs.getOrNull(index) ?: ""
                                //updatedText = updatedText.replace(original, "{{${input}}}")

                                updatedText = updatedText.replace(original, input)


                            }
                            preview.text = updatedText
                        }

                        for (i in matches.indices) {
                            val defaultValue = defaultValues.getOrNull(i) ?: ""

                            val inputField = EditText(context).apply {
                                hint = "Field ${i + 1}"
                                setText(defaultValue)
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                inputType = InputType.TYPE_CLASS_TEXT
                                isFocusable = true
                                isFocusableInTouchMode = true
                                requestFocus()
                                setPadding(16, 16, 16, 16)
                            }


                            userInputs.add(defaultValue)

                            inputField.addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(
                                    s: CharSequence?,
                                    start: Int,
                                    count: Int,
                                    after: Int
                                ) {
                                }

                                override fun onTextChanged(
                                    s: CharSequence?,
                                    start: Int,
                                    before: Int,
                                    count: Int
                                ) {
                                    userInputs[i] = s.toString()
                                    updatePreview()
                                }

                                override fun afterTextChanged(s: Editable?) {}
                            })

                            layout.addView(inputField)
                        }

                        updatePreview()
                    }



                    "FOOTER" -> {
                        val footer = TextView(context).apply {
                            text = comp.optString("text")
                            setTextColor(Color.GRAY)
                            setPadding(0, 10, 0, 10)
                        }
                        layout.addView(footer)
                    }

                    "BUTTONS" -> {
                        val buttons = comp.optJSONArray("buttons") ?: JSONArray()
                        for (b in 0 until buttons.length()) {
                            val btn = buttons.getJSONObject(b)
                            val button = Button(context).apply {
                                text = btn.optString("text", "Action")
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            }
                            layout.addView(button)
                        }
                    }
                }

                Log.d("TEMPLATE_UI", "Rendering template: ${template.optString("name")}")
            }

        }
    fun loadMessages() {
        val number = intent.getStringExtra("wa_id_or_sender") ?: return
        val url = "https://waba.mpocket.in/api/phone/get/361462453714220/$number/1"

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                val messages = mutableListOf<Message>()
                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)
                    val messageType = obj.optString("message_type") ?: "text"

                    val messageBody = when (messageType) {
                        "template" -> {
                            val extraInfoStr = obj.optString("extra_info")
                            if (extraInfoStr.isNotEmpty()) {
                                try {
                                    val extraInfo = JSONObject(extraInfoStr)
                                    val name = extraInfo.optString("name")
                                    val components = extraInfo.optJSONArray("components")

                                    val body = (0 until (components?.length() ?: 0))
                                        .mapNotNull { components?.optJSONObject(it) }
                                        .firstOrNull { it.optString("type") == "body" }

                                    val params = body?.optJSONArray("parameters")
                                    val values = (0 until (params?.length() ?: 0)).map { i ->
                                        params!!.getJSONObject(i).optString("text")
                                    }

                                    val templateText = templateBodyMap[name] ?: name

                                    var rendered = templateText
                                    values.forEachIndexed { index, v ->
                                        rendered = rendered.replace("{{${index + 1}}}", v)
                                    }
                                    rendered
                                } catch (e: Exception) {
                                    "Template Message"
                                }
                            } else {
                                "Template Message"
                            }
                        }

                        else -> obj.optString("message_body", "")
                    }

                  //  val imageUrl = obj.optString("url").ifEmpty { obj.optString("file_url") }
                    var imageUrl = obj.optString("url").ifEmpty { obj.optString("file_url") }

                    if (messageType == "template" && imageUrl.isNullOrEmpty()) {
                        try {
                            val extraInfoStr = obj.optString("extra_info")
                            if (extraInfoStr.isNotEmpty()) {
                                val extraInfo = JSONObject(extraInfoStr)
                                val components = extraInfo.optJSONArray("components")

                                val header = (0 until (components?.length() ?: 0))
                                    .mapNotNull { components?.optJSONObject(it) }
                                    .firstOrNull {
                                        it.optString("type").equals("HEADER", true) &&
                                                it.optString("format").equals("IMAGE", true) &&
                                                it.has("example")
                                    }

                                val example = header?.optJSONObject("example")
                                val headerHandle = example?.optJSONArray("header_handle")
                                val extractedUrl = headerHandle?.optString(0)

                                if (!extractedUrl.isNullOrEmpty()) {
                                    imageUrl = extractedUrl
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("IMG_PARSE", "Header image parse failed", e)
                        }
                    }

                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis() / 1000)
                    if (messageBody.isNullOrEmpty() && messageType != "image") continue

                    val sender = obj.optString("sender")
                    val recipientId = obj.optString("recipient_id")
                    val waId = obj.optString("wa_id")

                    val message = Message(
                        sender = if (sender.isNullOrEmpty()) null else sender,
                        messageBody = messageBody,
                        messageType = messageType,
                        timestamp = timestamp,
                        url = imageUrl,
                        recipientId = recipientId,
                        waId = waId
                    )


                    messages.add(message)
                }

                messages.reverse()
                adapter.setMessages(messages)

                binding.recyclerViewMessages.post {
                    binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                }
            },
            { error ->
                Log.e("Volley", "Error loading messages", error)
                Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show()
            })

        Volley.newRequestQueue(this).add(request)
    }


}