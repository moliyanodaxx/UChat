package com.example.uchat.data.remote

import android.util.Log
import com.example.uchat.data.model.MsgType
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketClient {

    private val TAG = "WebSocketClient"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messages = MutableSharedFlow<JsonObject>(extraBufferCapacity = 128)
    val messages: SharedFlow<JsonObject> = _messages

    private val _connectionState = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 8)
    val connectionState: SharedFlow<Boolean> = _connectionState

    var serverUrl: String = ""

    private var shouldReconnect = true
    private var reconnectJob: Job? = null

    fun connect(url: String) {
        serverUrl = url
        shouldReconnect = true
        doConnect(url)
    }

    private fun doConnect(url: String) {
        try {
            val request = Request.Builder().url(url).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "Connected to $url")
                    scope.launch { _connectionState.emit(true) }
                    startHeartbeat()
                }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d(TAG, "Received: $text")
                try {
                    val json = JsonParser.parseString(text).asJsonObject
                    scope.launch { _messages.emit(json) }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error: $text", e)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Disconnected: $reason")
                stopHeartbeat()
                scope.launch { _connectionState.emit(false) }
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WS failure", t)
                stopHeartbeat()
                scope.launch { _connectionState.emit(false) }
                scheduleReconnect()
            }
        })
        } catch (e: Exception) {
            Log.e(TAG, "connect() failed: $url", e)
            scope.launch { _connectionState.emit(false) }
        }
    }

    fun send(obj: JsonObject) {
        val text = gson.toJson(obj)
        Log.d(TAG, "Sending: $text")
        webSocket?.send(text)
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        stopHeartbeat()
        webSocket?.close(1000, "User disconnect")
        webSocket = null
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect || serverUrl.isEmpty()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            if (shouldReconnect) {
                Log.d(TAG, "Reconnecting...")
                doConnect(serverUrl)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(25_000)
                val hb = JsonObject().apply { addProperty("type", MsgType.HEARTBEAT) }
                send(hb)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun isConnected() = webSocket != null
}
