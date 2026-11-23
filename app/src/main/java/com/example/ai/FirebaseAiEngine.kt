package com.example.ai

import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.*
import kotlinx.coroutines.withTimeout

object FirebaseAiEngine {
    private const val TAG = "FirebaseAiEngine"

    private val generativeModel by lazy {
        Firebase.ai.generativeModel("gemini-3.5-flash-lite")
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(false)
    }

    suspend fun generateGenerativeReply(
        context: Context?,
        query: String,
        attachmentType: String,
        attachmentUri: String,
        recentMessages: List<com.example.data.ai.AiMessageEntity>
    ): String? {
        if (context == null || !isNetworkAvailable(context)) return null
        return try {
            withTimeout(35_000L) {
                val promptText = buildString {
                    append("أنت المساعد الإسلامي الذكي التوليدي المدرب باحترافية عالية. تقتصر مهامك حصراً على العلوم والمسائل الدينية، الفتاوى، الرؤى والأحلام، الخطب والخواطر، والاستدلال بالقرآن والسنة.\n")
                    if (attachmentType == "image") {
                        append("المستخدم أرفق صورة. قم بتحليل محتواها بدقة من منظور إسلامي شرعي.\n")
                    } else if (attachmentType == "audio") {
                        append("المستخدم أرفق تسجيلاً صوتياً. قم بالتحليل والتوجيه الشرعي بناءً عليه.\n")
                    }
                    if (recentMessages.isNotEmpty()) {
                        append("سياق المحادثة السابق:\n")
                        recentMessages.takeLast(4).forEach { msg ->
                            val roleName = if (msg.role == "user") "المستخدم" else "المساعد"
                            append("$roleName: ${msg.content}\n")
                        }
                    }
                    append("سؤال المستخدم: $query\n")
                    append("أجب بإجابة إسلامية علمية عميقة، موثّقة بالقرآن والسنة بأسلوب عربي فصيح.")
                }

                val response = if (attachmentType == "image" && attachmentUri.isNotBlank()) {
                    val uri = Uri.parse(attachmentUri)
                    val bitmap = context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                    if (bitmap != null) {
                        generativeModel.generateContent(
                            content {
                                image(bitmap)
                                text(promptText)
                            }
                        )
                    } else {
                        generativeModel.generateContent(promptText)
                    }
                } else {
                    generativeModel.generateContent(promptText)
                }

                response.text?.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic cloud inference failed, falling back to local engine", e)
            null
        }
    }
}
