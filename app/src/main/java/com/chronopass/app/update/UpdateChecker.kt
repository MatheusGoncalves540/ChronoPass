package com.chronopass.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val REPO = "MatheusGoncalves540/ChronoPass"

object UpdateChecker {

    data class UpdateInfo(val tagName: String, val version: String, val downloadUrl: String)

    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? =
            withContext(Dispatchers.IO) {
                runCatching {
                            val conn =
                                    URL("https://api.github.com/repos/$REPO/releases/latest")
                                            .openConnection() as
                                            HttpURLConnection
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
                        }
                        .getOrNull()
            }

    fun isNewer(remote: String, local: String): Boolean {
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

    // --- Download para o storage privado do app (não toca no app "Downloads") ---

    private fun apkFile(context: Context, info: UpdateInfo): File =
            File(context.filesDir, "update/chronopass-${info.version}.apk")

    /** APK completo já baixado, se existir. */
    fun downloadedApk(context: Context, info: UpdateInfo): File? =
            apkFile(context, info).takeIf { it.isFile }

    /**
     * Baixa o APK direto para o storage privado do app, com progresso visível. Escreve num `.part`
     * e renomeia só ao terminar, para nunca instalar um download pela metade. Retorna null em caso
     * de falha.
     */
    suspend fun downloadApk(
            context: Context,
            info: UpdateInfo,
            onProgress: (downloaded: Long, total: Long) -> Unit
    ): File? =
            withContext(Dispatchers.IO) {
                runCatching {
                            val dir = File(context.filesDir, "update")
                            dir.mkdirs()
                            // Limpa restos de tentativas anteriores (downloads cancelados ou
                            // versões antigas).
                            dir.listFiles()?.forEach { it.delete() }
                            val part = File(dir, "chronopass-${info.version}.apk.part")
                            val conn = URL(info.downloadUrl).openConnection() as HttpURLConnection
                            conn.setRequestProperty("User-Agent", "ChronoPass-App")
                            conn.connect()
                            val total = conn.contentLength.toLong().takeIf { it > 0 } ?: 0L
                            var downloaded = 0L
                            var lastPct = -1
                            conn.inputStream.use { input ->
                                part.outputStream().use { output ->
                                    val buf = ByteArray(64 * 1024)
                                    while (true) {
                                        val n = input.read(buf)
                                        if (n < 0) break
                                        output.write(buf, 0, n)
                                        downloaded += n
                                        // Throttling: no máximo 1 atualização de progresso por 1%.
                                        val pct =
                                                if (total > 0) (downloaded * 100 / total).toInt()
                                                else -1
                                        if (pct != lastPct) {
                                            lastPct = pct
                                            onProgress(downloaded, total)
                                        }
                                    }
                                }
                            }
                            val target = apkFile(context, info)
                            part.renameTo(target)
                            target
                        }
                        .getOrNull()
            }

    /** Dispara o instalador do Android via FileProvider. */
    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        context.startActivity(intent)
    }
}

private enum class UpdatePhase {
    Ask,
    Downloading,
    NeedPermission
}

@Composable
fun UpdateAvailableDialog(
        info: UpdateChecker.UpdateInfo,
        onDismiss: () -> Unit,
        onInstallLaunched: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(UpdatePhase.Ask) }
    var downloaded by remember { mutableStateOf(0L) }
    var total by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    // Ao voltar das configurações de "fontes desconhecidas", instala direto:
    // o APK já está baixado, não precisa pedir nada de novo.
    val settingsLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (!UpdateChecker.canInstallUnknownApps(context))
                        return@rememberLauncherForActivityResult
                UpdateChecker.downloadedApk(context, info)?.let { file ->
                    onInstallLaunched()
                    UpdateChecker.installApk(context, file)
                }
            }

    fun startDownload() {
        phase = UpdatePhase.Downloading
        error = null
        scope.launch {
            val file =
                    UpdateChecker.downloadApk(context, info) { d, t ->
                        downloaded = d
                        total = t
                    }
            when {
                file == null -> {
                    phase = UpdatePhase.Ask
                    error =
                            "Não foi possível baixar a atualização. Confira sua conexão e tente de novo."
                }
                UpdateChecker.canInstallUnknownApps(context) -> {
                    onInstallLaunched()
                    UpdateChecker.installApk(context, file)
                }
                else -> phase = UpdatePhase.NeedPermission
            }
        }
    }

    when (phase) {
        UpdatePhase.Ask ->
                AlertDialog(
                        onDismissRequest = onDismiss,
                        title = { Text("Nova versão disponível") },
                        text = {
                            Text(
                                    buildString {
                                        append(
                                                "A versão ${info.version} do ChronoPass já está disponível. Deseja atualizar agora?"
                                        )
                                        if (!UpdateChecker.canInstallUnknownApps(context)) {
                                            append(
                                                    "\n\nAo terminar o download, o Android vai pedir uma permissão única para instalar apps de fora da Play Store."
                                            )
                                        }
                                        error?.let { append("\n\n$it") }
                                    }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { startDownload() }) { Text("Atualizar") }
                        },
                        dismissButton = { TextButton(onClick = onDismiss) { Text("Agora não") } }
                )
        UpdatePhase.Downloading ->
                AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        dismissButton = {},
                        title = { Text("Baixando atualização") },
                        text = {
                            Column {
                                if (total > 0) {
                                    LinearProgressIndicator(
                                            progress = { downloaded.toFloat() / total },
                                            modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                            "${downloaded * 100 / total}% · ChronoPass ${info.version}"
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(8.dp))
                                    Text("Preparando download…")
                                }
                            }
                        }
                )
        UpdatePhase.NeedPermission ->
                AlertDialog(
                        onDismissRequest = onDismiss,
                        title = { Text("Permissão para instalar") },
                        text = {
                            Text(
                                    "O download terminou. Para instalar o ChronoPass, o Android pede que você " +
                                            "permita instalar apps de fora da Play Store. Toque em \"Abrir configurações\", " +
                                            "ative a permissão para o ChronoPass e volte — a instalação começa na hora."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                    onClick = {
                                        settingsLauncher.launch(
                                                UpdateChecker.unknownAppsSettingsIntent(context)
                                        )
                                    }
                            ) { Text("Abrir configurações") }
                        },
                        dismissButton = { TextButton(onClick = onDismiss) { Text("Agora não") } }
                )
    }
}
