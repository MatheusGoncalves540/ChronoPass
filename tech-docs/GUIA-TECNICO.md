# ChronoPass — Guia Técnico (codebase)

Documentação técnica do código-fonte, para **desenvolvedores** que vão compilar,
testar, manter ou evoluir o projeto. Para o uso do app no dia a dia (linguagem
simples), veja o [Manual Operacional](../MANUAL-OPERACIONAL.md).

> Grafo de conhecimento do código: `graphify-out/` contém a navegação cruzada do
> código (god nodes, comunidades, relações entre arquivos). Use
> `graphify query "<pergunta>"` / `graphify path A B` antes de fuçar o código, e
> rode `graphify update .` depois de modificar código.

---

## 1. Visão geral

Aplicativo Android nativo (Kotlin + Jetpack Compose, Material 3), **100% offline
para o uso diário** — a marcação de ponto nunca depende de internet. Arquitetura
simples em pacotes por responsabilidade, com estado compartilhado em um `ViewModel`
único (`ui/ChronoViewModel.kt`). Persistência local com Room/SQLite; fotos no
armazenamento privado do app (`android:allowBackup="false"`), nunca na galeria.

Integração **opcional** com o SummusBackoffice (pacote `sync/`, detalhes no §12): sem
URL + api-key configuradas em Configurações, o app roda exatamente como um app 100%
local, sem servidor nenhum. Com a integração ativa, o app envia ponto/cadastro e
recebe cadastro, loja e correções de ponto — sem perder nenhuma marcação já gravada.

Princípio de produto: **livro de ponto digital com evidência fotográfica e
geográfica**, não um sistema biométrico.

## 2. Stack e versões

| Item | Valor |
|---|---|
| Versão | 2.2.0 (versionCode 4) |
| SDK | minSdk 26 (Android 8.0) · targetSdk 35 · compileSdk 35 |
| Linguagem | Kotlin |
| Build | JDK 17 · Gradle wrapper |
| UI | Jetpack Compose (Material 3, BOM 2024.12.01) |
| Banco | Room 2.6.1 (SQLite) |
| Câmera | CameraX 1.4.1 |
| Localização | play-services-location 21.3.0 (Fused Location Provider) |
| Imagens | Coil 2.7.0 |
| PDF | `PdfDocument` do SDK (zero dependência) |

## 3. Estrutura do projeto

```
app/src/main/java/com/chronopass/app/
├── MainActivity.kt          # Navegação (NavHost) e gatilho do auto-update
├── data/                    # Room: entities, DAOs, database, repositório
│   ├── PunchRules.kt        # Regras de negócio puras (testáveis na JVM)
│   └── entities/            # Employee, Punch, Store, AppSetting
├── camera/                  # CameraX + armazenamento privado das fotos
├── location/                # LocationHelper (Fused Location)
├── sync/                    # Sincronização com o SummusBackoffice, subida + descida (§12)
├── reports/                 # PdfExport, CsvExport, ReportPeriod, TimeUtil
├── backup/                  # BackupManager (zip: data.json + fotos)
├── update/                  # UpdateChecker (GitHub Releases)
└── ui/
    ├── screens/             # Home, Punch, Admin, Employees, Records, Reports, Settings
    ├── components/          # Logo, EmployeePicker
    └── theme/               # Material 3

scripts/                     # dev.bat, apk.bat, test.bat, release.bat, gradlew.bat
docs/                        # PLANO.md (especificação/MVP), PLANO-FUTURO.md (visão facial)
keystore/                    # Chave de release (gitignorada)
graphify-out/                # Grafo de conhecimento do código (gerado por graphify)
```

## 4. Onde está cada item

