package com.example.humanreactor.vercel_port

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * GeminiClient - 一个简单的客户端，用于与Gemini API通信。
 */
class GeminiClient(private val baseUrl: String) {

    // API接口定义
    private interface GeminiApiService {
        @POST("api/gemini_flexible.js")
        suspend fun generateContent(@Body request: GeminiRequest): Response<GeminiResponse>
    }

    // 请求模型
    private data class GeminiRequest(val prompt: String)

    // 响应模型
    private data class GeminiResponse(
        val candidates: List<Candidate>? = null,
        val error: ErrorResponse? = null
    )

    private data class Candidate(
        val content: Content? = null,
        val finishReason: String? = null
    )

    private data class Content(
        val parts: List<Part>? = null,
        val role: String? = null
    )

    private data class Part(
        val text: String? = null
    )

    private data class ErrorResponse(
        val message: String? = null,
        @SerializedName("code") val errorCode: Int? = null
    )

    // 创建OkHttpClient，添加日志拦截器，设置超时时间
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 创建Retrofit实例
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // 创建API服务
    private val apiService = retrofit.create(GeminiApiService::class.java)

    /**
     * 生成内容 - 向Gemini API发送提示并获取响应。
     *
     * @param prompt 要发送到Gemini的提示文本
     * @return 生成的文本响应
     * @throws Exception 如果API调用失败或返回错误
     */
    suspend fun generateContent(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = GeminiRequest(prompt)
                Log.d("GeminiClient", "Request URL: ${retrofit.baseUrl()}api/gemini")

                val response = apiService.generateContent(request)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body?.error != null) {
                        throw Exception("API错误: ${body.error.message}")
                    }

                    val generatedText = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                    if (generatedText != null) {
                        generatedText
                    } else {
                        throw Exception("无法获取生成的文本")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    throw Exception("API调用失败 (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                throw e
            }
        }
    }

    companion object {
        private const val TAG = "GeminiClient"
    }
}