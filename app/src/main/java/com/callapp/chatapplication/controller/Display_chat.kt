package com.callapp.chatapplication.controller

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
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
import com.callapp.chatapplication.R
import com.callapp.chatapplication.databinding.ActivityDisplayChatBinding
import com.callapp.chatapplication.model.Message
import com.callapp.chatapplication.view.MessageAdapter
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Display_chat : AppCompatActivity() {
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
                            url = null
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
                        val messageBody = obj.optString("message_body")
                        val imageUrl = obj.optString("url").ifEmpty { obj.optString("file_url") }
                        val timestamp = obj.optLong("timestamp", System.currentTimeMillis() / 1000)

                        if (messageBody.isNullOrEmpty() && messageType != "image") {
                            continue
                        }
                        val sender = obj.optString("sender")

                        val message = Message(
                            sender = if (sender.isNullOrEmpty()) null else sender,
                            messageBody = messageBody,
                            messageType = messageType,
                            timestamp = timestamp,
                            url = imageUrl
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
                val result = mutableListOf<JSONObject>()
                for (i in 0 until (templates?.length() ?: 0)) {
                    templates?.getJSONObject(i)?.let { result.add(it) }
                }
                onResult(result)
            },
            { error ->
                onError(error.message ?: "Template fetch failed")
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

        val templateNames = templateList.map { it.optString("name") }
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, templateNames)
        spinnerTemplates.adapter = adapter

        val inputFields = dialogView.findViewById<LinearLayout>(R.id.inputFieldsLayout)
        var selectedTemplate: JSONObject? = null
        var userInputs = mutableListOf<String>()

        spinnerTemplates.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                Log.d("TEMPLATE_UI", "Spinner selected position: $pos")

                selectedTemplate = templateList[pos]
                inputFields.removeAllViews()
                userInputs.clear()

                selectedTemplate?.let { safeTemplate ->
                    Log.d(
                        "TEMPLATE_UI",
                        "Calling renderTemplateUI for: ${safeTemplate.optString("name")}"
                    )
                    renderTemplateUI(safeTemplate, inputFields, this@Display_chat, userInputs)
                }

                val components = try {
                    selectedTemplate?.optJSONArray("components") ?: JSONArray()
                } catch (e: Exception) {
                    JSONArray()
                }

                val bodyComp = (0 until components.length())
                    .mapNotNull { components.optJSONObject(it) }
                    .find { it.optString("type") == "BODY" }

                val text = bodyComp?.optString("text") ?: ""
                val placeholderCount = Regex("\\{\\{\\d+\\}\\}").findAll(text).count()


                for (i in 0 until placeholderCount) {
                    val input = EditText(this@Display_chat)
                    input.hint = "Field ${i + 1}"
                    input.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    // Add input to layout
                    inputFields.addView(input)

                    // Add empty string to maintain userInputs size
                    userInputs.add("")

                    val index = i // capture index correctly
                    input.addTextChangedListener(object : TextWatcher {

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            if (index < userInputs.size) {
                                userInputs[index] = s.toString()
                            }
                        }

                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun afterTextChanged(s: Editable?) {}

                    })

                }
            }


            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        buttonCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        buttonSend.setOnClickListener {
            selectedTemplate?.let {
                sendTemplateMessage(it, userInputs)
            }
            alertDialog.dismiss()
        }

        alertDialog.show()
        alertDialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

    }

    fun sendTemplateMessage(template: JSONObject, inputs: List<String>) {
        val name = template.optString("name")
        val to = intent.getStringExtra("wa_id_or_sender") ?: return
        val componentsArray = JSONArray()

        val bodyParams = JSONArray()
        inputs.forEach {
            bodyParams.put(JSONObject().put("type", "text").put("text", it))
        }

        componentsArray.put(JSONObject().put("type", "body").put("parameters", bodyParams))

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

        val url = "https://waba.mpocket.in/api/361462453714220/messages"
        val request = object : JsonObjectRequest(Method.POST, url, payload,
            { response ->
                Toast.makeText(this, "Template sent", Toast.LENGTH_SHORT).show()
            },
            { error ->
                val errData = error.networkResponse?.data?.let { String(it) }
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
                    Log.d("TEMPLATE_UI", "Header format: $format")
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
                            val image = ImageView(context).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 400
                                )
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageResource(R.drawable.baseline_attach_file_24)
                            }
                            layout.addView(image)
                        }

                    }
                }

                "BODY" -> {
                    val bodyText = comp.optString("text")
                    val placeholderCount = Regex("\\{\\{\\d+\\}\\}").findAll(bodyText).count()

                    for (j in 0 until placeholderCount) {

                        val input = EditText(context).apply {
                            hint = "Field ${j + 1}"
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            isFocusable = true
                            isFocusableInTouchMode = true
                            isClickable = true
                            requestFocus()  // <--- add this to trigger soft keyboard
                        }
                        layout.addView(input)
                        userInputs.add("")

                        val index = j
                        input.addTextChangedListener(object : TextWatcher {
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                userInputs[index] = s.toString()
                            }

                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun afterTextChanged(s: Editable?) {}
                        })
                    }

                    // Then add the preview below
                    val preview = TextView(context).apply {
                        text = bodyText
                        setPadding(0, 10, 0, 10)
                    }
                    layout.addView(preview)
                }


                "FOOTER" -> {
                    val footer = TextView(context).apply {
                        text = comp.optString("text")
                        Log.d("TEMPLATE_UI", "Footer: ${comp.optString("text")}")
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
                            Log.d(
                                "TEMPLATE_UI",
                                "Buttons count: ${comp.optJSONArray("buttons")?.length()}"
                            )
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
    fun formatTimestamp(timestamp: String): String {
        val tsLong = timestamp.toLong() * 1000
        val date = Date(tsLong)
        val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return format.format(date)
    }


}