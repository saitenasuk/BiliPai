
package com.android.purebilibili.feature.login

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import com.android.purebilibili.core.util.Logger
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.CaptchaData
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class LoginState {
    object Loading : LoginState()
    data class QrCode(val bitmap: Bitmap) : LoginState()
    data class Scanned(val bitmap: Bitmap) : LoginState()
    object Success : LoginState()
    data class Error(val msg: String) : LoginState()
    
    // 🔥 手机号登录状态
    object PhoneIdle : LoginState()  // 等待输入手机号
    data class CaptchaReady(val captchaData: CaptchaData) : LoginState()  // 验证码准备就绪
    data class SmsSent(val captchaKey: String) : LoginState()  // 短信已发送
    object PasswordMode : LoginState()  // 密码登录模式
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<LoginState>(LoginState.Loading)
    val state = _state.asStateFlow()

    private var qrcodeKey: String = ""
    private var isPolling = true

    fun loadQrCode() {
        isPolling = true
        viewModelScope.launch {
            try {
                _state.value = LoginState.Loading
                Logger.d("LoginDebug", "1. 开始获取二维码...")

                val resp = NetworkModule.passportApi.generateQrCode()

                // 🔥 核心修复：处理可空类型
                val data = resp.data ?: throw Exception("服务器返回数据为空")
                val url = data.url ?: throw Exception("二维码 URL 为空")

                // 👇 这里使用 ?: 抛出异常，解决了 Type mismatch 问题
                qrcodeKey = data.qrcode_key ?: throw Exception("二维码 Key 为空")

                Logger.d("LoginDebug", "2. 二维码获取成功 Key: $qrcodeKey")
                val bitmap = generateQrBitmap(url)
                currentBitmap = bitmap // 🔥 保存以便在 Scanned 状态使用
                _state.value = LoginState.QrCode(bitmap)

                startPolling()
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("LoginDebug", "获取二维码失败", e)
                _state.value = LoginState.Error(e.message ?: "网络错误")
            }
        }
    }

    private var currentBitmap: Bitmap? = null // 🔥 保存当前二维码用于 Scanned 状态

    private fun startPolling() {
        viewModelScope.launch {
            Logger.d("LoginDebug", "3. 开始轮询...")
            while (isPolling) {
                delay(2000) // 🔥 缩短轮询间隔，更快响应
                try {
                    val response = NetworkModule.passportApi.pollQrCode(qrcodeKey)
                    val body = response.body()

                    // 🔥 核心修复：处理可空类型，默认为 -1 防止空指针
                    val code = body?.data?.code ?: -1

                    Logger.d("LoginDebug", "轮询状态: Code=$code")

                    when (code) {
                        0 -> {
                            // 🔥 登录成功
                            Logger.d("LoginDebug", ">>> 登录成功！开始解析 Cookie <<<")

                            val cookies = response.headers().values("Set-Cookie")
                            var sessData = ""
                            var biliJct = "" // 🔥 CSRF token

                            for (line in cookies) {
                                if (line.contains("SESSDATA")) {
                                    val parts = line.split(";")
                                    for (part in parts) {
                                        val trimPart = part.trim()
                                        if (trimPart.startsWith("SESSDATA=")) {
                                            sessData = trimPart.substringAfter("SESSDATA=")
                                            break
                                        }
                                    }
                                }
                                // 🔥 提取 bili_jct (CSRF Token)
                                if (line.contains("bili_jct")) {
                                    val parts = line.split(";")
                                    for (part in parts) {
                                        val trimPart = part.trim()
                                        if (trimPart.startsWith("bili_jct=")) {
                                            biliJct = trimPart.substringAfter("bili_jct=")
                                            break
                                        }
                                    }
                                }
                            }

                            if (sessData.isNotEmpty()) {
                                Logger.d("LoginDebug", "✅ 成功提取 SESSDATA: $sessData")
                                Logger.d("LoginDebug", "✅ 成功提取 bili_jct: $biliJct")

                                // 保存并更新缓存
                                TokenManager.saveCookies(getApplication(), sessData)
                                // 🔥 保存 CSRF Token (持久化)
                                if (biliJct.isNotEmpty()) {
                                    TokenManager.saveCsrf(getApplication(), biliJct)
                                }

                                isPolling = false
                                withContext(Dispatchers.Main) {
                                    _state.value = LoginState.Success
                                }
                            } else {
                                _state.value = LoginState.Error("Cookie 解析失败")
                            }
                        }
                        86090 -> {
                            // 🔥 新增: 已扫描待确认
                            Logger.d("LoginDebug", "📱 二维码已扫描，等待确认...")
                            currentBitmap?.let { bitmap ->
                                withContext(Dispatchers.Main) {
                                    _state.value = LoginState.Scanned(bitmap)
                                }
                            }
                        }
                        86038 -> {
                            // 二维码已过期
                            _state.value = LoginState.Error("二维码已过期，请刷新")
                            isPolling = false
                        }
                        86101 -> {
                            // 未扫描，继续轮询
                            Logger.d("LoginDebug", "等待扫描...")
                        }
                    }
                } catch (e: Exception) {
                    com.android.purebilibili.core.util.Logger.e("LoginDebug", "轮询异常", e)
                }
            }
        }
    }

    fun stopPolling() { isPolling = false }

    private fun generateQrBitmap(content: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val w = bitMatrix.width
        val h = bitMatrix.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        for (x in 0 until w) {
            for (y in 0 until h) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
    
    // ========== 🔥 手机号登录方法 ==========
    
    // 当前验证码数据 (极验验证成功后暂存)
    private var currentCaptchaData: CaptchaData? = null
    private var currentValidate: String = ""
    private var currentSeccode: String = ""
    private var currentChallenge: String = ""
    private var currentCaptchaKey: String = ""  // 发送短信后返回的 key
    private var currentPhone: Long = 0
    
    /**
     * 获取极验验证参数
     */
    fun getCaptcha() {
        viewModelScope.launch {
            try {
                _state.value = LoginState.Loading
                Logger.d("LoginDebug", "获取极验验证参数...")
                
                val response = NetworkModule.passportApi.getCaptcha()
                if (response.code == 0 && response.data != null) {
                    currentCaptchaData = response.data
                    Logger.d("LoginDebug", "极验参数获取成功: gt=${response.data.geetest?.gt}")
                    _state.value = LoginState.CaptchaReady(response.data)
                } else {
                    _state.value = LoginState.Error("获取验证参数失败: ${response.message}")
                }
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("LoginDebug", "获取验证参数异常", e)
                _state.value = LoginState.Error("网络错误: ${e.message}")
            }
        }
    }
    
    /**
     * 保存极验验证结果
     */
    fun saveCaptchaResult(validate: String, seccode: String, challenge: String) {
        currentValidate = validate
        currentSeccode = seccode
        currentChallenge = challenge
        Logger.d("LoginDebug", "极验验证成功: validate=$validate")
    }
    
    /**
     * 发送短信验证码
     */
    fun sendSmsCode(phone: Long) {
        viewModelScope.launch {
            try {
                _state.value = LoginState.Loading
                currentPhone = phone
                
                val captchaData = currentCaptchaData ?: run {
                    _state.value = LoginState.Error("验证参数丢失，请重试")
                    return@launch
                }
                
                Logger.d("LoginDebug", "发送短信验证码到: $phone")
                
                val response = NetworkModule.passportApi.sendSmsCode(
                    tel = phone,
                    token = captchaData.token,
                    challenge = currentChallenge,
                    validate = currentValidate,
                    seccode = currentSeccode
                )
                
                if (response.code == 0 && response.data != null) {
                    currentCaptchaKey = response.data.captchaKey
                    Logger.d("LoginDebug", "短信发送成功: captchaKey=${currentCaptchaKey}")
                    _state.value = LoginState.SmsSent(currentCaptchaKey)
                } else {
                    _state.value = LoginState.Error("短信发送失败: ${response.message}")
                }
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("LoginDebug", "发送短信异常", e)
                _state.value = LoginState.Error("网络错误: ${e.message}")
            }
        }
    }
    
    /**
     * 短信验证码登录
     */
    fun loginBySms(code: Int) {
        viewModelScope.launch {
            try {
                _state.value = LoginState.Loading
                Logger.d("LoginDebug", "短信验证码登录: phone=$currentPhone, code=$code")
                
                val response = NetworkModule.passportApi.loginBySms(
                    tel = currentPhone,
                    code = code,
                    captchaKey = currentCaptchaKey
                )
                
                val body = response.body()
                if (body?.code == 0) {
                    // 解析 Cookie
                    val cookies = response.headers().values("Set-Cookie")
                    handleLoginCookies(cookies)
                } else {
                    _state.value = LoginState.Error("登录失败: ${body?.message ?: "未知错误"}")
                }
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("LoginDebug", "短信登录异常", e)
                _state.value = LoginState.Error("网络错误: ${e.message}")
            }
        }
    }
    
    /**
     * 密码登录
     */
    fun loginByPassword(phone: Long, password: String) {
        viewModelScope.launch {
            try {
                _state.value = LoginState.Loading
                Logger.d("LoginDebug", "密码登录: phone=$phone")
                
                // 1. 获取 RSA 公钥
                val keyResponse = NetworkModule.passportApi.getWebKey()
                if (keyResponse.code != 0 || keyResponse.data == null) {
                    _state.value = LoginState.Error("获取密钥失败: ${keyResponse.message}")
                    return@launch
                }
                
                val hash = keyResponse.data.hash
                val key = keyResponse.data.key
                
                // 2. RSA 加密密码
                val encryptedPassword = RsaEncryption.encryptPassword(password, key, hash)
                if (encryptedPassword == null) {
                    _state.value = LoginState.Error("密码加密失败")
                    return@launch
                }
                
                // 3. 需要验证码
                val captchaData = currentCaptchaData ?: run {
                    _state.value = LoginState.Error("验证参数丢失，请重试")
                    return@launch
                }
                
                // 4. 登录
                val response = NetworkModule.passportApi.loginByPassword(
                    username = phone,
                    password = encryptedPassword,
                    token = captchaData.token,
                    challenge = currentChallenge,
                    validate = currentValidate,
                    seccode = currentSeccode
                )
                
                val body = response.body()
                if (body?.code == 0) {
                    val cookies = response.headers().values("Set-Cookie")
                    handleLoginCookies(cookies)
                } else {
                    _state.value = LoginState.Error("登录失败: ${body?.message ?: "未知错误"}")
                }
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("LoginDebug", "密码登录异常", e)
                _state.value = LoginState.Error("网络错误: ${e.message}")
            }
        }
    }
    
    /**
     * 处理登录返回的 Cookie
     */
    private suspend fun handleLoginCookies(cookies: List<String>) {
        var sessData = ""
        var biliJct = ""
        
        for (line in cookies) {
            if (line.contains("SESSDATA")) {
                sessData = line.split(";").firstOrNull { it.trim().startsWith("SESSDATA=") }
                    ?.substringAfter("SESSDATA=") ?: ""
            }
            if (line.contains("bili_jct")) {
                biliJct = line.split(";").firstOrNull { it.trim().startsWith("bili_jct=") }
                    ?.substringAfter("bili_jct=") ?: ""
            }
        }
        
        if (sessData.isNotEmpty()) {
            Logger.d("LoginDebug", "✅ 登录成功: SESSDATA=$sessData")
            TokenManager.saveCookies(getApplication(), sessData)
            if (biliJct.isNotEmpty()) {
                TokenManager.saveCsrf(getApplication(), biliJct)
            }
            withContext(Dispatchers.Main) {
                _state.value = LoginState.Success
            }
        } else {
            _state.value = LoginState.Error("Cookie 解析失败")
        }
    }
    
    /**
     * 重置手机登录状态
     */
    fun resetPhoneLogin() {
        currentCaptchaData = null
        currentValidate = ""
        currentSeccode = ""
        currentChallenge = ""
        currentCaptchaKey = ""
        currentPhone = 0
        _state.value = LoginState.PhoneIdle
    }
}
