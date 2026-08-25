# ChronoPass — App Android de ponto por reconhecimento facial

## Contexto

Hoje o projeto tem só `index.py`: um protótipo em Python (`face_recognition` + `cv2`) que
carrega a foto de um funcionário, tira uma foto na webcam e compara. A ideia está certa, mas
`face_recognition` é dlib compilado e **não roda no Android** — nem via Kivy/Chaquopy.

O objetivo é um app Android que fica no celular da loja para os funcionários baterem ponto por
reconhecimento facial, 100% offline, com painel administrativo embutido e exportação de
relatórios. O dado em jogo é biométrico (dado sensível pela LGPD) + folha de ponto, então
criptografia em repouso e testes não são opcionais.

**Decisões já fechadas com o usuário:**

| Tema | Decisão |
|---|---|
| Arquitetura | Kotlin + Jetpack Compose, tudo nativo no aparelho, sem servidor |
| Fluxo de ponto | 1:1 — toca no nome, o rosto confirma |
| Anti-fraude | Desafio de vida (piscar/virar) + foto da marcação guardada cifrada |
| Marcações | Livres: pares entrada/saída alternados, ilimitados por dia |
| Exportação | CSV, PDF (espelho de ponto) e backup cifrado completo |
| Acesso admin | Senha de gerente (Argon2id) + chave do banco no Android Keystore |
| Modelo facial | Eu baixo um MobileFaceNet `.tflite` de licença permissiva e commito |
| Internet | Fora de escopo agora; o modelo de dados não pode impedir sync futuro |

---

## Stack

- **Kotlin + Jetpack Compose (Material 3)**, minSdk 26, targetSdk 35
- **CameraX** — preview + `ImageAnalysis`
- **ML Kit Face Detection (bundled)** — detecção, recorte, `eyeOpenProbability` e
  `headEulerAngleY` (o desafio de vida sai de graça daqui)
- **LiteRT/TFLite + MobileFaceNet** — embedding de 192 floats; equivale ao
  `face_encodings` do protótipo
- **Room + SQLCipher** — banco cifrado
- **PDF pelo `android.graphics.pdf.PdfDocument`** (SDK, zero dependência)
- **DI manual** via `AppContainer` — sem Hilt

> Trade-off registrado: ML Kit *bundled* pesa ~16 MB mas garante funcionamento sem internet e
> sem Play Services. A variante *unbundled* deixaria o APK em ~10 MB porém baixa o modelo no
> primeiro uso. Como é celular de loja e o requisito é offline, fica bundled. APK final ~30 MB.

## Módulos Gradle

```
:app          Compose, navegação, telas, DI container
:core-data    Room + SQLCipher, entidades, DAOs, repositórios, crypto, auditoria
:core-face    CameraX, ML Kit, TFLite, liveness, matching
:core-report  CSV, PDF, backup/restore cifrado
```

`:core-data` e `:core-report` não dependem de UI — testáveis na JVM, sem emulador.
`:core-face` expõe uma interface `FaceEmbedder` para que os testes rodem sem o modelo real.

---

## Modelo de dados

UUID como PK, `updatedAt` em tudo e exclusão lógica (`deletedAt`) — é o que deixa a porta
aberta pro sync por internet depois sem migração dolorosa.

- `employee(id, name, code, active, createdAt, updatedAt, deletedAt, consentAcceptedAt)`
- `face_template(id, employeeId, embedding BLOB[192 floats], quality, createdAt)` — 3 a 5 por
  pessoa, capturados em ângulos/luzes diferentes
- `punch(id, employeeId, timestampUtc, tzOffsetMinutes, direction IN|OUT, source FACE|ADMIN,
  matchScore, livenessPassed, photoFile, bootId, elapsedRealtime, clockSuspect,
  createdAt, updatedAt, deletedAt)`
- `audit_log(id, actor, action, targetTable, targetId, before, after, timestampUtc)`
- `admin_credential(id, argon2Hash, salt, failedAttempts, lockedUntil)`
- `app_setting(key, value)`