| Requisito | Arquivo |
|---|---|
| Selecionar funcionário / próxima marcação | `ui/screens/HomeScreen.kt`, `ui/screens/PunchScreen.kt` |
| Regra entrada↔saída, horas e almoço CLT | `data/PunchRules.kt` |
| Câmera + foto privada | `camera/CameraCapture.kt`, `camera/PhotoStore.kt`, `camera/PhotoCompressor.kt` |
| Localização (lat/lon/precisão) | `location/LocationHelper.kt` |
| Loja + raio (somente-leitura quando a loja é gerida pelo Summus) | `data/entities` (Store), `ui/screens/SettingsScreen.kt` |
| Banco Room | `data/database`, `data/dao`, `data/repo` |
| Sincronização com o SummusBackoffice (subida + descida) | `sync/SummusClient.kt`, `sync/SyncManager.kt`, `sync/SyncRules.kt`, `sync/SummusPayloads.kt`, `sync/PullPayloads.kt`, `sync/OutboxPayloads.kt` |
| Redimensionamento/compressão de foto (teto 1280px, WEBP) | `camera/PhotoCompressor.kt`, `camera/ImageScale.kt` |
| Área admin (senha) | `ui/screens/AdminScreen.kt` (padrão `1234`, troque em Configurações) |
| Funcionários CRUD + foto de cadastro | `ui/screens/EmployeesScreen.kt` |
| Marcações + correção + excluir | `ui/screens/RecordsScreen.kt` |
| CSV / PDF | `reports/CsvExport.kt`, `reports/PdfExport.kt` |
| Períodos de relatório | `reports/ReportPeriod.kt`, `reports/TimeUtil.kt` |
| Backup / Restaurar (zip) | `backup/BackupManager.kt`, `ui/screens/ReportsScreen.kt` |
| Auto-update (GitHub Releases) | `update/UpdateChecker.kt` |
| Navegação | `MainActivity.kt` |
| Estado compartilhado | `ui/ChronoViewModel.kt` |

## 5. Regras de negócio (`PunchRules`, testadas em JVM)

- **Horas trabalhadas** = soma dos intervalos Entrada→Saída. Marcação sem par
  (entrada pendente ou saída solta) é ignorada, não descarta o resto do dia.
- **Almoço** = soma dos intervalos Saída→Entrada do dia.
- **Intervalo mínimo CLT**: 1h para jornada > 6h, 30 min para 4–6h. Abaixo disso,
  o dia ganha um `*` no PDF e um aviso no rodapé.
- Correções manuais de marcações são normalizadas para nunca casar pares errados.

## 6. Build

Não há SDK Android neste ambiente — abra em **Android Studio** (Giraffe+ / JDK 17):

```
Open project  →  d:/BUSINESS/ChronoPass
```

O Studio gera o `gradle-wrapper.jar`. Ou, com Gradle instalado:
`gradle wrapper && ./gradlew assembleDebug`.

### Scripts (`scripts/`)

| Script | O que faz |
|---|---|
| `dev.bat` | Build + instala no emulador `chrono` (inicia se preciso) e abre o app |
| `apk.bat` | Gera o APK release em `app/build/outputs/apk/release/app-release.apk` |
| `test.bat` | Roda os testes de lógica (não precisa de emulador) |
| `release.bat` | Roda testes, builda o APK release, faz o bump de versão (tag vX.Y.Z) e publica no GitHub Releases (branch `production`) |
| `contract-test.bat [baseUrl] [apiKey]` | Roda `SummusContractTest` (E2E) contra um SummusBackoffice real — default `http://localhost:3001` / chave `dev` |

## 7. Testes

```
./gradlew test
```

Testes de lógica pura na JVM (sem emulador):

| Teste | Cobre |
|---|---|
| `PunchRulesTest` | Alternância entrada/saída, soma de horas (incluindo turno que vira a meia-noite e dia sem par), almoço e mínimo CLT |
| `ReportPeriodTest` | Intervalos de "este mês", "mês passado", 7/30 dias |
| `UpdateCheckerTest` | Comparação de versões do auto-update |
| `SyncRulesTest` | Backoff medido de `lastAttemptAt` (não `createdAt`); guarda de revisão da descida (só aplica revisão maior que a local) |
| `PullPayloadsTest` | Parser do envelope do pull: campos completos, nulos explícitos, listas vazias/ausentes, `schemaVersion` inesperado e envelope inválido falham limpo (sem crash, sem avançar cursor) |
| `ApplyFromSummusTest` | `applyFromSummus` não enfileira na `sync_outbox` (seam anti-eco); merge de funcionário e de correção de ponto |
| `OutboxPayloadsTest` | Codec da fila `sync_outbox`: round-trip de employee/punch, nulos explícitos |
| `SummusPayloadsTest` | Montagem dos lotes 1/2 no shape real do servidor (`punchType`, RFC3339, `photoKey`, etc.) |
| `ImageScaleTest` | Cálculo do `inSampleSize`: teto de 1280px no maior lado, teto inválido não trava |
| `SummusContractTest` | E2E opcional contra um SummusBackoffice real (lotes 1/2, ack 2xx) — pulado automaticamente sem `SUMMUS_TEST_BASE_URL` definida; ver `scripts/contract-test.bat` |

