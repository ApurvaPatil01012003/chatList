package com.callapp.chatapplication.controller

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
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
import com.android.volley.Response
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
    private lateinit var binding: ActivityDisplayChatBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    val phoneNumberId = "361462453714220"
    private lateinit var waId: String
    private var isActiveLast24Hours: Boolean = true
    val templateBodyMap = mutableMapOf<String, String>()
    val templateButtonsMap = mutableMapOf<String, JSONArray>()
    val templateFullMap = mutableMapOf<String, JSONObject>()


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
        isActiveLast24Hours = intent.getBooleanExtra("active_last_24_hours", true)


        val name = intent.getStringExtra("contact_name")
        var number = intent.getStringExtra("wa_id_or_sender")
        waId = intent.getStringExtra("wa_id_or_sender") ?: ""
        Log.d("WAID_CHECK", "Assigned waId: $waId")


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
            val waId = intent.getStringExtra("wa_id_or_sender") ?: return@setOnClickListener
            val accessToken = "Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"

            if (text.isNotEmpty()) {
                val isSent = if (isActiveLast24Hours) 1 else 0

                val msg = Message(
                    sender = "you",
                    messageBody = text,
                    messageType = "text",
                    timestamp = System.currentTimeMillis() / 1000,
                    url = null,
                    recipientId = waId,
                    waId = waId,
                    extraInfo = null,
                    sent = isSent
                )

                adapter.addMessage(msg)
                binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)
                binding.editTextMessage.setText("")

                if (!isActiveLast24Hours) {
                    Toast.makeText(
                        this,
                        "Message failed: Contact inactive for 24+ hours",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                sendTextMessage(
                    to = waId,
                    text = text,
                    phoneNumberId = phoneNumberId,
                    accessToken = accessToken,
                    onSuccess = {
                        Toast.makeText(this, "Message Sent", Toast.LENGTH_SHORT).show()
                    },
                    onError = {
                        Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }


        number = intent.getStringExtra("wa_id_or_sender") ?: ""

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

        binding.buttonAttach.setOnClickListener {
            showAttachmentPopup()
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

//    fun fetchTemplates(onResult: (List<JSONObject>) -> Unit, onError: (String) -> Unit) {
//        val url =
//            "https://waba.mpocket.in/api/phone/get/message_templates/$phoneNumberId?accessToken=Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"
//        val request = JsonObjectRequest(Request.Method.GET, url, null,
//            { response ->
//                val templates = response.optJSONArray("data") ?: response.optJSONArray("templates")
//
//                templateBodyMap.clear()
//
//                val result = mutableListOf<JSONObject>()
//
//                val templatesLength = templates?.length() ?: 0
//                for (i in 0 until templatesLength) {
//                    val template = templates?.getJSONObject(i) ?: continue
//                    val name = template.optString("name")
//                    val componentsStr = template.optString("components")
//
//                    try {
//                        val components = JSONArray(componentsStr)
//                        for (j in 0 until components.length()) {
//                            val comp = components.getJSONObject(j)
//                            if (comp.optString("type") == "BODY") {
//                                val bodyText = comp.optString("text")
//                                templateBodyMap[name] = bodyText
//                            }
//
//
//                        }
//
//                        result.add(template)
//                    } catch (e: Exception) {
//                        Log.e("TEMPLATE_PARSE", "Error parsing components for $name", e)
//                    }
//
//
//                }
//
//                onResult(result) // result can be used for template dialog
//                Log.d("TEMPLATE_MAP_SIZE", "templateBodyMap size: ${templateBodyMap.size}")
//                Log.d("TEMPLATE_KEYS", "templateBodyMap keys: ${templateBodyMap.keys}")
//
//                loadMessages()
//            },
//            { error ->
//                onError(error.message ?: "Template fetch failed")
//                Log.d("TEMPLATE_MAP_SIZE", "templateBodyMap size: ${templateBodyMap.size}")
//                Log.d("TEMPLATE_KEYS", "templateBodyMap keys: ${templateBodyMap.keys}")
//
//                loadMessages()
//            }
//        )
//
//        Volley.newRequestQueue(this).add(request)
//    }




    fun fetchTemplates(onResult: (List<JSONObject>) -> Unit, onError: (String) -> Unit) {
        val url =
            "https://waba.mpocket.in/api/phone/get/message_templates/$phoneNumberId?accessToken=Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val templates = response.optJSONArray("data") ?: response.optJSONArray("templates")

                templateBodyMap.clear()
                templateButtonsMap.clear()

                val result = mutableListOf<JSONObject>()
                val templatesLength = templates?.length() ?: 0

                for (i in 0 until templatesLength) {
                    val template = templates?.getJSONObject(i) ?: continue
                    val name = template.optString("name")
                    val componentsStr = template.optString("components")


                    try {
                        val components = JSONArray(componentsStr)
                        val buttonArray = JSONArray()

                        for (j in 0 until components.length()) {
                            val comp = components.getJSONObject(j)
                            when (comp.optString("type")) {
                                "BODY" -> {
                                    val bodyText = comp.optString("text")
                                    templateBodyMap[name] = bodyText
                                }

                                "BUTTONS" -> {
                                    val buttons = comp.optJSONArray("buttons") ?: continue
                                    for (k in 0 until buttons.length()) {
                                        val btn = buttons.getJSONObject(k)
                                        val type = btn.optString("type")
                                        val text = btn.optString("text", "Click")
                                        val param = when (type) {
                                            "URL" -> btn.optString("url")
                                            "PHONE_NUMBER" -> btn.optString("phone_number")
                                            else -> ""
                                        }

                                        buttonArray.put(JSONObject().apply {
                                            put("index", k)
                                            put("text", text)
                                            put("type", type)
                                            put("param", param)
                                        })
                                    }
                                }
                            }
                        }

                        if (buttonArray.length() > 0) {
                            templateButtonsMap[name] = buttonArray
                        }

                        result.add(template)
                        templateFullMap[name] = template

                    } catch (e: Exception) {
                        Log.e("TEMPLATE_PARSE", "Error parsing components for $name", e)
                    }
                }

                Log.d("TEMPLATE_MAP_SIZE", "templateBodyMap size: ${templateBodyMap.size}")
                Log.d("TEMPLATE_KEYS", "templateBodyMap keys: ${templateBodyMap.keys}")
                Log.d("TEMPLATE_BUTTONS", "templateButtonsMap: $templateButtonsMap")

                onResult(result)
                loadMessages() // optional
            },
            { error ->
                onError(error.message ?: "Template fetch failed")
                Log.d("TEMPLATE_MAP_SIZE", "templateBodyMap size: ${templateBodyMap.size}")
                Log.d("TEMPLATE_KEYS", "templateBodyMap keys: ${templateBodyMap.keys}")
                loadMessages()
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
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, templateNames)
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

                        val imm =
                            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
        val buttonsUIArray = JSONArray()



        val hasImageHeader = componentsStr.contains("\"type\":\"HEADER\"") &&
                componentsStr.contains("\"format\":\"IMAGE\"")
        val totalPlaceholders = getBodyPlaceholderCount(template)
        val imageInputsCount = if (hasImageHeader) 1 else 0
        val bodyInputs = inputs.drop(imageInputsCount)

        if (bodyInputs.size < totalPlaceholders || bodyInputs.any { it.isBlank() }) {
            Toast.makeText(this, "Please fill all template fields", Toast.LENGTH_SHORT).show()
            return
        }

        var headerImageUrl: String? = null

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

                        if (imageUrl.isNullOrBlank() || !imageUrl.startsWith("http")) {
                            Toast.makeText(this, "Valid image URL required", Toast.LENGTH_SHORT)
                                .show()
                            return
                        }

                        headerImageUrl = imageUrl // save for display

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
                    val storedButtons = templateButtonsMap[name] ?: JSONArray()
                    for (k in 0 until storedButtons.length()) {
                        val btn = storedButtons.getJSONObject(k)
                        val type = btn.optString("type").uppercase()
                        val subType = if (type == "PHONE_NUMBER") "VOICE_CALL" else type
                        val param = btn.optString("param")

                        // API payload
                        val buttonObj = JSONObject().apply {
                            put("type", "button")
                            put("sub_type", subType)
                            put("index", k)

                            if (subType == "URL") {
                                put("parameters", JSONArray().put(
                                    JSONObject().apply {
                                        put("type", "text")
                                        put("text", param)
                                    }
                                ))
                            }
                        }
                        componentsArray.put(buttonObj)

                        // Display in chat
                        buttonsUIArray.put(JSONObject().apply {
                            put("index", k)
                            put("text", btn.optString("text", "Action"))
                            put("type", type)
                            put("param", param)
                        })
                    }

                    template.put("buttons", buttonsUIArray)
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


                val mediaId = "template_header_${System.currentTimeMillis()}"
                if (!headerImageUrl.isNullOrBlank()) {
                    getSharedPreferences("image_url_cache", Context.MODE_PRIVATE)
                        .edit().putString(mediaId, headerImageUrl).apply()
                }

                val renderedText = renderBody(templateBodyMap[name], bodyInputs)
               // val renderedButtons = template.optJSONArray("rendered_buttons") ?: JSONArray()
                val renderedButtons = buttonsUIArray

                val extraInfoJson = JSONObject().apply {
                    put("name", name)
                    put("components", componentsArray)
                    put("media_id", mediaId)
                    put("buttons", renderedButtons)
                }



                val message = Message(
                    sender = "you",
                    messageBody = renderedText,
                    messageType = "template",
                    timestamp = System.currentTimeMillis() / 1000,
                    url = headerImageUrl,
                    recipientId = to,
                    waId = to,
                    extraInfo = extraInfoJson.toString(),
                    componentData = componentsArray.toString(),
                    sent = 1
                )

                adapter.addMessage(message)
                binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)
            },
            { error ->
                val errData = error.networkResponse?.data?.let { String(it) }
                Log.e("TEMPLATE_SEND", "Error: $errData")
                Toast.makeText(this, "Send failed: $errData", Toast.LENGTH_LONG).show()
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

    fun renderBody(templateBody: String?, values: List<String>): String {
        if (templateBody.isNullOrBlank()) return ""
        return values.foldIndexed(templateBody) { index, acc, v ->
            acc.replace("{{${index + 1}}}", v)
        }
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
            Log.d("TEMPLATE_UI", "Processing component type: $type")

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

                        Log.d(
                            "TEMPLATE_UI",
                            "Default image URL set in userInputs[0]: $defaultImageUrl"
                        )

                        imageEditText.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {}
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
                                val enteredUrl = s.toString().trim()
                                userImageUrl =
                                    if (enteredUrl.isNotEmpty()) enteredUrl else defaultImageUrl

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

                    val startIndex =
                        if (userInputs.isNotEmpty() && userInputs[0].startsWith("http")) 1 else 0

                    for (i in 0 until matches.size) {
                        val defaultValue = exampleArray?.optString(i) ?: ""
                        val index = startIndex + i
                        if (userInputs.size <= index) userInputs.add(defaultValue)
                        else userInputs[index] = defaultValue
                    }
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
                                userInputs[actualIndex] = s.toString()

                                var updated = bodyText
                                matches.forEachIndexed { j, m ->
                                    val idx = startIndex + j
                                    updated =
                                        updated.replace(m.value, userInputs.getOrNull(idx) ?: "")
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
                    val buttons = comp.optJSONArray("buttons")
                    Log.d(
                        "TEMPLATE_UI",
                        "BUTTONS component found, buttons length: ${buttons?.length() ?: 0}"
                    )

                    if (buttons != null) {
                        for (b in 0 until buttons.length()) {
                            val btn = buttons.getJSONObject(b)
                            val buttonText = btn.optString("text", "Action")
                            Log.d("TEMPLATE_UI", "Rendering button #$b: $buttonText")

                            val button = Button(context).apply {
                                text = buttonText
                            }
                            layout.addView(button)
                        }
                    } else {
                        Log.w(
                            "TEMPLATE_UI",
                            "BUTTONS component present but 'buttons' array is null"
                        )
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
                val prefs = getSharedPreferences("image_url_cache", Context.MODE_PRIVATE)
                val messages = mutableListOf<Message>()

                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)
                    val extraInfoStr = obj.optString("extra_info")
                    Log.d("extraonfo","$extraInfoStr")
                    val messageType = obj.optString("message_type") ?: "text"

                    val messageBody = when (messageType) {
                        "template" -> {
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
                                    values.foldIndexed(templateText) { index, acc, v ->
                                        acc.replace("{{${index + 1}}}", v)
                                    }
                                } catch (e: Exception) {
                                    Log.e("TEMPLATE_ERR", "Template parse failed", e)
                                    "Template Message"
                                }
                            } else "Template Message"
                        }


                        else -> obj.optString("message_body", "")
                    }

                    var sender = obj.optString("sender") ?: ""
                    if (sender.isEmpty()) {
                        val waIdFromMsg = obj.optString("wa_id") ?: ""
                        val currentUserWaId = intent.getStringExtra("wa_id_or_sender") ?: ""
                        if (waIdFromMsg == currentUserWaId) sender = "you"
                    }

                    var imageUrl: String? = obj.optString("url")
                    if (imageUrl.isNullOrBlank() || imageUrl == "null") {
                        imageUrl = obj.optString("file_url")
                    }

                    if (messageType == "image" && (imageUrl.isNullOrBlank() || imageUrl == "null")) {
                        try {
                            if (!extraInfoStr.isNullOrBlank() && extraInfoStr != "null") {
                                val extraInfo = JSONObject(extraInfoStr)
                                val mediaId = extraInfo.optString("media_id")
                                Log.d("PREF_READ", "Trying to restore image for media_id=$mediaId")

                                if (!mediaId.isNullOrBlank()) {
                                    val cachedUrl = prefs.getString(mediaId, null)
                                    Log.d(
                                        "PREF_READ_RESULT",
                                        "Restored from prefs: $mediaId → $cachedUrl"
                                    )

                                    if (!cachedUrl.isNullOrBlank()) {
                                        imageUrl = cachedUrl
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("IMG_RESTORE", "Failed to parse extra_info", e)
                        }
                    }


                    if (messageType == "template" && (imageUrl.isNullOrBlank() || imageUrl == "null")) {
                        val restored = resolveTemplateImageFromPrefs(prefs, extraInfoStr)
                        if (!restored.isNullOrBlank()) {
                            imageUrl = restored
                            Log.d("IMG_RESTORE", "Restored template image from prefs → $imageUrl")
                        }
                    }

                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis() / 1000)
                    if (messageBody.isEmpty() && messageType != "image") return@JsonArrayRequest

                    val componentDataJson: String? = try {
                        val name = JSONObject(extraInfoStr).optString("name")
                        templateFullMap[name]?.optString("component_data")
                    } catch (e: Exception) {
                        null
                    }

                    Log.d("Component_data","$componentDataJson")
                    val message = Message(
                        sender = if (sender.isEmpty()) null else sender,
                        messageBody = messageBody,
                        messageType = messageType,
                        timestamp = timestamp,
                        url = imageUrl,
                        recipientId = obj.optString("recipient_id"),
                        waId = obj.optString("wa_id"),
                        extraInfo = extraInfoStr,
                        componentData = componentDataJson,
                        sent = obj.optInt("sent", 0),
                        delivered = obj.optInt("delivered", 0),
                        read = obj.optInt("read", 0)
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

    fun resolveTemplateImageFromPrefs(prefs: SharedPreferences, extraInfoStr: String?): String? {
        if (extraInfoStr.isNullOrBlank()) return null
        return try {
            val json = JSONObject(extraInfoStr)
            val mediaId = json.optString("media_id")
            if (!mediaId.isNullOrBlank()) {
                prefs.getString(mediaId, null)
            } else {
                val components = json.optJSONArray("components") ?: return null
                for (i in 0 until components.length()) {
                    val comp = components.getJSONObject(i)
                    if (comp.optString("type").equals("header", true)) {
                        val param = comp.optJSONArray("parameters")
                            ?.optJSONObject(0)
                            ?.optJSONObject("image")
                        return param?.optString("link")
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e("IMG_PREF_PARSE", "Failed to restore image", e)
            null
        }
    }


    private fun getBodyPlaceholderCount(template: JSONObject): Int {
        val components = JSONArray(template.optString("components"))
        Log.d("BTN_COMPONENT", "Component type: $components")
        for (i in 0 until components.length()) {
            val comp = components.getJSONObject(i)
            if (comp.optString("type") == "BODY") {
                val bodyText = comp.optString("text")
                return Regex("\\{\\{\\d+\\}\\}").findAll(bodyText).count()
            }
        }
        return 0
    }

    fun JSONArray.putAll(other: JSONArray) {
        for (i in 0 until other.length()) {
            this.put(other.get(i))
        }
    }

    fun showAttachmentPopup() {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.popup_attachment_options, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val location = IntArray(2)
        binding.buttonAttach.getLocationOnScreen(location)

        val x = location[0]
        val y = location[1]

        popupWindow.showAtLocation(
            binding.buttonAttach,
            Gravity.NO_GRAVITY,
            x,
            y - popupWindow.contentView.measuredHeight - 10
        )

        val PICK_IMAGES_REQUEST_CODE = 1001
        val PICK_VIDEO_REQUEST_CODE = 1002

        popupView.findViewById<LinearLayout>(R.id.optionImage).setOnClickListener {
            popupWindow.dismiss()

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_IMAGES_REQUEST_CODE)
        }

        popupView.findViewById<LinearLayout>(R.id.optionVideo).setOnClickListener {
            popupWindow.dismiss()

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_VIDEO_REQUEST_CODE)
        }
        popupView.findViewById<LinearLayout>(R.id.optionDocument).setOnClickListener {
            popupWindow.dismiss()
            // openDocumentPicker()
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
//            val uri = data.data!!
//            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
//            uploadFileAndSend(
//                this,
//                uri,
//                mimeType,
//                waId,
//                phoneNumberId,
//                "Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"
//            )
//        }
//    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

            // Optional: persist URI permission if needed
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w("URI_PERMISSION", "Failed to persist permission: ${e.message}")
            }

            when (requestCode) {
                1001, 1002 -> {
                    uploadFileAndSend(
                        this,
                        uri,
                        mimeType,
                        waId,
                        phoneNumberId,
                        "Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"
                    )
                }
            }
        }
    }


    fun uploadFileAndSend(
        context: Context,
        uri: Uri,
        fileType: String,
        waId: String,
        phoneNumberId: String,
        accessToken: String
    ) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileData = inputStream?.readBytes() ?: return

        val fileName = "upload_" + System.currentTimeMillis() + "." +
                MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)

        val request = MultipartRequest(
            url = "https://waba.mpocket.in/api/$phoneNumberId/upload-file",
            fileData = fileData,
            fileName = fileName,
            fileType = fileType,
            listener = Response.Listener { response ->
                val metaMediaId = response.getString("metaMediaId")
                val s3Url = response.getString("s3Url")

                val prefs = context.getSharedPreferences("image_url_cache", Context.MODE_PRIVATE)
                prefs.edit().putString(metaMediaId, s3Url).apply()
                Log.d("PREF_WRITE", "Saved $metaMediaId → $s3Url")

                sendMediaMessage(s3Url, fileType, waId, phoneNumberId, accessToken)

                val sentMessage = Message(
                    sender = "you",
                    messageBody = "Image",
                    messageType = "image",
                    timestamp = System.currentTimeMillis() / 1000,
                    url = s3Url,
                    recipientId = waId,
                    waId = waId,
                    extraInfo = "{\"media_id\":\"$metaMediaId\"}"

                )

                adapter.addMessage(sentMessage)

                binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)

                Log.d("UPLOAD_SUCCESS", "metaMediaId: $metaMediaId, s3Url: $s3Url")
            },
            errorListener = Response.ErrorListener {
                Log.e("UPLOAD_FAIL", "Error: ${it.message}", it)
                val responseBody = it.networkResponse?.data?.let { data -> String(data) }
                Log.e("UPLOAD_FAIL", "Response Body: $responseBody")
                Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(context).add(request)
    }

//    fun sendMediaMessage(
//        s3Url: String,
//        fileType: String,
//        waId: String,
//        phoneNumberId: String,
//        accessToken: String
//    ) {
//        if (waId.isBlank()) {
//            Toast.makeText(this, "Recipient waId is missing", Toast.LENGTH_SHORT).show()
//            Log.e("SEND_MEDIA", "waId is blank, aborting")
//            return
//        }
//
//        val messageType = when {
//            fileType.startsWith("image") -> "image"
//            fileType.startsWith("video") -> "video"
//            else -> "document"
//        }
//
//        val url = "https://waba.mpocket.in/api/$phoneNumberId/messages"
//
//        val body = JSONObject().apply {
//            put("messaging_product", "whatsapp")
//            put("to", waId)
//            put("type", messageType)
//            put(messageType, JSONObject().apply {
//                put("link", s3Url)
//            })
//        }
//
//        Log.d("SEND_MEDIA_PAYLOAD", body.toString(2))
//
//        val request = object : JsonObjectRequest(Method.POST, url, body,
//            Response.Listener {
//                Log.d("MediaSend", "Success: $it")
//                Toast.makeText(this@Display_chat, "Media sent successfully", Toast.LENGTH_SHORT)
//                    .show()
//
//            },
//            Response.ErrorListener {
//                val errorBody = it.networkResponse?.data?.let { data -> String(data) }
//                Log.e("MediaSend", "Failed", it)
//                Log.e("MediaSend", "Error Body: $errorBody")
//                Toast.makeText(this@Display_chat, "Media send failed", Toast.LENGTH_SHORT).show()
//            }
//        ) {
//            override fun getHeaders(): MutableMap<String, String> {
//                return mutableMapOf(
//                    "Authorization" to "Bearer $accessToken",
//                    "Content-Type" to "application/json"
//                )
//            }
//        }
//
//        Volley.newRequestQueue(this@Display_chat).add(request)
//    }


    fun sendMediaMessage(
        s3Url: String,
        fileType: String,
        waId: String,
        phoneNumberId: String,
        accessToken: String
    ) {
        if (waId.isBlank()) {
            Toast.makeText(this, "Recipient waId is missing", Toast.LENGTH_SHORT).show()
            Log.e("SEND_MEDIA", "waId is blank, aborting")
            return
        }

        val messageType = when {
            fileType.startsWith("image") -> "image"
            fileType.startsWith("video") -> "video"
            fileType.startsWith("application") || fileType.startsWith("text") -> "document"
            else -> {
                Toast.makeText(this, "Unsupported media type: $fileType", Toast.LENGTH_SHORT).show()
                Log.e("SEND_MEDIA", "Unsupported fileType: $fileType")
                return
            }
        }

        val url = "https://waba.mpocket.in/api/$phoneNumberId/messages"

        val mediaPayload = JSONObject().apply {
            put("link", s3Url)
            // Optional filename (only applies to documents)
            if (messageType == "document") put("filename", "file.${MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)}")
        }

        val body = JSONObject().apply {
            put("messaging_product", "whatsapp")
            put("to", waId)
            put("type", messageType)
            put(messageType, mediaPayload)
        }

        Log.d("SEND_MEDIA_PAYLOAD", body.toString(2))

        val request = object : JsonObjectRequest(Method.POST, url, body,
            Response.Listener {
                Log.d("MediaSend", "Success: $it")
                Toast.makeText(this@Display_chat, "Media sent successfully", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener {
                val errorBody = it.networkResponse?.data?.let { data -> String(data) }
                Log.e("MediaSend", "Failed", it)
                Log.e("MediaSend", "Error Body: $errorBody")
                Toast.makeText(this@Display_chat, "Media send failed", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(this@Display_chat).add(request)
    }


}