**Direção da marcação:** o app lê o último `punch` não-deletado do funcionário no dia e alterna.
Sem `punch` no dia → `IN`. A tela mostra qual será antes de confirmar.

**Detecção de relógio adulterado:** grava `bootId` + `SystemClock.elapsedRealtime()` junto do
horário. Se o relógio andou pra trás em relação ao último ponto do mesmo boot, marca
`clockSuspect = true` — o ponto é gravado (nunca se perde marcação) mas aparece sinalizado no
painel e nos relatórios.

---

## Criptografia

| O quê | Como |
|---|---|
| Banco | SQLCipher, passphrase de 32 bytes de `SecureRandom` |
| Passphrase | Envelopada com chave AES-GCM do Android Keystore (hardware quando disponível) |
| Fotos das marcações | AES-GCM com chave própria do Keystore, arquivos em `filesDir/photos/` |
| Senha do gerente | Argon2id (`argon2kt`), parâmetros m=64MB t=3 p=2 |
| Backup `.cpbk` | AES-GCM com chave derivada por Argon2id de uma senha escolhida na hora da exportação |
| Tentativas | Backoff progressivo: 5 erros → 30s, dobrando até 15min |

O backup existe justamente porque a chave do Keystore morre com o aparelho: **sem ele, celular
quebrado = histórico perdido**. Por isso a chave do backup vem de senha, não do Keystore.

Endurecimento: `android:allowBackup="false"`, `FLAG_SECURE` nas telas de admin e de cadastro
facial, embeddings e fotos jamais em log, `.tflite` e assets sem dado pessoal.

**LGPD:** tela de consentimento no cadastro do funcionário com registro do aceite; embeddings
são vetores não reversíveis (não guardamos a foto de cadastro, só os vetores); excluir o
funcionário apaga templates e fotos de verdade, não só o registro.

---

## Reconhecimento facial

Pipeline: CameraX → ML Kit detecta e recorta o maior rosto → normaliza 112×112 → MobileFaceNet
→ embedding L2-normalizado → distância cosseno contra os templates daquele funcionário → aceita
se `melhor distância < limiar`.

- **Limiar configurável no painel** — nenhum número mágico chumbado. O mundo real (luz da loja,
  câmera do aparelho, óculos) muda a distribuição; sem esse ajuste o app fica inutilizável na
  loja de iluminação ruim. Padrão 0.38, faixa 0.30–0.50, com um modo de diagnóstico que mostra
  a distância obtida.
- **Liveness** antes do match: um desafio sorteado entre piscar (`eyeOpenProbability` cai abaixo
  de 0.3 e volta acima de 0.7) e virar o rosto (`headEulerAngleY` passa de ±20°), com timeout de
  8 s. Falhou → não grava e oferece nova tentativa.
- **Falha de reconhecimento** → oferece tentar de novo; após 3 falhas, oferece registrar com
  autorização do gerente (`source = ADMIN`, entra na auditoria).

---

## Interfaces que crescem sem limite

Requisito explícito, tratado como regra de arquitetura e não como detalhe de tela:

- Toda lista é `LazyColumn` com **Paging 3** — funcionários, marcações, auditoria, relatórios.
  Nada de `Column` com `forEach` sobre a coleção inteira.
- **Nenhum dropdown com dados**: escolher funcionário/período abre uma tela de busca com filtro
  incremental. `ExposedDropdownMenu` só para conjuntos fixos e pequenos (tipo de relatório).
- Nome longo quebra em duas linhas, nunca é truncado com reticências.
- Tabela do relatório: scroll horizontal e vertical independentes, cabeçalho fixo.
- Tudo em `sp`/`dp`; testes de UI rodam com `fontScale = 2.0` para provar que nada corta.
- Teste instrumentado com **10.000 funcionários e 200.000 marcações** verificando scroll fluido
  e busca responsiva.

---

## Telas

