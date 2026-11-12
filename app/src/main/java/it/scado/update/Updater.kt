package it.scado.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import it.scado.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
)

/**
 * Aggiornamenti dalle GitHub Releases del repository: ogni release ha tag
 * "v<numero build>" e contiene l'APK. Il repository è pubblico, quindi non
 * serve nessun token.
 */
object Updater {
    private const val TIMEOUT_MS = 15_000

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = open("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            when (conn.responseCode) {
                404 -> return@withContext null // nessuna release ancora pubblicata
                !in 200..299 -> throw IOException("Il server ha risposto ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val code = tag.removePrefix("v").toIntOrNull() ?: return@withContext null
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") }
                ?.getString("browser_download_url")
                ?: return@withContext null
            UpdateInfo(
                versionCode = code,
                versionName = "1.0.$code",
                apkUrl = apk,
                notes = json.optString("body", "").trim(),
            )
        } finally {
            conn.disconnect()
        }
    }

    /** L'aggiornamento disponibile, oppure null se l'app è già all'ultima versione. */
    suspend fun availableUpdate(): UpdateInfo? =
        fetchLatest()?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }

    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "scado-${info.versionCode}.apk")
            val conn = open(info.apkUrl)
            try {
                if (conn.responseCode !in 200..299) throw IOException("Download non riuscito (${conn.responseCode})")
                val total = conn.contentLengthLong
                var done = 0L
                var lastPercent = -1
                conn.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            done += n
                            if (total > 0) {
                                val percent = (done * 100 / total).toInt()
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent / 100f)
                                }
                            }
                        }
                    }
                }
                file
            } catch (e: Exception) {
                file.delete()
                throw e
            } finally {
                conn.disconnect()
            }
        }

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /**
     * Apre l'installer di Android. La prima volta Android chiede di permettere
     * a Scado di installare app: in quel caso apro direttamente quella schermata.
     */
    fun install(context: Context, file: File) {
        if (!canInstall(context)) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Scado-Android/${BuildConfig.VERSION_NAME}")
        }
}
