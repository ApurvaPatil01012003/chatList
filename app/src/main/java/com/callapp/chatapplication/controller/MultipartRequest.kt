package com.callapp.chatapplication.controller

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

class MultipartRequest(
    url: String,
    private val fileData: ByteArray,
    private val fileName: String,
    private val fileType: String,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) : Request<JSONObject>(Method.POST, url, errorListener) {

    private val boundary = "apiclient-${System.currentTimeMillis()}"
    private val mimeType = "multipart/form-data;boundary=$boundary"
    private val responseListener = listener

    override fun getBodyContentType(): String = mimeType

    override fun getHeaders(): MutableMap<String, String> {
        return mutableMapOf(
            "Authorization" to "Bearer Vpv6mesdUaY3XHS6BKrM0XOdIoQu4ygTVaHmpKMNb29bc1c7"
        )
    }

    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        val writer = PrintWriter(OutputStreamWriter(bos, "UTF-8"), true)

        writer.append("--$boundary\r\n")
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
        writer.append("Content-Type: $fileType\r\n\r\n")
        writer.flush()
        bos.write(fileData)
        bos.write("\r\n--$boundary--\r\n".toByteArray())
        return bos.toByteArray()
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
        val json = JSONObject(String(response.data))
        return Response.success(json, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: JSONObject) {
        responseListener.onResponse(response)
    }
}
