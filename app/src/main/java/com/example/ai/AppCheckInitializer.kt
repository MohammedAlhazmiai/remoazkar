package com.example.ai

import android.content.Context
import android.content.Intent

/**
 * محقّق التهيئة الخفيف لنسخة Lite.
 */
object AppCheckInitializer {
    fun captureLaunchIntent(context: Context, intent: Intent?) {
        // no-op in Lite offline version
    }

    fun ensureConfigured(context: Context) {
        // no-op in Lite offline version
    }

    fun configureAppCheck(context: Context, intent: Intent?) {
        // no-op in Lite offline version
    }
}
