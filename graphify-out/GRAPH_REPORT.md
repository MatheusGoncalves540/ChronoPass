# Graph Report - ChronoPass  (2026-08-28)

## Corpus Check
- 40 files · ~22,394 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 399 nodes · 560 edges · 46 communities (22 shown, 24 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 55 edges (avg confidence: 0.83)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `bb7999ef`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- App
- Store
- Tabela "Onde está cada item do MVP"
- Punch
- Pipeline de Reconhecimento Facial
- ChronoViewModel
- punch Entity (Simple)
- ChronoRepository
- PunchScreen
- TimeUtil
- UpdateChecker
- .loadJson
- rememberLogo
- CameraCapture
- ReportPeriod
- ReportPeriodTest
- PdfExport
- RecordsScreen
- PhotoStore
- ChronoPass (Simplified Plan)
- gradlew
- Estratégia de Testes (Unit/Room/Segurança/Compose/Carga/Regressão)
- ic_launcher_foreground.png
- Registro Manual Após Falhas (source=ADMIN)
- FaceEmbedder Interface
- Gradle Module :core-report
- Boi do Forte Logo (Center Carnes)
- Painel Admin (Telas)
- AppContainer (Manual DI)
- Backup Cifrado (.cpbk)
- Modo Funcionário (Telas)
- app_setting Entity
- Fases de Implementação
- Kotlin + Jetpack Compose Native Architecture
- Gradle Module :app
- Regra de Interfaces com Paging 3 / Sem Truncamento
- Criptografia das Fotos (AES-GCM)
- Teste de Carga de UI (10k funcionários / 200k marcações)
- AGENTS.md
- PhotoCompressor
- CLAUDE.md
- UpdateCheckerTest

## God Nodes (most connected - your core abstractions)
1. `Punch` - 29 edges
2. `ChronoRepository` - 26 edges
3. `ChronoViewModel` - 25 edges
4. `Employee` - 20 edges
5. `Tabela "Onde está cada item do MVP"` - 19 edges
6. `TimeUtil` - 14 edges
7. `EmployeeDao` - 12 edges
8. `PunchDao` - 12 edges
9. `PdfExport` - 12 edges
10. `Row` - 11 edges

## Surprising Connections (you probably didn't know these)
- `employee Entity (Simple)` --semantically_similar_to--> `employee Entity`  [INFERRED] [semantically similar]
  PLANO.md → PLANO-FUTURO.md
- `punch Entity (Simple)` --semantically_similar_to--> `punch Entity`  [INFERRED] [semantically similar]
  PLANO.md → PLANO-FUTURO.md
- `Segurança Proporcional (sem Argon2id/biometria/liveness)` --conceptually_related_to--> `MobileFaceNet TFLite Model`  [INFERRED]
  PLANO.md → PLANO-FUTURO.md
- `Segurança Proporcional (sem Argon2id/biometria/liveness)` --conceptually_related_to--> `Senha do Gerente (Argon2id)`  [INFERRED]
  PLANO.md → PLANO-FUTURO.md
- `ChronoPass (Simplified Plan)` --conceptually_related_to--> `ChronoPass (Facial Recognition Plan)`  [INFERRED]
  PLANO.md → PLANO-FUTURO.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Gradle Modules (:app, :core-data, :core-face, :core-report)** — plano_futuro_module_app, plano_futuro_module_core_data, plano_futuro_module_core_face, plano_futuro_module_core_report [EXTRACTED 1.00]
- **Reporting & Backup Data Portability Flow** — plano_relatorios, plano_exportacao_csv, plano_exportacao_pdf, plano_backup_zip [INFERRED 0.75]
- **Arquitetura de Criptografia** — plano_futuro_db_encryption, plano_futuro_photo_encryption, plano_futuro_admin_password_argon2id, plano_futuro_backup_encryption_cpbk, plano_futuro_login_attempt_backoff [EXTRACTED 1.00]

## Communities (46 total, 24 thin omitted)

### Community 0 - "App"
Cohesion: 0.12
Nodes (13): App(), MainActivity, AdminScreen(), NavController, NavController, ReportsScreen(), slug(), today() (+5 more)

### Community 1 - "Store"
Cohesion: 0.07
Nodes (14): SettingsDao, StoreDao, ChronoDatabase, Converters, get(), Context, migrate(), AppSetting (+6 more)

### Community 2 - "Tabela "Onde está cada item do MVP""
Cohesion: 0.08
Nodes (27): BackupManager.kt, CameraCapture.kt, PhotoStore.kt, data/dao, data/database (Room DB), Store Entity (data/entities), PunchRules.kt, data/repo (+19 more)

### Community 3 - "Punch"
Cohesion: 0.09
Nodes (5): PunchDao, Punch, PunchRules, CsvExport, PunchRulesTest

### Community 4 - "Pipeline de Reconhecimento Facial"
Cohesion: 0.14
Nodes (21): LEIA-ME.txt (Guia de Imagens), Configuração do Ícone do App, ic_launcher_background.xml (Cor de Fundo do Ícone), Configuração da Logo (logo.png), Scripts de Rebuild (dev.bat/apk.bat/gradlew assembleRelease), Senha do Gerente (Argon2id), CameraX, admin_credential Entity (+13 more)

### Community 5 - "ChronoViewModel"
Cohesion: 0.09
Nodes (6): AndroidViewModel, EmployeeDao, Employee, ChronoViewModel, Flow, StateFlow

### Community 6 - "punch Entity (Simple)"
Cohesion: 0.10
Nodes (21): Room + SQLite (sem SQLCipher), Regra de Próxima Marcação (Entrada/Saída), employee Entity (Simple), punch Entity (Simple), Detecção de Relógio Adulterado (clockSuspect), Criptografia do Banco (SQLCipher + Keystore Envelope), employee Entity, face_template Entity (+13 more)

### Community 8 - "PunchScreen"
Cohesion: 0.16
Nodes (17): awaitOrNull(), distanceMeters(), Fix, freshLocation(), getCurrentFix(), Context, T, lastLocation() (+9 more)

### Community 10 - "UpdateChecker"
Cohesion: 0.23
Nodes (9): Context, UpdateAvailableDialog(), UpdateChecker, UpdateInfo, UpdatePhase, Ask, Downloading, NeedPermission (+1 more)

### Community 11 - ".loadJson"
Cohesion: 0.40
Nodes (3): BackupManager, Context, JSONObject

### Community 12 - "rememberLogo"
Cohesion: 0.24
Nodes (7): Bitmap, Context, LogoAsset, rememberLogo(), HomeScreen(), NavController, ImageBitmap

### Community 13 - "CameraCapture"
Cohesion: 0.19
Nodes (12): await(), CameraCapture(), Context, T, takePhoto(), CameraDialog(), EmployeeDialog(), EmployeesScreen() (+4 more)

### Community 14 - "ReportPeriod"
Cohesion: 0.25
Nodes (6): ReportPeriod, CUSTOM, LAST_30, LAST_7, LAST_MONTH, THIS_MONTH

### Community 16 - "PdfExport"
Cohesion: 0.29
Nodes (6): android, Bitmap, PdfExport, Row, EmployeePickerDialog(), Paint

### Community 17 - "RecordsScreen"
Cohesion: 0.24
Nodes (9): AddPunchDialog(), NavController, Period, ALL, TODAY, WEEK, YESTERDAY, PunchDetailDialog() (+1 more)

### Community 19 - "ChronoPass (Simplified Plan)"
Cohesion: 0.40
Nodes (5): ChronoPass (Simplified Plan), ChronoPass (Facial Recognition Plan), face_recognition (dlib) Library, index.py Python Prototype, Princípio do Projeto (Livro de Ponto Digital)

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "Estratégia de Testes (Unit/Room/Segurança/Compose/Carga/Regressão)"
Cohesion: 0.67
Nodes (3): TDD em Lógica Pura, Estratégia de Testes (Unit/Room/Segurança/Compose/Carga/Regressão), Comandos de Verificação (gradlew)

## Knowledge Gaps
- **65 isolated node(s):** `IN`, `OUT`, `THIS_MONTH`, `LAST_MONTH`, `LAST_7` (+60 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **24 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Punch` connect `Punch` to `Store`, `ChronoViewModel`, `ChronoRepository`, `PunchScreen`, `.loadJson`, `PdfExport`, `RecordsScreen`?**
  _High betweenness centrality (0.144) - this node is a cross-community bridge._
- **Why does `ChronoViewModel` connect `ChronoViewModel` to `App`, `Store`, `PunchScreen`, `rememberLogo`, `CameraCapture`, `RecordsScreen`?**
  _High betweenness centrality (0.097) - this node is a cross-community bridge._
- **Why does `PunchScreen()` connect `PunchScreen` to `App`, `Punch`, `ChronoViewModel`, `CameraCapture`, `PdfExport`?**
  _High betweenness centrality (0.087) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Punch` (e.g. with `PunchScreen()` and `.handlesDuplicateAdjacentPunch_withoutLosingTheGap()`) actually correct?**
  _`Punch` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Are the 4 inferred relationships involving `Tabela "Onde está cada item do MVP"` (e.g. with `EmployeesScreen.kt` and `RecordsScreen.kt`) actually correct?**
  _`Tabela "Onde está cada item do MVP"` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `IN`, `OUT`, `THIS_MONTH` to the rest of the system?**
  _65 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `App` be split into smaller, more focused modules?**
  _Cohesion score 0.12280701754385964 - nodes in this community are weakly interconnected._