## 8. Chave de release

A assinatura de release usa `keystore/keystore.properties` + `keystore/chronopass-release.jks`
(ambos gitignorados). Num clone novo sem esses arquivos, o build continua funcionando, mas
cai para a chave de **debug** e avisa.

## 9. Permissões

| Permissão | Para quê |
|---|---|
| `CAMERA` | Foto da marcação e foto de cadastro do funcionário |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Registrar lat/lon/precisão no ponto |
| `INTERNET` | Somente o auto-update (GitHub Releases) |
| `REQUEST_INSTALL_PACKAGES` | Instalar o APK baixado na atualização |

Fotos e dados ficam no armazenamento privado do app (`android:allowBackup="false"`), nunca na galeria.

## 10. Imagens trocáveis (build-time)

| Imagem | Arquivo | Onde aparece |
|---|---|---|
| Logo da loja | `app/src/main/assets/logo.png` | Topo da tela inicial e cabeçalho do PDF (grande, centralizada) |
| Ícone do app | `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png` | Ícone na área de trabalho do Android |

Troque o arquivo e recompile (`scripts/dev.bat` / `scripts/apk.bat`). Instruções detalhadas
(formato, margens, cor de fundo em `res/values/ic_launcher_background.xml`) em
`app/src/main/assets/LEIA-ME.txt`.

## 11. Atualização automática

