package com.chronopass.app.sync

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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

    /**
     * Resultado do GET. Diferente do [PostResult.Ack], que só olha o status: aqui o corpo É a
     * resposta. Erro de HTTP e erro de rede caem no mesmo [Erro] de propósito — o chamador trata os
     * dois igual (reporta e não avança o cursor).
     */
    sealed interface GetResult {
        data class Ok(val body: String) : GetResult
        data class Erro(val motivo: String) : GetResult
    }

    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 20_000

    /** Teto de bytes de uma foto baixada (o servidor recusa upload acima de 5 MiB). */
    const val MAX_PHOTO_BYTES = 5 * 1024 * 1024

    // Contrato real do backoffice: a URL configurada é a BASE; o POST vai em paths fixos.
    const val SYNC_PATH = "/api/integrations/chronopass/sync"
    const val PHOTOS_PATH = "/api/integrations/chronopass/photos"
    const val PULL_PATH = "/api/integrations/chronopass/pull"
    const val EMPLOYEE_PHOTO_PATH = "/api/integrations/chronopass/employee-photo/"

    /** Concatena a base (tolerante a barra final) com o path fixo do contrato. */
    fun endpointUrl(base: String, path: String): String = base.trimEnd('/') + path

    /** URL da descida. `since` vazio (primeiro pull / cursor perdido) = janela completa. */
    fun pullUrl(base: String, since: String): String =
            endpointUrl(base, PULL_PATH) +
                    if (since.isBlank()) "" else "?since=" + URLEncoder.encode(since, "UTF-8")

    fun employeePhotoUrl(base: String, uid: String): String =
            endpointUrl(base, EMPLOYEE_PHOTO_PATH) + URLEncoder.encode(uid, "UTF-8")

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

    /** Mesma montagem de conexão do [post] (timeouts, header de auth) para os dois GETs. */
    private fun abrirGet(url: String, apiKey: String): HttpURLConnection =
            (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

    /** GET que lê o corpo (descida: o pull). Suspend: roda em Dispatchers.IO. */
    suspend fun get(url: String, apiKey: String): GetResult =
            withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    conn = abrirGet(url, apiKey)
                    val code = conn.responseCode
                    if (code in 200..299) {
                        GetResult.Ok(conn.inputStream.bufferedReader().use { it.readText() })
                    } else {
                        val snippet =
                                try {
                                    conn.errorStream
                                            ?.bufferedReader()
                                            ?.use { it.readText() }
                                            ?.take(200)
                                            ?: ""
                                } catch (_: Exception) {
                                    ""
                                }
                        GetResult.Erro("HTTP $code $snippet".trim())
                    }
                } catch (e: Exception) {
                    GetResult.Erro("rede: ${e.message ?: e.javaClass.simpleName}")
                } finally {
                    conn?.disconnect()
                }
            }

    /**
     * Bytes crus da foto de funcionário. null em qualquer falha (rede, HTTP, tamanho acima do
     * teto): imagem não é dado de marcação, a próxima rodada tenta de novo pelo mesmo photoHash.
     */
    suspend fun getBytes(url: String, apiKey: String): ByteArray? =
            withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    conn = abrirGet(url, apiKey)
                    if (conn.responseCode !in 200..299) null
                    else conn.inputStream.use { lerLimitado(it, MAX_PHOTO_BYTES) }
                } catch (_: Exception) {
                    null
                } finally {
                    conn?.disconnect()
                }
            }

    // Leitura limitada: Content-Length é declarado pelo outro lado, então o teto é medido no que
    // realmente chega — resposta gigante/infinita não vira OOM.
    private fun lerLimitado(input: InputStream, max: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) return out.toByteArray()
            if (out.size() + n > max) return null
            out.write(buf, 0, n)
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
     * key = uid do punch (ou "employee.<uid>" p/ foto de funcionário); fileName = nome base do
     * arquivo. Ponte enxuta: a Fase 4 decide quando chamar (foto ainda na fila, etc.).
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
