package com.callapp.chatapplication.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
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
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setSupportActionBar(binding.toolbar)


        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setIconifiedByDefault(false)
        searchView.isIconified = false
        searchView.clearFocus()
        searchView.queryHint = "Search by name or number"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filterList(newText.orEmpty())
                return true
            }
        })



        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        chatController = ChatController(this)


        adapter = ChatAdapter(emptyList()) { chat ->
            val intent = Intent(this, Display_chat::class.java)
            intent.putExtra("contact_name", chat.contact_name)
            intent.putExtra("wa_id_or_sender", chat.wa_id_or_sender)
            intent.putExtra("message_count", chat.message_count)
            startActivity(intent)
        }
        recyclerView.adapter = adapter


        val phoneNumberId = "361462453714220"
        val accessToken = "Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"

        chatController.fetchChats(phoneNumberId, accessToken,
            onSuccess = { chatList ->
                binding.textTotal.text="Total : ${chatList.size}"
                val active = chatList.count { it.active_last_24_hours }
                binding.textActive.text = "Active: $active"

                adapter.updateList(chatList)
            },
            onError = {
                Toast.makeText(this, "Error: $it", Toast.LENGTH_LONG).show()
            })

    }
}
