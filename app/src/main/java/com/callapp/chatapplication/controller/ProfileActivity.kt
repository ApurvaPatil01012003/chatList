package com.callapp.chatapplication.controller

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.callapp.chatapplication.R
import com.callapp.chatapplication.databinding.ActivityProfileBinding
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.black))

        var name = intent.getStringExtra("ContactName")
        binding.txtName.text=name
        var number = intent.getStringExtra("ContactNumber")
        binding.txtNumber.text="+$number"
        binding.phone.text="+$number"
        var User = intent.getStringExtra("User_Name")
        binding.username.text=User
        var firstMessage = intent.getStringExtra("first_message_date") ?: ""
        binding.txtFirstMsg.text = "First message: ${formatDate(firstMessage)}"

        var lastMessage = intent.getStringExtra("last_message_date") ?: ""
        binding.txtLastMsg.text = "Last message: ${formatDate(lastMessage)}"

        val initials = getCountryInitials("+$number")
        var countryFlag = intent.getStringExtra("Flag")?:""
        binding.country.text="$countryFlag $initials"
var Active24_hours=intent.getBooleanExtra("Active",true)
   if(Active24_hours)
   {
       binding.activeDot.visibility= View.VISIBLE
   }else{
       binding.activeDot.visibility= View.GONE
   }

    }

    fun formatDate(apiDate: String): String {
        return try {
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            apiFormat.timeZone = TimeZone.getTimeZone("UTC")

            val date = apiFormat.parse(apiDate)
            val outputFormat = SimpleDateFormat("M/d/yyyy, hh:mm:ss a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()

            outputFormat.format(date!!)
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    fun getCountryInitials(phoneNumber: String): String {
        return try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val numberProto = phoneUtil.parse(phoneNumber, null)
            val regionCode = phoneUtil.getRegionCodeForNumber(numberProto)
            regionCode ?: "??"
        } catch (e: NumberParseException) {
            "??"
        }
    }
}