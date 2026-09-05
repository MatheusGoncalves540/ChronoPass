package com.chronopass.app.sync

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Fase 3 (SUMUS-INTEGRACAO.md §10.3): cliente HTTP do Summus.
 *
 * Só a ponte Android: montar o envelope de device/store/versão, ler a foto e fazer o POST. A
 * montagem dos lotes em si é pura (SummusPayloads) para a Fase 5 testar em JVM puro.
 *
 * Enviar/enfileirar/drenar NÃO acontece aqui — a Fase 4 decide quando chamar [post]. Conexão sempre
 * fechada (try/finally); api-key só no header Authorization, nunca no corpo.
 */
object SummusClient {

    /** Resultado tipado para a Fase 4 decidir retry/ack (2xx = sucesso). */
    sealed interface PostResult {
        /** HTTP 2xx — pode remover o lote da fila. */
        data class Ack(val httpCode: Int) : PostResult

        /** Resposta HTTP != 2xx — erro de servidor/contrato; retry/backoff na Fase 4. */
        data class HttpError(val httpCode: Int, val bodySnippet: String) : PostResult

        /**
         * Falhou antes de receber status (rede, timeout, DNS, exceção) — retry/backoff na Fase 4.
         */
        data class TransportError(val message: String) : PostResult
    }

    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 20_000

    // Contrato real do backoffice: a URL configurada é a BASE; o POST vai em paths fixos.
    const val SYNC_PATH = "/api/integrations/chronopass/sync"
    const val PHOTOS_PATH = "/api/integrations/chronopass/photos"

    /** Concatena a base (tolerante a barra final) com o path fixo do contrato. */
    fun endpointUrl(base: String, path: String): String = base.trimEnd('/') + path

    /** Lote 1 (metadados: employees + punches) -> POST {base}/api/integrations/chronopass/sync. */
    suspend fun postSync(baseUrl: String, apiKey: String, body: JSONObject): PostResult =
            post(endpointUrl(baseUrl, SYNC_PATH), apiKey, body)

    /** Lote 2 (fotos) -> POST {base}/api/integrations/chronopass/photos. */
    suspend fun postPhotos(baseUrl: String, apiKey: String, body: JSONObject): PostResult =
            post(endpointUrl(baseUrl, PHOTOS_PATH), apiKey, body)

    /** POST de um lote pronto. Suspend: roda em Dispatchers.IO. */
    private suspend fun post(
            url: String,
            apiKey: String,
            body: JSONObject,
            connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
            readTimeoutMs: Int = READ_TIMEOUT_MS,
    ): PostResult =
            withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = connectTimeoutMs
                    conn.readTimeout = readTimeoutMs
                    conn.setRequestProperty("Authorization", "Bearer $apiKey")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    val bytes = body.toString().toByteArray(Charsets.UTF_8)
                    conn.setFixedLengthStreamingMode(bytes.size)
                    conn.outputStream.use { it.write(bytes) }
                    val code = conn.responseCode
                    if (code in 200..299) {
                        PostResult.Ack(code)
                    } else {
                        // Corpo de erro costuma ser pequeno; trunca para diagnóstico sem estourar
                        // memória.
                        val snippet =
                                try {
                                    conn.errorStream
                                            ?.bufferedReader()
                                            ?.use { it.readText() }
                                            ?.take(500)
                                            ?: ""
                                } catch (_: Exception) {
                                    ""
                                }
                        PostResult.HttpError(code, snippet)
                    }
                } catch (e: Exception) {
                    PostResult.TransportError(e.message ?: e.javaClass.simpleName)
                } finally {
                    conn?.disconnect()
                }
            }

    /** Identidade do aparelho (§3): "chronopass-<ANDROID_ID>" + Build.MODEL. */
    fun deviceId(context: Context): String {
        val androidId =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        return "chronopass-$androidId"
    }

    fun deviceModel(): String = Build.MODEL

    // Mesmo padrão do UpdateChecker/MainActivity (getPackageInfo sem flags).
    fun appVersion(context: Context): String =
            runCatching {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    }
                    .getOrNull()
                    ?: "0.0.0"

    /**
     * Lê a foto privada do ponto e devolve o payload do Lote 2.
     *
     * key = uid do punch; fileName = nome base do arquivo (ex.: "2026-08-25_08-03-12.jpg");
     * contentType fixo image/jpeg (CameraX captura JPEG; PhotoCompressor re-encoda para JPEG).
     * Ponte enxuta: a Fase 4 decide quando chamar (foto ainda na fila, etc.).
     */
    fun photoPayload(punchUid: String, photoPath: String): SummusPayloads.PhotoPayload? =
            runCatching {
                        val f = File(photoPath)
                        val data = f.readBytes()
                        SummusPayloads.PhotoPayload(
                                key = punchUid,
                                fileName = SummusPayloads.photoFileName(f.name),
                                dataBase64 =
                                        Base64.encodeToString(
                                                data,
                                                Base64.NO_WRAP or Base64.NO_PADDING
                                        ),
                        )
                    }
                    .getOrNull()
}
