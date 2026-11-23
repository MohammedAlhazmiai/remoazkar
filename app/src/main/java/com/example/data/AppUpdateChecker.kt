package com.example.data

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Checks the public GitHub Releases feed for a newer signed APK. */
class AppUpdateChecker(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    fun shouldAutomaticallyCheck(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val lastCheck = preferences.getLong(KEY_LAST_CHECK, 0L)
        return lastCheck == 0L || nowMillis - lastCheck >= AUTOMATIC_CHECK_INTERVAL_MILLIS
    }

    fun wasAutomaticallyPromptedFor(version: String): Boolean =
        preferences.getString(KEY_LAST_PROMPTED_VERSION, null) == version

    fun markAutomaticallyPrompted(version: String) {
        preferences.edit().putString(KEY_LAST_PROMPTED_VERSION, version).apply()
    }

    suspend fun check(force: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!force && !shouldAutomaticallyCheck()) return@withContext UpdateCheckResult.Skipped

        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API_URL)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "ZadAlMuslimAndroid/${BuildConfig.VERSION_NAME}")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext UpdateCheckResult.NoPublishedRelease
                if (!response.isSuccessful) return@withContext UpdateCheckResult.Failed

                val json = response.body?.string()?.let(::JSONObject)
                    ?: return@withContext UpdateCheckResult.Failed
                val version = json.optString("tag_name")
                    .removePrefix("v")
                    .takeIf(String::isNotBlank)
                    ?: return@withContext UpdateCheckResult.Failed

                val comparison = compareDottedVersions(version, BuildConfig.VERSION_NAME)
                    ?: return@withContext UpdateCheckResult.Failed
                if (comparison <= 0) return@withContext UpdateCheckResult.UpToDate

                val apkUrl = findApkDownloadUrl(json)
                    ?: return@withContext UpdateCheckResult.Failed
                UpdateCheckResult.Available(
                    AppUpdateInfo(
                        version = version,
                        releaseNotes = json.optString("body").trim().take(MAX_RELEASE_NOTES_CHARS),
                        downloadUrl = apkUrl,
                        releaseUrl = json.optString("html_url").ifBlank { RELEASES_PAGE_URL }
                    )
                )
            }
        } catch (_: Exception) {
            UpdateCheckResult.Failed
        } finally {
            preferences.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
        }
    }

    private fun findApkDownloadUrl(release: JSONObject): String? {
        val assets = release.optJSONArray("assets") ?: return null
        val apkAssets = (0 until assets.length()).mapNotNull { index ->
            assets.optJSONObject(index)?.let { asset ->
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://")) {
                    name to url
                } else {
                    null
                }
            }
        }
        return apkAssets.firstOrNull { it.first.startsWith("zad-almuslim-", ignoreCase = true) }?.second
            ?: apkAssets.firstOrNull()?.second
    }

    companion object {
        private const val PREFERENCES_NAME = "github_app_updates"
        private const val KEY_LAST_CHECK = "last_check_millis"
        private const val KEY_LAST_PROMPTED_VERSION = "last_prompted_version"
        private const val AUTOMATIC_CHECK_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
        private const val MAX_RELEASE_NOTES_CHARS = 4_000
        private const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/MohammedAlhazmiai/remoazkar/releases/latest"
        private const val RELEASES_PAGE_URL =
            "https://github.com/MohammedAlhazmiai/remoazkar/releases"
    }
}

data class AppUpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releaseUrl: String
)

sealed class UpdateCheckResult {
    data class Available(val update: AppUpdateInfo) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    object NoPublishedRelease : UpdateCheckResult()
    object Skipped : UpdateCheckResult()
    object Failed : UpdateCheckResult()
}

/** Returns null for malformed version names; missing numeric components compare as zero. */
internal fun compareDottedVersions(first: String, second: String): Int? {
    fun parts(value: String): List<Long>? {
        val normalized = value.trim().removePrefix("v")
        if (!normalized.matches(Regex("\\d+(?:\\.\\d+)*"))) return null
        return normalized.split('.').map { it.toLongOrNull() ?: return null }
    }

    val left = parts(first) ?: return null
    val right = parts(second) ?: return null
    for (index in 0 until maxOf(left.size, right.size)) {
        val comparison = (left.getOrElse(index) { 0L }).compareTo(right.getOrElse(index) { 0L })
        if (comparison != 0) return comparison
    }
    return 0
}
