package com.callapp.chatapplication.controller

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.NumberParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Display_chat : AppCompatActivity() {
    private lateinit var binding: ActivityDisplayChatBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter

    //  val phoneNumberId = "361462453714220"
    lateinit var phoneNumberId: String

    private lateinit var waId: String
    private var isActiveLast24Hours: Boolean = true
    val templateBodyMap = mutableMapOf<String, String>()
    val templateButtonsMap = mutableMapOf<String, JSONArray>()
    val templateFullMap = mutableMapOf<String, JSONObject>()
    private var name: String? = null
    private var number: String? = null
    private var userName: String? = null
    private var fMsgDate: String?=null
    private var lMsgDate :String?=null
    private var flagEmoji: String?=null
    private var currentPage = 1
    private var isLoading = false
    private var allMessagesLoaded = false
    private val allMessages = mutableListOf<Message>()

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


        name = intent.getStringExtra("contact_name")
        number = intent.getStringExtra("wa_id_or_sender")
        waId = intent.getStringExtra("wa_id_or_sender") ?: ""
        phoneNumberId = intent.getStringExtra("phoneNumberId") ?: ""
        userName = intent.getStringExtra("user_name") ?: ""
        fMsgDate=intent.getStringExtra("first_message_date")?:""
        lMsgDate =intent.getStringExtra("last_message_date")?:""
        loadMessages(page = 1)
        if (!userName.isNullOrBlank() && userName != "null") {
            val initial = userName!!.trim().firstOrNull()?.uppercaseChar() ?: 'N'
            binding.txtTagInitial.text = initial.toString()
            binding.txtTagInitial.visibility = View.VISIBLE
        } else {
            binding.txtTagInitial.visibility = View.GONE
        }


        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.black))
        binding.txtTag.text = userName


        supportActionBar?.title = "$name"
        supportActionBar?.subtitle = "        $number"

        binding.recyclerViewMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (!isLoading && !allMessagesLoaded && layoutManager.findFirstVisibleItemPosition() == 0) {
                    loadMessages(page = ++currentPage, appendToTop = true)
                }
            }
        })

        binding.imgDown.setOnClickListener {
            val itemCount = binding.recyclerViewMessages.adapter?.itemCount ?: 0
            if (itemCount > 0) {
                binding.recyclerViewMessages.smoothScrollToPosition(itemCount - 1)
            }
        }

        binding.recyclerViewMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()
                val totalItemCount = recyclerView.adapter?.itemCount ?: 0


                binding.imgDown.visibility =
                    if (lastVisibleItem < totalItemCount - 1) View.VISIBLE else View.GONE
            }
        })



        if (name != null && number != null) {
            val sharedPreferences = getSharedPreferences("ContactPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("contact_name", name)
            editor.putString("contact_number", number)
            editor.apply()
        }

        val countryIso = getCountryIsoFromPhoneNumber(number ?: "")
        if (countryIso != null) {
            flagEmoji = countryCodeToFlagEmoji(countryIso)
            binding.toolbar.title = "$flagEmoji $name"
        }

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

//                if (!isActiveLast24Hours) {
//                    Toast.makeText(
//                        this,
//                        "Message failed: Contact inactive for 24+ hours",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return@setOnClickListener
//                }
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
                    for (j in 0 until totalPlaceholders) {
                        val value = bodyInputs.getOrNull(j) ?: ""
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
        val regex = Regex("\\{\\{(\\d+)\\}\\}")
        return regex.replace(templateBody) { match ->
            val index = match.groupValues[1].toIntOrNull()?.minus(1) ?: return@replace match.value
            values.getOrNull(index) ?: ""
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

        var headerComponent: JSONObject? = null
        var bodyComponent: JSONObject? = null
        var footerComponent: JSONObject? = null
        var buttonsComponent: JSONObject? = null
        var bodyText = ""
        var bodyMatches: List<MatchResult> = emptyList()
        var bodyExampleArray: JSONArray? = null

        for (i in 0 until components.length()) {
            val comp = components.optJSONObject(i)
            when (comp?.optString("type")) {
                "HEADER" -> headerComponent = comp
                "BODY" -> bodyComponent = comp
                "FOOTER" -> footerComponent = comp
                "BUTTONS" -> buttonsComponent = comp
            }
        }

        val hasImageHeader = headerComponent?.optString("format") == "IMAGE"
        val startIndex = if (hasImageHeader) 1 else 0

        if (hasImageHeader) {
            val defaultImageUrl = headerComponent
                ?.optJSONObject("example")
                ?.optJSONArray("header_handle")
                ?.optString(0) ?: ""

            userInputs.add(defaultImageUrl)

            val fieldLayout = LayoutInflater.from(context)
                .inflate(R.layout.single_input_field, null)
            val imageEditText = fieldLayout.findViewById<EditText>(R.id.dynamicInput)
            imageEditText.hint = "Image URL"
            imageEditText.inputType = InputType.TYPE_TEXT_VARIATION_URI

            if (defaultImageUrl.isNotBlank() && !defaultImageUrl.contains("scontent.whatsapp.net")) {
                imageEditText.setText(defaultImageUrl)
            }

            layout.addView(fieldLayout)

            imageEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val newUrl = s.toString().trim()
                    userInputs[0] = newUrl
                    updateBodyPreview(
                        bodyText,
                        bodyMatches,
                        userInputs,
                        layout,
                        context,
                        hasImageHeader
                    )
                }
            })
        }

        if (bodyComponent != null) {
            bodyText = bodyComponent.optString("text")
            val regex = Regex("\\{\\{(\\d+)\\}\\}")
            bodyMatches = regex.findAll(bodyText).toList()
            bodyExampleArray = bodyComponent.optJSONObject("example")
                ?.optJSONArray("body_text")
                ?.optJSONArray(0)

            for (i in bodyMatches.indices) {
                val defaultValue = bodyExampleArray?.optString(i) ?: ""
                val inputIndex = startIndex + i
                if (userInputs.size <= inputIndex) userInputs.add(defaultValue)
                else userInputs[inputIndex] = defaultValue

                val placeholderIndex = bodyMatches[i].groupValues[1].toInt()

                val fieldLayout = LayoutInflater.from(context)
                    .inflate(R.layout.single_input_field, null)
                val editText = fieldLayout.findViewById<EditText>(R.id.dynamicInput)
                editText.hint = "Enter value for {{${placeholderIndex}}}"
                editText.inputType = InputType.TYPE_CLASS_TEXT
                editText.setText(defaultValue)

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
                        userInputs[inputIndex] = s.toString()
                        updateBodyPreview(
                            bodyText,
                            bodyMatches,
                            userInputs,
                            layout,
                            context,
                            hasImageHeader
                        )
                    }
                })

                layout.addView(fieldLayout)
            }
        }

        updateBodyPreview(bodyText, bodyMatches, userInputs, layout, context, hasImageHeader)

        footerComponent?.optString("text")?.let { footerText ->
            val footer = TextView(context).apply {
                text = footerText
                setTextColor(Color.GRAY)
                setPadding(0, 10, 0, 10)
            }
            layout.addView(footer)
        }

        buttonsComponent?.optJSONArray("buttons")?.let { buttons ->
            for (b in 0 until buttons.length()) {
                val btn = buttons.getJSONObject(b)
                val buttonText = btn.optString("text", "Action")
                val button = Button(context).apply {
                    text = buttonText
                }
                layout.addView(button)
            }
        }
    }

    private fun updateBodyPreview(
        bodyText: String,
        matches: List<MatchResult>,
        userInputs: List<String>,
        layout: LinearLayout,
        context: Context,
        hasImageHeader: Boolean
    ) {
        // Replace placeholders with current inputs
        val previewText = buildString {
            var lastIndex = 0
            for ((i, match) in matches.withIndex()) {
                val idx = i + if (hasImageHeader) 1 else 0
                append(bodyText.substring(lastIndex, match.range.first))
                append(userInputs.getOrNull(idx) ?: "")
                lastIndex = match.range.last + 1
            }
            append(bodyText.substring(lastIndex))
        }

        // Remove previous preview if exists
        for (i in layout.childCount - 1 downTo 0) {
            val view = layout.getChildAt(i)
            if (view.tag == "previewBox") {
                layout.removeViewAt(i)
            }
        }

        // Create container card
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(12, 12, 12, 20)
            }
            setPadding(16, 16, 16, 16)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 24f
                setStroke(2, Color.LTGRAY)
            }
            elevation = 8f
            tag = "previewBox"
        }

        // Add image if exists
        if (hasImageHeader) {
            val imageUrl = userInputs.getOrNull(0)
            if (!imageUrl.isNullOrBlank() && imageUrl.startsWith("http")) {
                val previewImage = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 120f, context.resources.displayMetrics
                        ).toInt()
                    ).apply {
                        bottomMargin = 10
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    background = GradientDrawable().apply {
                        setStroke(1, Color.LTGRAY)
                        cornerRadius = 16f
                    }
                }

                Glide.with(context).load(imageUrl).into(previewImage)
                card.addView(previewImage)
            }
        }

        // Add scrollable text preview
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 140f, context.resources.displayMetrics
                ).toInt()
            ).apply {
                topMargin = 8
            }
            isVerticalScrollBarEnabled = true
            isNestedScrollingEnabled = true
            overScrollMode = ScrollView.OVER_SCROLL_IF_CONTENT_SCROLLS
            background = GradientDrawable().apply {
                setStroke(1, Color.LTGRAY)
                cornerRadius = 16f
            }
        }

        val preview = TextView(context).apply {
            text = previewText
            setTextColor(Color.DKGRAY)
            textSize = 14f
            setPadding(12, 12, 12, 12)
            setLineSpacing(6f, 1.1f)
        }

        scrollView.addView(preview)
        card.addView(scrollView)
        layout.addView(card)
    }
    fun loadMessages(page: Int = 1, appendToTop: Boolean = false) {
        val number = intent.getStringExtra("wa_id_or_sender") ?: return
        val url = "https://waba.mpocket.in/api/phone/get/$phoneNumberId/$number/$page"

        if (isLoading || allMessagesLoaded) return
        isLoading = true

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                isLoading = false

                val prefs = getSharedPreferences("image_url_cache", Context.MODE_PRIVATE)
                val newMessages = mutableListOf<Message>()

                if (response.length() == 0) {
                    allMessagesLoaded = true
                    Log.d("MESSAGE_COUNT", "Total messages loaded: ${allMessages.size}")

                    val timestamps = allMessages.map { it.timestamp }
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val minDate = timestamps.minOrNull()?.let { sdf.format(Date(it * 1000)) }
                    val maxDate = timestamps.maxOrNull()?.let { sdf.format(Date(it * 1000)) }
                    Log.d("MESSAGE_RANGE", "Messages from: $minDate → $maxDate")
                    return@JsonArrayRequest
                }

                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)
                    val messageType = obj.optString("message_type") ?: "text"
                    val extraInfoStr = obj.optString("extra_info")

                    val messageBody = when (messageType) {
                        "template" -> {
                            try {
                                val extraInfo = JSONObject(extraInfoStr)
                                val name = extraInfo.optString("name")
                                val components = extraInfo.optJSONArray("components")
                                val body = (0 until (components?.length() ?: 0))
                                    .mapNotNull { components?.optJSONObject(it) }
                                    .firstOrNull { it.optString("type") == "body" }

                                val params = body?.optJSONArray("parameters")
                                val values = (0 until (params?.length() ?: 0)).map { i -> params!!.getJSONObject(i).optString("text") }
                                val templateText = templateBodyMap[name] ?: name
                                values.foldIndexed(templateText) { index, acc, v -> acc.replace("{{${index + 1}}}", v) }
                            } catch (e: Exception) {
                                Log.e("TEMPLATE_PARSE", "Error parsing template body", e)
                                "Template Message"
                            }
                        }
                        else -> obj.optString("message_body", "")
                    }

                    var sender = obj.optString("sender") ?: ""
                    if (sender.isEmpty()) {
                        val waIdFromMsg = obj.optString("wa_id") ?: ""
                        val currentUserWaId = intent.getStringExtra("wa_id_or_sender") ?: ""
                        if (waIdFromMsg == currentUserWaId) sender = "you"
                    }

                    var imageUrl = obj.optString("url").takeIf { it.isNotBlank() && it != "null" } ?: obj.optString("file_url")

                    var caption: String? = null
                    if (!extraInfoStr.isNullOrBlank()) {
                        try {
                            val extra = JSONObject(extraInfoStr)
                            caption = extra.optString("caption", null)
                        } catch (_: Exception) {}
                    }
                    if (caption.isNullOrBlank() && messageType == "document") {
                        caption = obj.optString("filename", null)
                    }
                    if (caption.isNullOrBlank() && messageType in listOf("image", "video", "document")) {
                        caption = messageBody
                    }

                    if (messageType == "image" && (imageUrl.isNullOrBlank() || imageUrl == "null")) {
                        try {
                            val extraInfo = JSONObject(extraInfoStr)
                            val mediaId = extraInfo.optString("media_id")
                            if (!mediaId.isNullOrBlank()) {
                                imageUrl = prefs.getString(mediaId, null)
                            }
                        } catch (_: Exception) {}
                    }

                    if (messageType == "template" && (imageUrl.isNullOrBlank() || imageUrl == "null")) {
                        imageUrl = resolveTemplateImageFromPrefs(prefs, extraInfoStr)
                    }

                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis() / 1000)

                    if ((messageBody.isEmpty() || messageBody == "null") && messageType !in listOf("image", "video", "document")) {
                        continue
                    }

                    val extraInfoJson = try { JSONObject(extraInfoStr) } catch (_: Exception) { JSONObject() }
                    if (!extraInfoJson.has("buttons")) {
                        val name = extraInfoJson.optString("name")
                        val storedButtons = templateButtonsMap[name]
                        if (storedButtons != null) {
                            val buttonsUIArray = JSONArray()
                            for (k in 0 until storedButtons.length()) {
                                val btn = storedButtons.getJSONObject(k)
                                val btnJson = JSONObject().apply {
                                    put("index", k)
                                    put("text", btn.optString("text", "Action"))
                                    put("type", btn.optString("type"))
                                    put("param", btn.optString("param"))
                                }
                                buttonsUIArray.put(btnJson)
                            }
                            extraInfoJson.put("buttons", buttonsUIArray)
                        }
                    }

                    val componentDataJson: String? = try {
                        val name = extraInfoJson.optString("name")
                        templateFullMap[name]?.optString("component_data")
                    } catch (_: Exception) { null }


                    newMessages.add(
                        Message(
                            sender = if (sender.isEmpty()) null else sender,
                            messageBody = messageBody,
                            messageType = messageType,
                            timestamp = timestamp,
                            url = imageUrl,
                            recipientId = obj.optString("recipient_id"),
                            waId = obj.optString("wa_id"),
                            sent = obj.optInt("sent", 0),
                            delivered = obj.optInt("delivered", 0),
                            read = obj.optInt("read", 0),
                            caption = caption,
                            extraInfo = extraInfoJson.toString(),
                            componentData = componentDataJson,

                            )
                    )
                }

                Log.d("PAGE_DEBUG", "Loaded page $page with ${newMessages.size} messages")

                if (appendToTop) {
                    allMessages.addAll(0, newMessages)
                    allMessages.sortBy { it.timestamp }
                    adapter.setMessages(allMessages)


                    binding.recyclerViewMessages.scrollToPosition(newMessages.size)
                }
                else {
                    allMessages.clear()
                    allMessages.addAll(newMessages)
                    allMessages.sortBy { it.timestamp }
                    adapter.setMessages(allMessages)
                    binding.recyclerViewMessages.scrollToPosition(allMessages.size - 1)
                }


            },
            { error ->
                isLoading = false
                Log.e("LOAD_ERROR", "Failed to load page $page", error)
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

        popupView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        val popupHeight = popupView.measuredHeight
        popupWindow.showAtLocation(
            binding.buttonAttach,
            Gravity.NO_GRAVITY,
            x,
            y - popupHeight - 60
        )


        val PICK_IMAGES_REQUEST_CODE = 1001
        val PICK_VIDEO_REQUEST_CODE = 1002
        val PICK_DOCUMENT_REQUEST_CODE = 1003

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

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_DOCUMENT_REQUEST_CODE)
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"


            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w("URI_PERMISSION", "Failed to persist permission: ${e.message}")
            }

            when (requestCode) {
                1001, 1003, 1002 -> {
                    showImagePreviewDialog(uri, mimeType)
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
        accessToken: String,
        caption: String? = null
    ) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileData = inputStream?.readBytes() ?: return
        Log.d("UPLOAD_DEBUG", "URI received: $uri")

        val mimeType = context.contentResolver.getType(uri)
        Log.d("UPLOAD_DEBUG", "Mime type: $mimeType")

        val fileSizeMB = fileData.size / (1024.0 * 1024.0)
        Log.d("UPLOAD_DEBUG", "File size: %.2f MB".format(fileSizeMB))


        val fileName = "upload_" + System.currentTimeMillis() + "." +
                MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)

        Log.d("UPLOAD_DEBUG", "file name : $fileName")

        val request = MultipartRequest(
            url = "https://waba.mpocket.in/api/$phoneNumberId/upload-file",
            fileData = fileData,
            fileName = fileName,
            fileType = fileType,
            listener = Response.Listener { response ->

                val metaMediaId = response.optString("metaMediaId", "")
                val s3Url = response.optString("s3Url", "")
                Log.d("UPLOAD_RESPONSE", "Response: $response")

                if (metaMediaId.isEmpty() || s3Url.isEmpty()) {
                    Log.e("UPLOAD_FAIL", "Missing metaMediaId or s3Url in response: $response")
                    Toast.makeText(
                        context,
                        "Upload failed: incomplete response",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Listener
                }

                val prefs = context.getSharedPreferences("image_url_cache", Context.MODE_PRIVATE)
                prefs.edit().putString(metaMediaId, s3Url).apply()
                Log.d("PREF_WRITE", "Saved $metaMediaId → $s3Url")

                sendMediaMessage(s3Url, fileType, waId, phoneNumberId, accessToken, caption)

                val messageType = when {
                    fileType.startsWith("image") -> "image"
                    fileType.startsWith("video") -> "video"
                    fileType.startsWith("application") || fileType.startsWith("text") -> "document"
                    else -> "unknown"
                }

                val messageBody = when (messageType) {
                    "image" -> "Image"
                    "video" -> "Video"
                    "document" -> "Document"
                    else -> "Media"
                }

                val extraInfoJson = JSONObject().apply {
                    put("media_id", metaMediaId)
                    caption?.let { put("caption", it) }
                }

                val sentMessage = Message(
                    sender = "you",
                    messageBody = messageBody,
                    messageType = messageType,
                    timestamp = System.currentTimeMillis() / 1000,
                    url = s3Url,
                    recipientId = waId,
                    waId = waId,
                    caption = caption,
                    extraInfo = extraInfoJson.toString(),

                )

                Log.d("extraonfo", "$extraInfoJson")

                prefs.edit().putString(metaMediaId, s3Url).apply()

                prefs.edit().putString("caption_$metaMediaId", caption).apply()
                Log.d("extraonf", "Saved caption caption_$metaMediaId = $caption")




                adapter.addMessage(sentMessage)

                binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)

                Log.d("UPLOAD_SUCCESS", "metaMediaId: $metaMediaId, s3Url: $s3Url ")
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

    fun sendMediaMessage(
        s3Url: String,
        fileType: String,
        waId: String,
        phoneNumberId: String,
        accessToken: String,
        caption: String? = null
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

            if (messageType == "image" || messageType == "video" || messageType == "document") {
                if (!caption.isNullOrBlank()) {
                    put("caption", caption)
                }
            }

            if (messageType == "document") {
                val extension =
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType) ?: "file"
                put("filename", "file.$extension")
            }
        }


        val body = JSONObject().apply {
            put("messaging_product", "whatsapp")
            put("to", waId)
            put("type", messageType)
            put(messageType, mediaPayload)
        }


        Log.d("SEND_MEDIA_PAYLOAD", "Sending $messageType message to $waId with URL: $s3Url")
        Log.d("SEND_MEDIA_PAYLOAD", body.toString(2))

        val request = object : JsonObjectRequest(Method.POST, url, body,
            Response.Listener { response ->
                Log.d("MediaSend", "Success: $response")
                Toast.makeText(
                    this@Display_chat,
                    "$messageType sent successfully",
                    Toast.LENGTH_SHORT
                ).show()
            },
            Response.ErrorListener { error ->
                val errorBody = error.networkResponse?.data?.let { String(it) }
                val statusCode = error.networkResponse?.statusCode
                Log.e("MediaSend", "HTTP $statusCode Error: $errorBody", error)
                Toast.makeText(
                    this@Display_chat,
                    "$messageType send failed ($statusCode)",
                    Toast.LENGTH_SHORT
                ).show()
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


    private fun showImagePreviewDialog(uri: Uri, mimeType: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_send_image, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imagePreview)
        val captionEditText = dialogView.findViewById<EditText>(R.id.captionEditText)
        val btnSend = dialogView.findViewById<Button>(R.id.btnSend)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val documentPreview = dialogView.findViewById<LinearLayout>(R.id.documentPreview)
        val documentFileName = dialogView.findViewById<TextView>(R.id.documentFileName)
        val exoPlayerView = dialogView.findViewById<PlayerView>(R.id.exoPlayerView)

        imageView.visibility = View.GONE
        documentPreview.visibility = View.GONE
        exoPlayerView.visibility = View.GONE

        var player: ExoPlayer? = null

        when {
            mimeType.startsWith("image") -> {
                imageView.visibility = View.VISIBLE
                imageView.setImageURI(uri)
            }

            mimeType.startsWith("video") -> {
                exoPlayerView.visibility = View.VISIBLE
                player = ExoPlayer.Builder(this).build()
                exoPlayerView.player = player
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.playWhenReady = false
            }

            else -> {
                documentPreview.visibility = View.VISIBLE
                documentFileName.text = getFileNameFromUri(this, uri)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnSend.setOnClickListener {
            dialog.dismiss()
            player?.release()
            val caption = captionEditText.text.toString().trim()
            uploadFileAndSend(
                this,
                uri,
                mimeType,
                waId,
                phoneNumberId,
                "Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7",
                caption
            )
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            player?.release()
        }

        dialog.setOnDismissListener {
            player?.release()
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }


    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = "document"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndexOpenableColumnName()
            if (nameIndex != -1 && it.moveToFirst()) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }

    fun Cursor.getColumnIndexOpenableColumnName(): Int {
        return getColumnIndex("_display_name")
            .takeIf { it != -1 }
            ?: getColumnIndex(OpenableColumns.DISPLAY_NAME)
    }


    fun getCountryIsoFromPhoneNumber(rawNumber: String): String? {
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        return try {
            val formattedNumber = if (!rawNumber.startsWith("+")) "+$rawNumber" else rawNumber
            val numberProto = phoneNumberUtil.parse(formattedNumber, null)
            phoneNumberUtil.getRegionCodeForNumber(numberProto)
        } catch (e: NumberParseException) {
            null
        }
    }

    fun countryCodeToFlagEmoji(isoCode: String): String {
        return isoCode
            .uppercase()
            .map { char -> Character.toChars(0x1F1E6 - 'A'.code + char.code).concatToString() }
            .joinToString("")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.viewProfile -> {
                val profileIntent = Intent(this, ProfileActivity::class.java)
                profileIntent.putExtra("ContactName", name ?: "Unknown")
                profileIntent.putExtra("ContactNumber", number ?: "Unknown")
                profileIntent.putExtra("User_Name", userName)
                profileIntent.putExtra("first_message_date",fMsgDate)
                profileIntent.putExtra("last_message_date",lMsgDate)
                profileIntent.putExtra("Active",isActiveLast24Hours)
                profileIntent.putExtra("Flag",flagEmoji)
                startActivity(profileIntent)
                Log.d("DisplayActivity", "Name is : " + name)
                true


            }

            else -> super.onOptionsItemSelected(item)
        }
    }


}