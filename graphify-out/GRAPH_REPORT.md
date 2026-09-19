# Graph Report - ChronoPass  (2026-09-18)

## Corpus Check
- 58 files · ~43,180 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 696 nodes · 1328 edges · 40 communities (31 shown, 9 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 47 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `057a6188`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ChronoViewModel
- Store
- Tabela "Onde está cada item do MVP"
- Punch
- OutboxDao
- ChronoPass — Manual Operacional
- ChronoRepository
- ChronoPass — Guia Técnico (codebase)
- PunchScreen.kt
- FakePunchDao
- UpdateChecker
- BackupManager
- Fotos de ponto fora do banco: disco da VM + exportação ZIP + exclusão
- .parse
- TimeUtil
- Compressão de fotos: ChronoPass → Summus
- PdfExport
- SummusClient
- SyncOutcome
- ChronoDatabase
- gradlew
- OutboxPayloads
- ic_launcher_foreground.png
- Consolidar funcionário duplicado entre Summus e ChronoPass
- Employee
- PunchType
- Boi do Forte Logo (Center Carnes)
- PunchRulesTest
- PullPayloadsTest
- OutboxPayloadsTest
- SummusPayloadsTest.kt
- RecordsScreen.kt
- Period
- CsvExport.kt
- AGENTS.md
- targetSize
- CLAUDE.md

## God Nodes (most connected - your core abstractions)
1. `Employee` - 64 edges
2. `Punch` - 61 edges
3. `ChronoRepository` - 51 edges
4. `ChronoViewModel` - 39 edges
5. `FakePunchDao` - 25 edges
6. `Store` - 24 edges
7. `FakeEmployeeDao` - 24 edges
8. `PunchType` - 22 edges
9. `FakeOutboxDao` - 21 edges
10. `Tabela "Onde está cada item do MVP"` - 19 edges

## Surprising Connections (you probably didn't know these)
- `Configuração do Ícone do App` --conceptually_related_to--> `ic_launcher_foreground.png (Ícone do App)`  [INFERRED]
  app/src/main/assets/LEIA-ME.txt → README.md
- `Configuração da Logo (logo.png)` --conceptually_related_to--> `logo.png (Logo da Loja)`  [INFERRED]
  app/src/main/assets/LEIA-ME.txt → README.md
- `App()` --calls--> `AdminScreen()`  [INFERRED]
  app/src/main/java/com/chronopass/app/MainActivity.kt → app/src/main/java/com/chronopass/app/ui/screens/AdminScreen.kt
- `App()` --calls--> `EmployeesScreen()`  [INFERRED]
  app/src/main/java/com/chronopass/app/MainActivity.kt → app/src/main/java/com/chronopass/app/ui/screens/EmployeesScreen.kt
- `App()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/chronopass/app/MainActivity.kt → app/src/main/java/com/chronopass/app/ui/screens/HomeScreen.kt

## Import Cycles
- None detected.

## Communities (40 total, 9 thin omitted)

### Community 0 - "ChronoViewModel"
Cohesion: 0.09
Nodes (12): AndroidViewModel, ChronoViewModel, Bitmap, Context, LogoAsset, rememberLogo(), AdminScreen(), NavController (+4 more)

### Community 1 - "Store"
Cohesion: 0.12
Nodes (7): StoreDao, Store, JSONObject, PhotoPayload, SummusPayloads, SummusContractTest, TimeZone

### Community 2 - "Tabela "Onde está cada item do MVP""
Cohesion: 0.08
Nodes (28): LEIA-ME.txt (Guia de Imagens), Configuração do Ícone do App, ic_launcher_background.xml (Cor de Fundo do Ícone), Configuração da Logo (logo.png), Scripts de Rebuild (dev.bat/apk.bat/gradlew assembleRelease), BackupManager.kt, CameraCapture.kt, PhotoStore.kt (+20 more)

### Community 3 - "Punch"
Cohesion: 0.16
Nodes (3): PunchDao, Punch, UidTest

### Community 4 - "OutboxDao"
Cohesion: 0.14
Nodes (3): OutboxDao, OutboxItem, SyncRulesTest

### Community 5 - "ChronoPass — Manual Operacional"
Cohesion: 0.11
Nodes (18): 10. Resumo em uma frase, 1. O que é o ChronoPass, 2. Instalação e primeira configuração, 3. Dia a dia: bater o ponto, 4. Área do gerente, 5. Relatórios, 6. Onde ficam as fotos e os dados, 7. Backup: exportar e restaurar (+10 more)

### Community 6 - "ChronoRepository"
Cohesion: 0.07
Nodes (4): SettingsDao, AppSetting, ChronoRepository, photoHashKey()

### Community 7 - "ChronoPass — Guia Técnico (codebase)"
Cohesion: 0.05
Nodes (37): 10. Ordem de implementação (modo Act), 1. Objetivo, 2. Configuração no app (feature em Configurações), 3. Identidade do aparelho, 4. Id externo das entidades — uid (UUID), 5. Lotes de envio, 6. Regras que evitam perda, 7. Fila de sincronização (`sync_outbox`) (+29 more)

### Community 8 - "PunchScreen.kt"
Cohesion: 0.10
Nodes (31): await(), CameraCapture(), Context, T, takePhoto(), ImageCapture, awaitOrNull(), distanceMeters() (+23 more)

### Community 9 - "FakePunchDao"
Cohesion: 0.08
Nodes (11): Pull, SummusEmployee, SummusPunchCorrection, SyncRules, ApplyFromSummusTest, FakeEmployeeDao, FakeOutboxDao, FakePunchDao (+3 more)

### Community 10 - "UpdateChecker"
Cohesion: 0.10
Nodes (17): App(), MainActivity, NavController, SettingsScreen(), ChronoTheme(), Context, UpdateAvailableDialog(), UpdateChecker (+9 more)

### Community 11 - "BackupManager"
Cohesion: 0.39
Nodes (3): BackupManager, Context, JSONObject

### Community 12 - "Fotos de ponto fora do banco: disco da VM + exportação ZIP + exclusão"
Cohesion: 0.08
Nodes (23): 1. `apps/server/internal/db/migrations/0009_rh_photos_disk.sql`, 1. Fotos novas vão direto para o disco — nem entram no banco, 2. `apps/server/internal/coldstorage/storage/local.go` — `Delete` e `fsync`, 2. "No disco consome menos espaço?" — pouco. O que libera espaço é exportar e excluir, 3. `apps/server/internal/modules/rh/photos.go` (novo) — gravação, leitura, migração, 3. O código fica no módulo `rh`, não no módulo `cold-storage`, 4. `apps/server/internal/modules/rh/photo_exports.go` (novo) — exportar e excluir, 4. Exportação por link assinado, com download nativo do navegador (+15 more)

### Community 13 - ".parse"
Cohesion: 0.27
Nodes (7): Falha, JSONObject, T, Ok, PullPayloads, PullResult, StorePull

### Community 14 - "TimeUtil"
Cohesion: 0.08
Nodes (12): ReportPeriod, CUSTOM, LAST_30, LAST_7, LAST_MONTH, THIS_MONTH, TimeUtil, NavController (+4 more)

### Community 15 - "Compressão de fotos: ChronoPass → Summus"
Cohesion: 0.10
Nodes (20): 1. `app/src/main/java/com/chronopass/app/camera/ImageScale.kt` — escala exata, 2. `app/src/main/java/com/chronopass/app/camera/PhotoCompressor.kt` — Bitmap em memória, 3. `app/src/main/java/com/chronopass/app/camera/CameraCapture.kt` — capturar pequeno, em memória, 4. `app/src/main/java/com/chronopass/app/camera/PhotoStore.kt`, 5. Fotos de batida: nada muda no servidor, 6. `apps/client/src/modules/rh/EmployeesTab.tsx` — redimensionar no navegador antes de subir, App — `d:\BUSINESS\ChronoPass`, branch `feat/integração-summus`, Compressão de fotos: ChronoPass → Summus (+12 more)

### Community 16 - "PdfExport"
Cohesion: 0.37
Nodes (5): android, Bitmap, PdfExport, Row, Paint

### Community 17 - "SummusClient"
Cohesion: 0.15
Nodes (12): Ack, Erro, GetResult, HttpError, Context, JSONObject, Ok, PostResult (+4 more)

### Community 18 - "SyncOutcome"
Cohesion: 0.17
Nodes (11): Context, PhotoStore, Descida, Falha, Inativo, JaRodando, Context, Ocioso (+3 more)

### Community 19 - "ChronoDatabase"
Cohesion: 0.19
Nodes (9): backfillUid(), ChronoDatabase, Converters, ids(), Context, novoUid(), sanearUid(), RoomDatabase (+1 more)

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "OutboxPayloads"
Cohesion: 0.31
Nodes (3): JSONObject, OutboxPayloads, PhotoRef

### Community 23 - "Consolidar funcionário duplicado entre Summus e ChronoPass"
Cohesion: 0.10
Nodes (19): 1. `apps/server/internal/modules/rh/pull.go` — emitir `mergeUids`, 2. `apps/server/internal/modules/rh/bridge.go` — soltar a guarda de duplicidade, 3. Opcional — `EmployeePhotoForStore` (mesmo arquivo, 1 linha), 4. `app/src/main/java/com/chronopass/app/sync/SyncRules.kt`, 5. `app/src/main/java/com/chronopass/app/sync/PullPayloads.kt`, 6. `app/src/main/java/com/chronopass/app/data/dao/Daos.kt`, 7. `app/src/main/java/com/chronopass/app/data/repo/ChronoRepository.kt`, 8. `app/src/main/java/com/chronopass/app/ui/screens/EmployeesScreen.kt` — barrar nome repetido no cadastro (+11 more)

### Community 24 - "Employee"
Cohesion: 0.17
Nodes (4): EmployeeDao, Flow, Employee, EmployeePickerDialog()

### Community 25 - "PunchType"
Cohesion: 0.19
Nodes (4): PunchType, IN, OUT, PunchRules

### Community 33 - "RecordsScreen.kt"
Cohesion: 0.70
Nodes (4): AddPunchDialog(), NavController, PunchDetailDialog(), RecordsScreen()

### Community 34 - "Period"
Cohesion: 0.40
Nodes (5): Period, ALL, TODAY, WEEK, YESTERDAY

### Community 43 - "targetSize"
Cohesion: 0.22
Nodes (4): targetSize(), Bitmap, PhotoCompressor, ImageScaleTest

## Knowledge Gaps
- **137 isolated node(s):** `IN`, `OUT`, `THIS_MONTH`, `LAST_MONTH`, `LAST_7` (+132 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Employee` connect `Employee` to `ChronoViewModel`, `Store`, `RecordsScreen.kt`, `CsvExport.kt`, `SummusPayloadsTest.kt`, `Punch`, `ChronoRepository`, `PunchScreen.kt`, `FakePunchDao`, `BackupManager`, `TimeUtil`, `OutboxPayloads`, `PunchType`, `OutboxPayloadsTest`?**
  _High betweenness centrality (0.144) - this node is a cross-community bridge._
- **Why does `ChronoViewModel` connect `ChronoViewModel` to `Store`, `RecordsScreen.kt`, `ChronoRepository`, `PunchScreen.kt`, `UpdateChecker`, `TimeUtil`, `Employee`, `PunchType`?**
  _High betweenness centrality (0.120) - this node is a cross-community bridge._
- **Why does `Punch` connect `Punch` to `ChronoViewModel`, `Store`, `RecordsScreen.kt`, `CsvExport.kt`, `SummusPayloadsTest.kt`, `ChronoRepository`, `PunchScreen.kt`, `FakePunchDao`, `BackupManager`, `TimeUtil`, `PdfExport`, `OutboxPayloads`, `PunchType`, `PunchRulesTest`, `OutboxPayloadsTest`?**
  _High betweenness centrality (0.117) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `ChronoRepository` (e.g. with `.applyFromSummus_naoEnfileiraNaOutbox()` and `.applyPullAvancaCursorEMarcaLojaGerida()`) actually correct?**
  _`ChronoRepository` has 11 INFERRED edges - model-reasoned connections that need verification._
- **What connects `IN`, `OUT`, `THIS_MONTH` to the rest of the system?**
  _137 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ChronoViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.09090909090909091 - nodes in this community are weakly interconnected._
- **Should `Store` be split into smaller, more focused modules?**
  _Cohesion score 0.11596638655462185 - nodes in this community are weakly interconnected._