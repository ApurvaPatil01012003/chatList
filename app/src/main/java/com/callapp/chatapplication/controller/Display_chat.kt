package com.callapp.chatapplication.controller

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
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
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject

class Display_chat : AppCompatActivity() {
    private val templateBodyMap = mutableMapOf<String, String>()
    private lateinit var binding: ActivityDisplayChatBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    val phoneNumberId = "361462453714220"

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
            val url = "https://waba.mpocket.in/api/phone/get/$phoneNumberId/$number/1"

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
                            waId = waId,
                            sent = obj.optInt("sent", 0),
                            delivered = obj.optInt("delivered", 0),
                            read = obj.optInt("read", 0)
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
            "https://waba.mpocket.in/api/phone/get/message_templates/$phoneNumberId?accessToken=Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"
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
        val dialog = Dialog(this)
        val dialogView = layoutInflater.inflate(R.layout.message_template, null)
        dialog.setContentView(dialogView)

        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )

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

                selectedTemplate?.let {
                    renderTemplateUI(it, inputFields, this@Display_chat, userInputs)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        buttonCancel.setOnClickListener { dialog.dismiss() }

        buttonSend.setOnClickListener {
            selectedTemplate?.let {
                sendTemplateMessage(it, userInputs)
            }
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            inputFields.postDelayed({
                for (i in 0 until inputFields.childCount) {
                    val view = inputFields.getChildAt(i)
                    val editText = when (view) {
                        is TextInputLayout -> view.editText
                        is EditText -> view
                        else -> null
                    }

                    if (editText != null) {
                        editText.requestFocus()
                        editText.isFocusableInTouchMode = true
                        editText.setSelection(editText.text?.length ?: 0)

                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)

                        break
                    }
                }
            }, 300)
        }

        dialog.show()
    }
    fun sendTemplateMessage(template: JSONObject, inputs: List<String>) {
        val name = template.optString("name")
        val to = intent.getStringExtra("wa_id_or_sender") ?: return
        val componentsStr = template.optString("components")
        val componentsArr = JSONArray(componentsStr)
        val componentsArray = JSONArray()

        val hasImageHeader = componentsStr.contains("\"type\":\"HEADER\"") &&
                componentsStr.contains("\"format\":\"IMAGE\"")
        val totalPlaceholders = getBodyPlaceholderCount(template)
        val imageInputsCount = if (hasImageHeader) 1 else 0
        val bodyInputs = inputs.drop(imageInputsCount)

        Log.d("TEMPLATE_INPUTS", "Inputs: $inputs | hasImageHeader: $hasImageHeader")
        Log.d("TEMPLATE_BODY_INPUTS", "bodyInputs: $bodyInputs | totalPlaceholders: $totalPlaceholders")

        if (bodyInputs.size < totalPlaceholders) {
            Toast.makeText(this, "Please fill all $totalPlaceholders template fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (bodyInputs.any { it.isBlank() }) {
            Toast.makeText(this, "Please fill all template fields", Toast.LENGTH_SHORT).show()
            return
        }

        for (i in 0 until componentsArr.length()) {
            val comp = componentsArr.getJSONObject(i)
            val type = comp.optString("type")

            when (type) {
                "HEADER" -> {
                    if (comp.optString("format") == "IMAGE") {
                        var imageUrl = inputs.getOrNull(0)?.trim()

                        val fallbackUrl = comp.optJSONObject("example")
                            ?.optJSONArray("header_handle")
                            ?.optString(0)

                        if (imageUrl.isNullOrBlank() || !imageUrl.startsWith("http")) {
                            imageUrl = fallbackUrl
                        }

                        Log.d("TEMPLATE_HEADER", "Resolved image URL: $imageUrl")

                        if (imageUrl.isNullOrBlank() || !imageUrl.startsWith("http")) {
                            Toast.makeText(this, "Image URL is required for this template", Toast.LENGTH_SHORT).show()
                            Log.e("TEMPLATE_ERROR", "Aborting: Invalid image URL: $imageUrl")
                            return
                        }


                        if (!imageUrl.isNullOrBlank() && imageUrl.startsWith("http")) {
                            val headerParams = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "image")
                                    put("image", JSONObject().put("link", imageUrl))
                                })
                            }
                            componentsArray.put(JSONObject().apply {
                                put("type", "header")
                                put("parameters", headerParams)
                            })
                        } else {
                            Toast.makeText(this, "Image URL is required for this template", Toast.LENGTH_SHORT).show()
                            Log.e("TEMPLATE_ERROR", "Aborting: Invalid image URL: $imageUrl")
                            return
                        }
                    }
                }

                "BODY" -> {
                    val params = JSONArray()
                    bodyInputs.forEach { value ->
                        params.put(JSONObject().apply {
                            put("type", "text")
                            put("text", value)
                        })
                    }
                    componentsArray.put(JSONObject().apply {
                        put("type", "body")
                        put("parameters", params)
                    })
                }

                "BUTTONS" -> {
                    val buttons = comp.optJSONArray("buttons") ?: continue

                    var btnIndex = 0
                    for (j in 0 until buttons.length()) {
                        val btn = buttons.getJSONObject(j)
                        val btnType = btn.optString("type").lowercase()

                        if (btnType == "phone_number") continue

                        val buttonObj = JSONObject().apply {
                            put("type", "button")
                            put("sub_type", btnType)
                            put("index", btnIndex++)
                        }

                        if (btnType == "quick_reply") {
                            buttonObj.put("parameters", JSONArray())
                            componentsArray.put(buttonObj)
                        } else if (btnType == "url") {
                            componentsArray.put(buttonObj)
                        }

                    }
                }



            }
        }


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

        Log.d("TEMPLATE_PAYLOAD", payload.toString(2))

        val url = "https://waba.mpocket.in/api/$phoneNumberId/messages"
        val request = object : JsonObjectRequest(Method.POST, url, payload,
            { response ->
                Log.d("TEMPLATE_SEND", "Success: $response")
                Toast.makeText(this, "Template sent", Toast.LENGTH_SHORT).show()
                loadMessages()
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

        for (i in 0 until components.length()) {
            val comp = components.optJSONObject(i)
            val type = comp.optString("type")

            when (type) {
                "HEADER" -> {
                    val format = comp.optString("format")
                    if (format == "IMAGE") {
                        val imageLayout = LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                        }

                        val imageView = ImageView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 10, 0, 10)
                            }
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }

                        val headerExample = comp.optJSONObject("example")
                        val defaultImageUrl = headerExample
                            ?.optJSONArray("header_handle")
                            ?.optString(0) ?: ""

                        var userImageUrl: String? = defaultImageUrl

                        // Load image into ImageView
                        Glide.with(context).load(defaultImageUrl).into(imageView)

                        // Input field
                        val fieldLayout = LayoutInflater.from(context)
                            .inflate(R.layout.single_input_field, null)
                        val imageEditText = fieldLayout.findViewById<EditText>(R.id.dynamicInput)
                        imageEditText.hint = "Image URL"
                        imageEditText.inputType = InputType.TYPE_TEXT_VARIATION_URI

                        // Fill EditText if default image is valid
                        if (defaultImageUrl.isNotBlank() && !defaultImageUrl.contains("scontent.whatsapp.net")) {
                            imageEditText.setText(defaultImageUrl)
                        }

                        // Ensure userInputs[0] is reserved for image URL
                        if (userInputs.isEmpty()) {
                            userInputs.add(defaultImageUrl)
                        } else {
                            userInputs[0] = defaultImageUrl
                        }

                        Log.d("TEMPLATE_UI", "Default image URL set in userInputs[0]: $defaultImageUrl")

                        imageEditText.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {}
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                val enteredUrl = s.toString().trim()
                                userImageUrl = if (enteredUrl.isNotEmpty()) enteredUrl else defaultImageUrl

                                if (userInputs.size > 0) userInputs[0] = userImageUrl ?: ""
                                else userInputs.add(0, userImageUrl ?: "")

                                Glide.with(context).load(userImageUrl).into(imageView)
                            }
                        })

                        imageLayout.addView(imageView)
                        imageLayout.addView(fieldLayout)
                        layout.addView(imageLayout)
                    }
                }


                "BODY" -> {
                    val bodyText = comp.optString("text")

                    val exampleArray = comp.optJSONObject("example")
                        ?.optJSONArray("body_text")
                        ?.optJSONArray(0)

                    val placeholderRegex = Regex("\\{\\{(\\d+)\\}\\}")
                    val matches = placeholderRegex.findAll(bodyText).toList()

                    // Determine starting index for body inputs
                    val startIndex = if (userInputs.isNotEmpty() && userInputs[0].startsWith("http")) 1 else 0

                    // Ensure enough space in userInputs
                    for (i in 0 until matches.size) {
                        val defaultValue = exampleArray?.optString(i) ?: ""
                        val index = startIndex + i
                        if (userInputs.size <= index) userInputs.add(defaultValue)
                        else userInputs[index] = defaultValue
                    }

                    // Fill preview
                    var previewText = bodyText
                    matches.forEachIndexed { i, match ->
                        val value = userInputs.getOrNull(startIndex + i) ?: ""
                        previewText = previewText.replace(match.value, value)
                    }

                    val preview = TextView(context).apply {
                        text = previewText
                        setPadding(0, 10, 0, 10)
                        setTextColor(Color.DKGRAY)
                    }
                    layout.addView(preview)

                    // Input fields
                    matches.forEachIndexed { i, matchResult ->
                        val placeholderIndex = matchResult.groupValues[1].toInt()
                        val actualIndex = startIndex + i

                        val fieldLayout = LayoutInflater.from(context)
                            .inflate(R.layout.single_input_field, null)
                        val editText = fieldLayout.findViewById<EditText>(R.id.dynamicInput)
                        editText.hint = "Enter value for {{${placeholderIndex}}}"
                        editText.inputType = InputType.TYPE_CLASS_TEXT
                        editText.setText(userInputs.getOrElse(actualIndex) { "" })

                        editText.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {}
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                userInputs[actualIndex] = s.toString()

                                var updated = bodyText
                                matches.forEachIndexed { j, m ->
                                    val idx = startIndex + j
                                    updated = updated.replace(m.value, userInputs.getOrNull(idx) ?: "")
                                }
                                preview.text = updated
                            }
                        })

                        layout.addView(fieldLayout)
                    }
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
                        }
                        layout.addView(button)
                    }
                }
            }
        }
    }

    fun loadMessages() {
        val number = intent.getStringExtra("wa_id_or_sender") ?: return
        val url = "https://waba.mpocket.in/api/phone/get/$phoneNumberId/$number/1"

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                val messages = mutableListOf<Message>()
                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)
                    val messageType = obj.optString("message_type") ?: "text"

                    val messageBody = when (messageType) {
                        "template" -> {
                            val extraInfoStr = obj.optString("extra_info")
                            Log.d("EXTRA_INFO_RAW", "extra_info for template: $extraInfoStr")

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
                            Log.d("EXTRA_INFO_RAW", "extra_info: $extraInfoStr")

                            if (extraInfoStr.isNotEmpty()) {
                                Log.d("DEBUG_EXTRA", "Raw extra_info string: $extraInfoStr")
                                val extraInfo = JSONObject(extraInfoStr)
                                val components = extraInfo.optJSONArray("components")
                                Log.d("DEBUG_EXTRA", "Found components length: ${components?.length()}")


                                val header = (0 until (components?.length() ?: 0))
                                    .mapNotNull { components?.optJSONObject(it) }
                                    .firstOrNull {
                                        it.optString("type").equals("HEADER", true) &&
                                                it.optString("format").equals("IMAGE", true)
                                    }

                                header?.let {
                                    val imageParam = it.optJSONArray("parameters")
                                        ?.optJSONObject(0)
                                        ?.optJSONObject("image")
                                        ?.optString("link")

                                    val exampleUrl = it.optJSONObject("example")
                                        ?.optJSONArray("header_handle")
                                        ?.optString(0)

                                    imageUrl = imageParam ?: exampleUrl ?: ""

                                    Log.d("HEADER_IMAGE_DEBUG", "Found imageUrl: $imageUrl")
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

    private fun getBodyPlaceholderCount(template: JSONObject): Int {
        val components = JSONArray(template.optString("components"))
        for (i in 0 until components.length()) {
            val comp = components.getJSONObject(i)
            if (comp.optString("type") == "BODY") {
                val bodyText = comp.optString("text")
                return Regex("\\{\\{\\d+\\}\\}").findAll(bodyText).count()
            }
        }
        return 0
    }


}