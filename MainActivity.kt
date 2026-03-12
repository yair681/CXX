package com.whatsappwatch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {

    private lateinit var qrImageView: ImageView
    private lateinit var statusText: TextView
    private lateinit var client: OkHttpClient
    private var webSocket: WebSocket? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // ← שנה לכתובת Render שלך
    private val BACKEND_URL = "wss://YOUR-APP-NAME.onrender.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        qrImageView = findViewById(R.id.qrImage)
        statusText = findViewById(R.id.statusText)

        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        connectWebSocket()
    }

    private fun connectWebSocket() {
        updateStatus("מתחבר...")
        
        val request = Request.Builder()
            .url(BACKEND_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                updateStatus("ממתין לQR...")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "qr" -> {
                            val base64Data = json.getString("data")
                                .replace("data:image/png;base64,", "")
                            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            showQR(bitmap)
                        }
                        "connected" -> {
                            updateStatus("✅ מחובר!")
                            handler.post { qrImageView.visibility = View.GONE }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WA", "Parse error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                updateStatus("שגיאה, מנסה שוב...")
                handler.postDelayed({ connectWebSocket() }, 5000)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handler.postDelayed({ connectWebSocket() }, 3000)
            }
        })
    }

    private fun showQR(bitmap: Bitmap) {
        handler.post {
            qrImageView.visibility = View.VISIBLE
            qrImageView.setImageBitmap(bitmap)
            statusText.text = "סרוק עם WhatsApp"
        }
    }

    private fun updateStatus(text: String) {
        handler.post { statusText.text = text }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.cancel()
        client.dispatcher.executorService.shutdown()
    }
}
