package com.android.purebilibili.core.network.socket

import android.util.Log
import com.android.purebilibili.core.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.ByteBuffer
import kotlin.math.min
import kotlin.math.pow

/**
 * Bilibili 直播弹幕 WebSocket 客户端
 * 
 * 功能：
 * 1. 自动重连 (Exponential Backoff)
 * 2. 鉴权 (Auth)
 * 3. 心跳保活 (Heartbeat)
 * 4. 消息分发 (Backpressure Support)
 */
class LiveDanmakuClient(
    private val scope: CoroutineScope
) {
    private val TAG = "LiveDanmakuClient"
    private var webSocket: WebSocket? = null
    
    // 连接状态
    private val _isConnected = AtomicBoolean(false)
    val isConnected: Boolean get() = _isConnected.get()
    
    // 重连参数
    private var retryCount = 0
    private val MAX_RETRY_DELAY = 10_000L // 最大重连间隔 10秒
    private var reconnectJob: Job? = null
    
    companion object {
        private const val HEARTBEAT_INTERVAL = 30_000L // 30秒一次心跳
    }
    
    // 心跳任务
    private var heartbeatJob: Job? = null
    
    // 当前连接参数
    private var currentHostUrl: String = ""
    private var currentAuthBody: String = ""
    private var suppressReconnect: Boolean = false

    // 入站消息队列：串行解码，避免高频 onMessage 创建大量并发协程
    private val incomingFrames = Channel<ByteArray>(
        capacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var decodeJob: Job? = null
    
    // 消息流 - 使用 ExtraBufferCapacity + DROP_OLDEST 防止爆内存 (Backpressure)
    // 当缓冲满时丢弃旧消息，保证 UI 不会因为积压而卡死
    private val _messageFlow = MutableSharedFlow<DanmakuProtocol.Packet>(
        replay = 0,
        extraBufferCapacity = 200, // 缓冲区容纳 200 条消息
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageFlow = _messageFlow.asSharedFlow()
    
    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "🟢 WebSocket Connected: $currentHostUrl")
            _isConnected.set(true)
            retryCount = 0 // 重置重连计数
            suppressReconnect = false
            
            // 发送认证包
            sendAuthPacket()
            
            // 启动心跳
            startHeartbeat()
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onIncomingMessage(bytes)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "🔴 WebSocket Closed: $code - $reason")
            _isConnected.set(false)
            stopHeartbeat()
            // 只有非正常关闭且未标记抑制重连才重连
            if (code != 1000 && !suppressReconnect) {
                scheduleReconnect()
            }
            suppressReconnect = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "❌ WebSocket Failure: ${t.message}")
            _isConnected.set(false)
            stopHeartbeat()
            if (!suppressReconnect) {
                scheduleReconnect()
            }
        }
    }

    init {
        startDecodeLoop()
    }
    
    /**
     * 连接直播弹幕服务器
     * 
     * @param url WebSocket 地址 (wss://...)
     * @param token 认证 Token
     * @param roomId 真实房间 ID
     */
    fun connect(url: String, token: String, roomId: Long, uid: Long = 0) {
        // 构建认证包 JSON
        val authJson = JSONObject().apply {
            put("uid", uid) // 使用传入的真实 UID (未登录为 0)
            put("roomid", roomId)
            put("protover", 2) // 降级为 Zlib (2)，避免 Brotli 兼容性问题
            put("platform", "web")
            put("type", 2)
            put("key", token)
        }
        
        this.currentHostUrl = url
        this.currentAuthBody = authJson.toString()
        
        internalConnect()
    }
    
    private fun internalConnect() {
        disconnect() // 先断开旧连接
        
        Log.d(TAG, "🔗 Connecting to $currentHostUrl...")
        val request = Request.Builder()
            .url(currentHostUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            .header("Origin", "https://live.bilibili.com")
            .build()
            
        webSocket = NetworkModule.okHttpClient.newWebSocket(request, listener)
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting...")
        stopHeartbeat()
        reconnectJob?.cancel()
        suppressReconnect = true
        webSocket?.close(1000, "Normal Closure")
        webSocket = null
        _isConnected.set(false)
    }
    
    /**
     * 发送认证包 (Op=7)
     */
    private fun sendAuthPacket() {
        Log.d(TAG, "🔐 Sending Auth Packet...")
        val packet = DanmakuProtocol.Packet(
            version = DanmakuProtocol.PROTO_VER_HEARTBEAT,
            operation = DanmakuProtocol.OP_AUTH,
            body = currentAuthBody.toByteArray()
        )
        sendPacket(packet)
    }
    
    /**
     * 启动心跳任务 (Op=2)
     */
    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && isConnected) {
                // 每 30 秒发送一次心跳
                Log.d(TAG, "💓 Sending Heartbeat...")
                val packet = DanmakuProtocol.Packet(
                    version = DanmakuProtocol.PROTO_VER_HEARTBEAT,
                    operation = DanmakuProtocol.OP_HEARTBEAT,
                    body = "[object Object]".toByteArray()
                )
                sendPacket(packet)
                delay(HEARTBEAT_INTERVAL)
            }
        }
    }
    
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
    }
    
    /**
     * 调度重连 (指数退避)
     */
    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        
        reconnectJob = scope.launch {
            val delayMs = min(1000.0 * 2.0.pow(retryCount), MAX_RETRY_DELAY.toDouble()).toLong()
            Log.d(TAG, "🔄 Reconnecting in ${delayMs}ms (Attempt ${retryCount + 1})...")
            delay(delayMs)
            retryCount++
            internalConnect()
        }
    }
    
    /**
     * 发送数据包
     */
    private fun sendPacket(packet: DanmakuProtocol.Packet) {
        val bytes = DanmakuProtocol.encode(packet)
        webSocket?.send(ByteString.of(*bytes))
    }

    private fun startDecodeLoop() {
        decodeJob?.cancel()
        decodeJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val frame = incomingFrames.receive()
                handleMessage(frame)
            }
        }
    }
    
    /**
     * 处理接收到的二进制消息
     */
    private suspend fun handleMessage(data: ByteArray) {
        try {
            // 解码数据包 (可能包含 recursive decompression)
            val packets = DanmakuProtocol.decode(data)

            packets.forEach { packet ->
                when (packet.operation) {
                    DanmakuProtocol.OP_HEARTBEAT_REPLY -> {
                        // 心跳回应，Body 前4字节为人气值
                        if (packet.body.size >= 4) {
                            val popularity = ByteBuffer.wrap(packet.body).order(java.nio.ByteOrder.BIG_ENDIAN).int
                            Log.d(TAG, "🔥 Popularity: $popularity")
                        }
                    }
                    DanmakuProtocol.OP_AUTH_REPLY -> {
                        val authCode = runCatching {
                            JSONObject(String(packet.body, Charsets.UTF_8)).optInt("code", -1)
                        }.getOrDefault(-1)
                        if (authCode == 0) {
                            Log.d(TAG, "✅ Auth Success")
                        } else {
                            Log.e(TAG, "❌ Auth Failed: code=$authCode")
                            // 认证失败通常不是网络抖动，避免进入无效重连风暴
                            suppressReconnect = true
                            webSocket?.close(4001, "Auth Failed: $authCode")
                        }
                    }
                    DanmakuProtocol.OP_MESSAGE -> {
                        // 所有的业务消息通知 (弹幕、礼物等)
                        // 缓冲区满时丢弃最旧消息，优先保留最新弹幕
                        _messageFlow.tryEmit(packet)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Message handling failed: ${e.message}")
        }
    }

    private fun enqueueMessageFrame(data: ByteArray) {
        if (!incomingFrames.trySend(data).isSuccess) {
            Log.w(TAG, "⚠️ Incoming frame dropped due to backpressure")
        }
    }

    private fun onIncomingMessage(bytes: ByteString) {
        enqueueMessageFrame(bytes.toByteArray())
    }
}
