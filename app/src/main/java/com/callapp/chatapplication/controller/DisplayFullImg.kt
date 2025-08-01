package com.callapp.chatapplication.controller

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.callapp.chatapplication.R
import com.callapp.chatapplication.databinding.ActivityDisplayFullImgBinding

class DisplayFullImg : AppCompatActivity() {
    private lateinit var binding: ActivityDisplayFullImgBinding
    private var isToolbarVisible = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDisplayFullImgBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val imageUrl = intent.getStringExtra("image_url")
        val sharedPreferences = getSharedPreferences("ContactPrefs", MODE_PRIVATE)
        val name = sharedPreferences.getString("contact_name", "Unknown")
        val number = sharedPreferences.getString("contact_number", "Unknown")
        Log.d("ImageUrl","img is : $imageUrl")

        setSupportActionBar(binding.toolbar)

        supportActionBar?.title = "$name"
        supportActionBar?.subtitle = "$number"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.main.setOnClickListener {
            isToolbarVisible = !isToolbarVisible
            binding.toolbar.visibility = if (isToolbarVisible) View.VISIBLE else View.GONE
        }


        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.fullImageView)
        }

    }
}