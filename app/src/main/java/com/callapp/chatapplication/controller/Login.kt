package com.callapp.chatapplication.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.callapp.chatapplication.R
import com.callapp.chatapplication.databinding.ActivityLoginBinding
import org.json.JSONObject

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val jwt = sharedPref.getString("jwt_token", null)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString()
            val password = binding.edtPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

    }


    private fun loginUser(email: String, password: String) {
        val url = "https://api.tickzap.com/api/users/login"

        val requestBody = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->

                val success = response.optBoolean("success")
                if (success) {
                    val token = response.optString("token")
                    val jwt = response.optString("jwt")
                    val userType = response.optString("user_type")
                    val emailResponse = response.optString("email")
                    val wabaId = response.optLong("waba_id")

                    val wabasObj = response.optJSONObject("wabas")
                    val wabaDetails = wabasObj?.optJSONObject(wabaId.toString())

                    var wabaName = ""
                    val phoneNumberList = mutableListOf<Pair<Long, String>>()

                    wabaDetails?.let {
                        wabaName = it.optString("name")
                        val phoneNumbersArray = it.optJSONArray("phone_numbers")

                        if (phoneNumbersArray != null) {
                            for (i in 0 until phoneNumbersArray.length()) {
                                val phoneNumberObj = phoneNumbersArray.getJSONObject(i)
                                val phoneNumberId = phoneNumberObj.optLong("phone_number_id")
                                val appId = phoneNumberObj.optString("app_id")
                                phoneNumberList.add(phoneNumberId to appId)
                            }
                        }
                    }


                    val sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("token",token)
                        putString("jwt_token", jwt)
                        putString("user_type", userType)
                        putString("email", emailResponse)
                        putLong("waba_id", wabaId)
                        putString("waba_name", wabaName)
                        apply()
                    }


                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("phone_number_list", ArrayList(phoneNumberList))
                    intent.putExtra("Token",token)
                    startActivity(intent)
                    finish()
                    Log.d("PhoneNumberId","${ArrayList(phoneNumberList)}")
                    Log.d("Token","Token is : $token")

                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                if (error.networkResponse?.data != null) {
                    try {
                        val errorJson = String(error.networkResponse.data, Charsets.UTF_8)
                        val errorObj = JSONObject(errorJson)
                        val errorMessage = errorObj.optString("message", "Invalid email or password")
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Network error: ${error.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf("Content-Type" to "application/json")
            }
        }

        Volley.newRequestQueue(this).add(request)
    }


}