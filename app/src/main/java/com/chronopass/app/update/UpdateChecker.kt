package com.chronopass.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val REPO = "MatheusGoncalves540/ChronoPass"

object UpdateChecker {
    data class UpdateInfo(val tagName: String, val version: String, val downloadUrl: String)

    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://api.github.com/repos/$REPO/releases/latest")
                .openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "ChronoPass-App")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.getString("tag_name")
            val version = tag.removePrefix("v")
            val assets = json.getJSONArray("assets")
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            if (apkUrl != null && isNewer(version, currentVersionName)) {
                UpdateInfo(tag, version, apkUrl)
            } else null
        }.getOrNull()
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    fun canInstallUnknownApps(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun unknownAppsSettingsIntent(context: Context): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))

    suspend fun downloadAndInstall(context: Context, info: UpdateInfo) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("ChronoPass ${info.version}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "chronopass-${info.version}.apk")
        val id = dm.enqueue(request)

        var downloading = true
        while (downloading) {
            withContext(Dispatchers.IO) {
                dm.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                        }
                    } else downloading = false
                }
            }
            if (downloading) delay(700)
        }

        val uri = dm.getUriForDownloadedFile(id) ?: return
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}

@Composable
fun UpdateAvailableDialog(info: UpdateChecker.UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (UpdateChecker.canInstallUnknownApps(context)) {
            scope.launch { UpdateChecker.downloadAndInstall(context, info) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova versão disponível") },
        text = { Text("A versão ${info.version} do ChronoPass já está disponível. Deseja atualizar agora?") },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                if (UpdateChecker.canInstallUnknownApps(context)) {
                    scope.launch { UpdateChecker.downloadAndInstall(context, info) }
                } else {
                    unknownSourcesLauncher.launch(UpdateChecker.unknownAppsSettingsIntent(context))
                }
            }) { Text("Atualizar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Agora não") } }
    )
}
