package com.example.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

/**
 * تهيئة كسولة وآمنة لـ Firebase App Check.
 *
 * لا يتم استدعاء أي فئة من حزم Firebase عند إقلاع التطبيق لتفادي أي تعليق أو انهيار أثناء التشغيل بدون إنترنت؛
 * بل يُكتفى بحفظ رمز الاختبار من نية التشغيل في MainActivity، ثم تُفعَّل التهيئة الفعلية عند أول طلب سحابي فقط.
 */
object AppCheckInitializer {
    private const val TAG = "AppCheckInit"
    private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
    private const val PENDING_PREFS_NAME = "appcheck_pending"
    private const val PENDING_TOKEN_KEY = "pending_debug_token"
    private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
    private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
    private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

    @Volatile
    private var capturedDebugToken: String? = null

    @Volatile
    private var configured: Boolean = false

    /**
     * يلتقط رمز التصحيح من Intent التشغيل فقط دون أي استدعاء لـ Firebase SDK عند الإقلاع.
     */
    fun captureLaunchIntent(context: Context, intent: Intent?) {
        runCatching {
            val token = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
            if (!token.isNullOrEmpty()) {
                capturedDebugToken = token
                context.applicationContext
                    .getSharedPreferences(PENDING_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PENDING_TOKEN_KEY, token)
                    .commit()
            }
        }
    }

    /**
     * يهيّئ FirebaseApp و App Check بشكل كسول عند الحاجة السحابية الفعلية فقط.
     */
    fun ensureConfigured(context: Context) {
        if (configured) return
        synchronized(this) {
            if (configured) return
            val appContext = context.applicationContext
            try {
                if (FirebaseApp.getApps(appContext).isEmpty()) {
                    FirebaseApp.initializeApp(appContext)
                }

                if (BuildConfig.DEBUG) {
                    val debugToken = capturedDebugToken
                        ?: appContext.getSharedPreferences(PENDING_PREFS_NAME, Context.MODE_PRIVATE)
                            .getString(PENDING_TOKEN_KEY, null)

                    if (!debugToken.isNullOrBlank()) {
                        val persistenceKey = FirebaseApp.getInstance().persistenceKey
                        val prefs = appContext.getSharedPreferences(
                            "$DEBUG_STORE_PREFIX$persistenceKey",
                            Context.MODE_PRIVATE
                        )
                        prefs.edit()
                            .putString(DEBUG_SECRET_KEY, debugToken)
                            .commit()
                        Log.d(TAG, "App Check debug secret configured lazily.")
                    }

                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    Log.d(TAG, "DebugAppCheckProviderFactory installed lazily.")
                } else {
                    val resId = appContext.resources.getIdentifier(
                        RECAPTCHA_SITE_KEY_RES,
                        "string",
                        appContext.packageName
                    )
                    if (resId != 0) {
                        val siteKey = appContext.getString(resId)
                        if (siteKey.isNotBlank()) {
                            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                                RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                            )
                            Log.d(TAG, "RecaptchaAppCheckProviderFactory installed lazily.")
                        } else {
                            Log.e(TAG, "recaptcha_site_key resource string is blank.")
                        }
                    } else {
                        Log.e(TAG, "recaptcha_site_key string resource not found in package.")
                    }
                }
                configured = true
            } catch (e: Exception) {
                Log.e(TAG, "Lazy Firebase App Check initialization failed", e)
            }
        }
    }

    fun configureAppCheck(context: Context, intent: Intent?) {
        captureLaunchIntent(context, intent)
        ensureConfigured(context)
    }
}