**Modo funcionário (padrão, sem saída fácil):**
1. Lista de funcionários com busca (LazyColumn paginada)
2. Confirmação: nome + qual marcação será (Entrada/Saída) + botão grande
3. Câmera com desafio de vida e feedback ao vivo
4. Resultado: "Ponto registrado — João, Entrada, 08:03"

**Painel admin** (toque longo no logo → senha):
- Funcionários: listar, cadastrar (com consentimento + captura de 3–5 templates), editar, desativar
- Marcações: listar/filtrar por período e pessoa, corrigir com justificativa obrigatória (auditado)
- Relatórios: período + pessoa(s) → CSV / PDF → `Intent.ACTION_SEND` via FileProvider
- Backup: exportar `.cpbk` cifrado / restaurar
- Configurações: limiar de reconhecimento, diagnóstico, política de retenção de fotos
- Auditoria: log completo, somente leitura

---

## Testes

| Camada | O que cobre |
|---|---|
| Unit (JVM) | matching e limiar, pareamento entrada/saída e soma de horas (incluindo turno que vira a meia-noite e dia sem par), geração de CSV, round-trip de cripto, backup→restore, detecção de relógio adulterado, backoff de senha |
| Room in-memory | DAOs, queries de período, migrações |
| Segurança | o `.db` não abre sem chave; arquivos de foto são bytes cifrados no disco; logs não contêm embedding nem nome |
| Compose UI | fluxo completo de ponto, entrada no admin, correção auditada |
| Carga de UI | 10.000 funcionários / 200.000 marcações; `fontScale 2.0` |
| Regressão facial | conjunto fixo de imagens em `androidTest/assets` medindo falsa aceitação e falsa rejeição a cada mudança no pipeline |

TDD nas partes de lógica pura (`:core-data`, `:core-report`): teste antes da implementação.

---

## Fases de implementação

1. **Fundação** — 4 módulos Gradle, Room + SQLCipher, Keystore, `AppContainer`, testes de cripto
2. **Funcionários** — CRUD + consentimento + auditoria, listas paginadas
3. **Face** — CameraX + ML Kit + MobileFaceNet, cadastro de templates, liveness, matching
4. **Ponto** — tela de marcação ponta a ponta, alternância entrada/saída, guarda de relógio
5. **Admin** — senha Argon2id, correção auditada, configurações
6. **Relatórios** — CSV, PDF, backup/restore, compartilhamento
7. **Endurecimento** — FLAG_SECURE, revisão de logs, testes de carga de UI, teste de fonte 200%

Fases 1–2 e 6 são testáveis sem emulador. Fases 3–5 exigem aparelho ou emulador com câmera.

---

## Verificação

```bash
./gradlew test                 # unit JVM: cripto, horas, CSV, backup
./gradlew connectedAndroidTest # Room, Compose, carga de UI, regressão facial
./gradlew assembleRelease      # confere tamanho do APK (~30 MB esperado)
```

Ponta a ponta no aparelho: cadastrar funcionário → bater entrada → bater saída → exportar CSV e
PDF do dia e conferir o total de horas → exportar backup → limpar dados do app → restaurar →
conferir que as marcações voltaram.

Prova de que a criptografia funciona: puxar o `.db` do aparelho via `adb` e verificar que
`sqlite3` recusa abrir e que `strings` não devolve nenhum nome de funcionário.

---

## Fora de escopo (por enquanto)

- Sincronização por internet — o esquema já está pronto (UUID, `updatedAt`, exclusão lógica),
  mas nada de rede é escrito agora
- AFD / Portaria 671 — só se a fiscalização exigir formalmente
- Escalas, horas extras, adicional noturno, banco de horas — o app registra e soma; o cálculo
  trabalhista fica com quem processa a folha
- Múltiplas lojas num aparelho só
- Modelo anti-spoofing dedicado — o desafio de vida cobre foto parada; se aparecer fraude com
  vídeo, entra um `.tflite` de anti-spoof sem mexer no resto do pipeline
