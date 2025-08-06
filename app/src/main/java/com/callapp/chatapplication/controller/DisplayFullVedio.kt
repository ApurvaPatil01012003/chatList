package com.callapp.chatapplication.controller

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.exoplayer.ExoPlayer
import com.callapp.chatapplication.R
import com.callapp.chatapplication.databinding.ActivityDisplayFullVedioBinding
import androidx.media3.common.MediaItem



class DisplayFullVedio : AppCompatActivity() {
    private lateinit var binding: ActivityDisplayFullVedioBinding
    private var player: ExoPlayer? = null
    private var isToolbarVisible = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDisplayFullVedioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val videoUrl = intent.getStringExtra("video_url")
        val sharedPreferences = getSharedPreferences("ContactPrefs", MODE_PRIVATE)
        val name = sharedPreferences.getString("contact_name", "Unknown")
        val number = sharedPreferences.getString("contact_number", "Unknown")

        binding.exoPlayerView.setOnClickListener{
            isToolbarVisible = !isToolbarVisible
            binding.toolbar.visibility = if (isToolbarVisible) View.VISIBLE else View.GONE
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "$name"
        supportActionBar?.subtitle = "$number"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }


        videoUrl?.let { playVideo(it) }
    }



    private fun playVideo(url: String) {
        player = ExoPlayer.Builder(this).build().also {
            binding.exoPlayerView.player = it
            val mediaItem = MediaItem.fromUri(url)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.playWhenReady = true
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
