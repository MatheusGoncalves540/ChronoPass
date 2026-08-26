# Graph Report - .  (2026-08-25)

## Corpus Check
- Corpus is ~19,239 words - fits in a single context window. You may not need a graph.

## Summary
- 345 nodes · 442 edges · 42 communities (21 shown, 21 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 38 edges (avg confidence: 0.85)
- Token cost: 0 input · 194,404 output

## Community Hubs (Navigation)
- App Navigation & ViewModel
- Database & Settings Storage
- Feature-to-File Index
- Punch Records & CSV Export
- Descoped Biometric Auth & Assets
- Employee Management
- Planned Data & Punch Architecture
- ChronoRepository Data Facade
- Punch Flow & Location Capture
- Time Utility Functions
- Punch Type & Business Rules
- Backup Import/Export
- Home Screen & Logo
- Camera Capture
- Report Period Enum
- Report Period Tests
- PDF Report Export
- Punch Rules Tests
- Photo File Storage
- Project Vision & Prototype
- Gradle Wrapper Script
- Planned Test Strategy
- Launcher Icon Artwork
- Planned Admin Override & Audit Log
- Planned Face Recognition Module
- Planned Report Module (PDF SDK)
- Logo Image Asset
- Planned Admin Panel Screens
- Planned DI Container
- Planned Backup Encryption
- Planned Employee Mode Screens
- Planned AppSetting Entity
- Planned Implementation Phases
- Planned Native Kotlin Architecture
- Planned App Module
- Planned Paging3 UI Rule
- Planned Photo Encryption
- Planned UI Load Test

## God Nodes (most connected - your core abstractions)
1. `ChronoRepository` - 26 edges
2. `ChronoViewModel` - 25 edges
3. `Punch` - 23 edges
4. `Employee` - 20 edges
5. `Tabela "Onde está cada item do MVP"` - 19 edges
6. `TimeUtil` - 14 edges
7. `EmployeeDao` - 12 edges
8. `PunchDao` - 12 edges
9. `Store` - 10 edges
10. `App()` - 9 edges

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

## Communities (42 total, 21 thin omitted)

### Community 0 - "App Navigation & ViewModel"
Cohesion: 0.07
Nodes (19): AndroidViewModel, App(), MainActivity, ChronoViewModel, AdminScreen(), NavController, EmployeeDialog(), EmployeesScreen() (+11 more)

### Community 1 - "Database & Settings Storage"
Cohesion: 0.09
Nodes (10): SettingsDao, StoreDao, ChronoDatabase, get(), Context, migrate(), AppSetting, Store (+2 more)

### Community 2 - "Feature-to-File Index"
Cohesion: 0.08
Nodes (27): BackupManager.kt, CameraCapture.kt, PhotoStore.kt, data/dao, data/database (Room DB), Store Entity (data/entities), PunchRules.kt, data/repo (+19 more)

### Community 3 - "Punch Records & CSV Export"
Cohesion: 0.11
Nodes (12): PunchDao, Punch, CsvExport, AddPunchDialog(), NavController, Period, ALL, TODAY (+4 more)

### Community 4 - "Descoped Biometric Auth & Assets"
Cohesion: 0.14
Nodes (21): LEIA-ME.txt (Guia de Imagens), Configuração do Ícone do App, ic_launcher_background.xml (Cor de Fundo do Ícone), Configuração da Logo (logo.png), Scripts de Rebuild (dev.bat/apk.bat/gradlew assembleRelease), Senha do Gerente (Argon2id), CameraX, admin_credential Entity (+13 more)

### Community 5 - "Employee Management"
Cohesion: 0.13
Nodes (4): EmployeeDao, Employee, EmployeePickerDialog(), Flow

### Community 6 - "Planned Data & Punch Architecture"
Cohesion: 0.10
Nodes (21): Room + SQLite (sem SQLCipher), Regra de Próxima Marcação (Entrada/Saída), employee Entity (Simple), punch Entity (Simple), Detecção de Relógio Adulterado (clockSuspect), Criptografia do Banco (SQLCipher + Keystore Envelope), employee Entity, face_template Entity (+13 more)

### Community 8 - "Punch Flow & Location Capture"
Cohesion: 0.15
Nodes (18): awaitOrNull(), distanceMeters(), Fix, freshLocation(), getCurrentFix(), Context, T, lastLocation() (+10 more)

### Community 10 - "Punch Type & Business Rules"
Cohesion: 0.17
Nodes (5): Converters, PunchType, IN, OUT, PunchRules

### Community 11 - "Backup Import/Export"
Cohesion: 0.40
Nodes (3): BackupManager, Context, JSONObject

### Community 12 - "Home Screen & Logo"
Cohesion: 0.24
Nodes (7): Bitmap, Context, LogoAsset, rememberLogo(), HomeScreen(), NavController, ImageBitmap

### Community 13 - "Camera Capture"
Cohesion: 0.32
Nodes (7): await(), CameraCapture(), Context, T, takePhoto(), ImageCapture, Modifier

### Community 14 - "Report Period Enum"
Cohesion: 0.25
Nodes (6): ReportPeriod, CUSTOM, LAST_30, LAST_7, LAST_MONTH, THIS_MONTH

### Community 16 - "PDF Report Export"
Cohesion: 0.38
Nodes (4): android, Bitmap, PdfExport, Paint

### Community 19 - "Project Vision & Prototype"
Cohesion: 0.40
Nodes (5): ChronoPass (Simplified Plan), ChronoPass (Facial Recognition Plan), face_recognition (dlib) Library, index.py Python Prototype, Princípio do Projeto (Livro de Ponto Digital)

### Community 20 - "Gradle Wrapper Script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "Planned Test Strategy"
Cohesion: 0.67
Nodes (3): TDD em Lógica Pura, Estratégia de Testes (Unit/Room/Segurança/Compose/Carga/Regressão), Comandos de Verificação (gradlew)

## Knowledge Gaps
- **60 isolated node(s):** `IN`, `OUT`, `THIS_MONTH`, `LAST_MONTH`, `LAST_7` (+55 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Punch` connect `Punch Records & CSV Export` to `App Navigation & ViewModel`, `Database & Settings Storage`, `ChronoRepository Data Facade`, `Punch Flow & Location Capture`, `Punch Type & Business Rules`, `Backup Import/Export`, `PDF Report Export`, `Punch Rules Tests`?**
  _High betweenness centrality (0.140) - this node is a cross-community bridge._
- **Why does `ChronoViewModel` connect `App Navigation & ViewModel` to `Database & Settings Storage`, `Punch Records & CSV Export`, `Employee Management`, `Punch Flow & Location Capture`, `Punch Type & Business Rules`, `Home Screen & Logo`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._
- **Why does `PunchScreen()` connect `Punch Flow & Location Capture` to `App Navigation & ViewModel`, `Punch Records & CSV Export`, `Camera Capture`?**
  _High betweenness centrality (0.096) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `Punch` (e.g. with `PunchScreen()` and `.totalWorked_sumsIntervals()`) actually correct?**
  _`Punch` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 4 inferred relationships involving `Tabela "Onde está cada item do MVP"` (e.g. with `EmployeesScreen.kt` and `RecordsScreen.kt`) actually correct?**
  _`Tabela "Onde está cada item do MVP"` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `IN`, `OUT`, `THIS_MONTH` to the rest of the system?**
  _60 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `App Navigation & ViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.07058823529411765 - nodes in this community are weakly interconnected._