Na abertura, o app consulta o **GitHub Releases** (`MatheusGoncalves540/ChronoPass`) e
oferece atualização quando existe versão nova. Baixa o APK para o armazenamento privado
com barra de progresso e instala via FileProvider (pede a permissão de "instalar apps de
fora da Play Store" quando necessário). Download em `.part` + rename: nunca instala um
arquivo pela metade.

## 12. Sincronização com o SummusBackoffice (opcional)

Feature de administração configurada em Configurações → SummusBackoffice (URL + api-key,
`app_settings`). Sem essas duas informações preenchidas, `SyncManager.sync` devolve
`SyncOutcome.Inativo` e o app funciona exatamente como sem integração nenhuma — nada muda
no uso diário. Contrato completo (payloads campo a campo) em
[`../SUMUS-INTEGRACAO.md`](../SUMUS-INTEGRACAO.md); aqui, o que o código faz.

### Subida (app → Summus) — já existia, sem mudança de contrato

- Fila persistente `sync_outbox` (Room): toda escrita de employee/punch enfileira um evento
  com o snapshot da entidade (`ChronoRepository`, método `enfileirar`).
- Dois lotes por rodada — metadados (`POST /api/integrations/chronopass/sync`) e fotos
  (`POST /api/integrations/chronopass/photos`) — cada um com teto de itens por rodada
  (`SyncRules.LOTE_METADADOS` = 50, `SyncRules.LOTE_FOTOS` = 20 fotos), repetindo até a fila
  esvaziar: um aparelho semanas offline não monta mais um `JSONObject` único em memória contra
  o `ReadTimeout` de 20s.
- Backoff por tentativa (`SyncRules.backoff`): 30s → 1min → 5min, medido a partir de
  `lastAttemptAt` (coluna nova na v5) — medir de `createdAt`, como antes, não freava retry de
  itens com mais de 5 minutos de idade.
- Foto de funcionário sobe no mesmo lote 2, com `key = "employee.<uid>"`; o servidor roteia
  chaves `employee.*` para o cadastro do RH em vez da lixeira de fotos de ponto.
- Gatilhos: abertura do app / retorno ao foreground, após cada ponto registrado, botão
  "Sincronizar agora". Sem WorkManager.

### Descida (Summus → app) — canal novo

`GET {base}/api/integrations/chronopass/pull?since=<cursor>`, atrás do mesmo api-key da
subida. Parser 100% JVM em `sync/PullPayloads.kt` (org.json, sem `android.*`, testável em
JUnit puro). Envelope, do jeito que o app consome:

```jsonc
{
  "schemaVersion": 1,
  "serverTime": "2026-09-05T14:03:12.000Z",
  "store": { "uid": "<rh_stores.id>", "name": "...", "latitude": -23.5, "longitude": -46.6, "radiusMeters": 150.0 },
  "employees": [
    { "uid": "<rh_employees.id>", "name": "...", "role": null, "active": true, "deleted": false, "photoHash": "<sha256|null>" }
  ],
  "punchCorrections": [
    { "uid": "...", "punchType": "in", "timestampUtc": "...", "editedBy": "...", "editedAt": "...",
      "editReason": "...", "deleted": false, "revision": 3 }
  ]
}
```

- `serverTime` é o cursor: guardado em `app_settings` (chave `summus_pull_since`), volta como
  `?since=` no próximo pull. Cursor OPACO — nenhuma aritmética de data do lado do app.
- `since` vazio (primeiro pull, ou cursor perdido) traz a janela completa; `store` e
  `employees` sempre vêm inteiros (sem paginação — o roster de uma loja é pequeno), só
  `punchCorrections` é recortado pelo `since`.
- Envelope inválido, `schemaVersion` diferente do esperado, ou `punchType` desconhecido
  derrubam o pull inteiro sem aplicar nada (`PullResult.Falha`) — o cursor não avança e o
  servidor reenvia a mesma janela no próximo pull.
- Foto de funcionário fica fora do corpo do pull: `photoHash` diferente do último aplicado
  dispara `GET {base}/api/integrations/chronopass/employee-photo/{uid}`, um funcionário por
  vez, com teto de 5 MiB (`SummusClient.MAX_PHOTO_BYTES`).

### Regra de ouro: nenhum dado de marcação se perde

- Toda escrita da descida passa por `ChronoRepository.applyFromSummus` / `applyPull` — grava
  direto nos DAOs **sem** enfileirar na `sync_outbox` (o "seam anti-eco": sem isso, a mudança
  recebida do servidor voltaria para cima no próximo sync).
- Correção de ponto é sempre **update**, nunca substituição cega: só aplica se a `revision`
  recebida for **maior** que `punch.serverRevision` local (`SyncRules.aplicaRevisao`) — reenvio
  da mesma correção não reaplica, e uma correção que chega fora de ordem não regride o ponto.
- `deleted:true` (funcionário ou ponto) é sempre soft-delete — a linha continua no banco,
  nunca é apagada.
- `MIGRATION_4_5` (`ChronoDatabase.kt`, version 5) cria índice único em `employee.uid` e
  `punch.uid`. Duplicata legada (de antes do índice existir) é resolvida **reatribuindo** um
  uid novo à linha mais nova, nunca com `DELETE` — nenhuma marcação já gravada some.

### Cadastro: o Summus manda, o app continua com fallback local

- `employee.origin` (`SUMMUS` | `LOCAL`) diz quem é dono do cadastro. Funcionário criado no
  Summus chega com `origin=SUMMUS` e o Summus passa a mandar em nome/cargo/status; funcionário
  cadastrado no próprio app nasce `origin=LOCAL` e continua funcionando offline normalmente —
  ele só sobe na subida e aparece do lado do Summus como sugestão de vínculo (a confirmação do
  vínculo é humana, não automática; não há casamento heurístico do lado do app).
- `employee.role` (cargo) só é preenchido pelo Summus; o app não tem tela para editar cargo.
- `store.managedBySummus` (setado quando o pull traz `store`) trava latitude, longitude e raio
  como **somente-leitura** na tela de Configurações (`SettingsScreen.kt`) — esses campos vêm do
  backoffice e seriam sobrescritos no próximo pull mesmo se editados localmente.

## 13. Documentos relacionados

| Documento | Natureza |
|---|---|
| `../docs/PLANO.md` | Especificação original e checklist do MVP (versão 1) |
| `../docs/PLANO-FUTURO.md` | Visão futura: reconhecimento facial, criptografia, auditoria |
| `../SUMUS-INTEGRACAO.md` | Contrato de sincronização com o SummusBackoffice — **implementado e testado** (schema dos payloads de subida e descida, fila `sync_outbox`, regras contra perda de dado) |
| `../MANUAL-OPERACIONAL.md` | Manual do usuário final (sem linguagem técnica) |

## 14. Fora de escopo (ver `../docs/PLANO-FUTURO.md`)

Reconhecimento facial, folha de pagamento, banco de horas complexo, AFD/Portaria 671 e
notificações — deliberadamente não implementados. (Sincronização com um backoffice existe
como integração opcional com o SummusBackoffice — ver §12 — mas o app em si continua
gerindo uma única loja por instalação; múltiplas lojas por aparelho não é implementado.)
O modelo de dados atual não impede adicionar o restante depois.
