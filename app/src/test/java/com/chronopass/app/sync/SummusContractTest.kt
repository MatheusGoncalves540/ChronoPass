// Contrato E2E contra o SummusBackoffice REAL (SUMUS-INTEGRACAO.md §10.3/§5).
//
// Envia Lote 1 (metadados) e Lote 2 (fotos) montados com SummusPayloads — o MESMO código de
// produção — e valida ack 2xx do backend. Roda em JVM pura (JUnit 4), sem emulador.
//
// Config por variáveis de ambiente (propagam ao JVM de teste sem mexer no Gradle):
//   SUMMUS_TEST_BASE_URL  -> ex.: http://localhost:3001 (dev) ou https://SEU-BACKEND (prod)
//   SUMMUS_TEST_API_KEY   -> ex.: dev
//
// Se SUMMUS_TEST_BASE_URL não estiver definida o teste é PULADO (Assume) — produção/CI normal
// (`./gradlew test`) não quebram. Rodar contra dev e prod só muda a config (URL + chave via env).
package com.chronopass.app.sync

import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import com.chronopass.app.data.entities.Store
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

class SummusContractTest {

    // 1x1 JPEG mínimo, base64 fixo (válido para decodificar no backend).
    private val jpeg1x1Base64 =
            "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AKp//2Q=="

    private fun now(): Long = System.currentTimeMillis()

    private fun contractEmployee(createdAt: Long) =
            Employee(
                    id = 1L,
                    uid = "uid-test-emp-1",
                    name = "Teste Integração",
                    code = "TESTE-1",
                    active = true,
                    deleted = false,
                    createdAt = createdAt,
            )

    // Ponto com foto (photoPath só alimenta o fileName do ref) — o Lote 2 carrega os bytes.
    private fun contractPunch(timestamp: Long) =
            Punch(
                    id = 1L,
                    uid = "uid-test-punch-1",
                    employeeId = 1L,
                    timestamp = timestamp,
                    type = PunchType.IN,
                    latitude = -23.5505,
                    longitude = -46.6333,
                    accuracy = 5f,
                    photoPath = "test.jpg",
                    createdAt = timestamp,
                    deleted = false,
            )

    private fun contractStore() =
            Store(
                    id = 1L,
                    name = "Loja Teste",
                    latitude = -23.5505,
                    longitude = -46.6333,
                    radius = 100f
            )

    private fun contractPhoto(exportedAt: Long) =
            SummusPayloads.PhotoPayload(
                    key = "uid-test-punch-1",
                    fileName = "test.jpg",
                    contentType = "image/jpeg",
                    dataBase64 = jpeg1x1Base64,
            )

    private fun requireEnv(): Pair<String, String> {
        val baseUrl = System.getenv("SUMMUS_TEST_BASE_URL")
        // Pula silenciosamente quando a env não foi setada (teste normal/CI/produção não quebra).
        assumeTrue(
                "SUMMUS_TEST_BASE_URL não definida — contract test pulado",
                !baseUrl.isNullOrBlank()
        )
        val apiKey = System.getenv("SUMMUS_TEST_API_KEY") ?: ""
        return baseUrl to apiKey
    }

    @Test
    fun lote1_metadados_aceitoPeloBackend() = runBlocking {
        val (baseUrl, apiKey) = requireEnv()
        val exportedAt = now()
        val lote1 =
                SummusPayloads.buildLote1(
                        deviceId = "chronopass-contract-test",
                        deviceModel = "jvm",
                        appVersion = "test",
                        store = contractStore(),
                        exportedAt = exportedAt,
                        employees = listOf(contractEmployee(exportedAt - 60_000)),
                        punches = listOf(contractPunch(exportedAt - 30_000)),
                )
        val result = SummusClient.postSync(baseUrl, apiKey, lote1)
        when (result) {
            is SummusClient.PostResult.Ack ->
                    println("Lote 1 (sync) -> ACK HTTP ${result.httpCode}")
            is SummusClient.PostResult.HttpError ->
                    fail("Lote 1 (sync) -> HTTP ${result.httpCode}: ${result.bodySnippet}")
            is SummusClient.PostResult.TransportError ->
                    fail("Lote 1 (sync) -> falha de transporte: ${result.message}")
        }
    }

    @Test
    fun lote2_fotos_aceitoPeloBackend() = runBlocking {
        val (baseUrl, apiKey) = requireEnv()
        val exportedAt = now()
        val lote2 =
                SummusPayloads.buildLote2(
                        deviceId = "chronopass-contract-test",
                        deviceModel = "jvm",
                        store = contractStore(),
                        exportedAt = exportedAt,
                        photos = listOf(contractPhoto(exportedAt)),
                )
        val result = SummusClient.postPhotos(baseUrl, apiKey, lote2)
        when (result) {
            is SummusClient.PostResult.Ack ->
                    println("Lote 2 (photos) -> ACK HTTP ${result.httpCode}")
            is SummusClient.PostResult.HttpError ->
                    fail("Lote 2 (photos) -> HTTP ${result.httpCode}: ${result.bodySnippet}")
            is SummusClient.PostResult.TransportError ->
                    fail("Lote 2 (photos) -> falha de transporte: ${result.message}")
        }
    }
}
