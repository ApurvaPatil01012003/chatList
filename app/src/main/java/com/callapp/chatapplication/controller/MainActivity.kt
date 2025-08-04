package com.callapp.chatapplication.controller

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.callapp.chatapplication.R
import com.callapp.chatapplication.databinding.ActivityMainBinding
import com.callapp.chatapplication.view.ChatAdapter
import androidx.appcompat.widget.SearchView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var chatController: ChatController
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private var selectedPhoneNumberId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        @Suppress("UNCHECKED_CAST")
        val phoneNumberList = intent.getSerializableExtra("phone_number_list") as? ArrayList<Pair<Long, String>> ?: arrayListOf()
        val phoneNumberIds = phoneNumberList.map { it.first.toString() }


        val spinner: Spinner = findViewById(R.id.spinner_Phone_no_id)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, phoneNumberIds)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: android.view.View,
                position: Int,
                id: Long
            ) {
                selectedPhoneNumberId = phoneNumberIds[position] // Save selected ID
                val accessToken = "Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"

                chatController.fetchChats(selectedPhoneNumberId!!, accessToken,
                    onSuccess = { chatList ->
                        binding.textTotal.text = "Total : ${chatList.size}"
                        val active = chatList.count { it.active_last_24_hours }
                        binding.textActive.text = "Active: $active"

                        chatAdapter.updateList(chatList)
                    },
                    onError = {
                        Toast.makeText(this@MainActivity, "Error: $it", Toast.LENGTH_LONG).show()
                    })
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }



        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        chatController = ChatController(this)

        chatAdapter = ChatAdapter(emptyList()) { chat ->
            val intent = Intent(this, Display_chat::class.java)
            intent.putExtra("contact_name", chat.contact_name)
            intent.putExtra("wa_id_or_sender", chat.wa_id_or_sender)
            intent.putExtra("phoneNumberId", selectedPhoneNumberId)
            intent.putExtra("message_count", chat.message_count)
            intent.putExtra("user_name", chat.user_name)
            intent.putExtra("active_last_24_hours", chat.active_last_24_hours)
            intent.putExtra("first_message_date",chat.first_message_date)
            intent.putExtra("last_message_date",chat.last_message_date)
            intent.putExtra("Total_pages",chat.total_pages)
            startActivity(intent)
        }
        recyclerView.adapter = chatAdapter


        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setIconifiedByDefault(false)
        searchView.isIconified = false
        searchView.clearFocus()
        searchView.queryHint = "Search by name or number"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                chatAdapter.filterList(newText.orEmpty())
                return true
            }
        })

        if (phoneNumberIds.isNotEmpty()) {
            val phoneNumberId = phoneNumberIds[0]
            val accessToken = "Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"

            chatController.fetchChats(phoneNumberId, accessToken,
                onSuccess = { chatList ->
                    binding.textTotal.text = "Total : ${chatList.size}"
                    val active = chatList.count { it.active_last_24_hours }
                    binding.textActive.text = "Active: $active"

                    chatAdapter.updateList(chatList)
                },
                onError = {
                    Toast.makeText(this, "Error: $it", Toast.LENGTH_LONG).show()
                })
        }
    }
}
