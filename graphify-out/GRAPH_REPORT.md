# Graph Report - ChronoPass  (2026-09-05)

## Corpus Check
- 55 files · ~35,074 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 621 nodes · 1223 edges · 28 communities (23 shown, 5 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 43 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a28fc860`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ChronoViewModel
- Employee
- Tabela "Onde está cada item do MVP"
- Punch
- OutboxDao
- ChronoPass — Manual Operacional
- ChronoRepository
- ChronoPass — Guia Técnico (codebase)
- PunchScreen.kt
- FakeEmployeeDao
- UpdateChecker
- BackupManager
- .parse
- TimeUtil
- ReportsScreen.kt
- SummusClient
- ChronoDatabase
- gradlew
- OutboxPayloads
- ic_launcher_foreground.png
- SummusContractTest
- Boi do Forte Logo (Center Carnes)
- AGENTS.md
- inSampleSize
- CLAUDE.md

## God Nodes (most connected - your core abstractions)
1. `Punch` - 61 edges
2. `Employee` - 60 edges
3. `ChronoRepository` - 47 edges
4. `ChronoViewModel` - 39 edges
5. `Store` - 24 edges
6. `PunchType` - 22 edges
7. `FakeEmployeeDao` - 21 edges
8. `FakePunchDao` - 21 edges
9. `Tabela "Onde está cada item do MVP"` - 19 edges
10. `TimeUtil` - 18 edges

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

## Communities (28 total, 5 thin omitted)

### Community 0 - "ChronoViewModel"
Cohesion: 0.09
Nodes (12): AndroidViewModel, ChronoViewModel, Bitmap, Context, LogoAsset, rememberLogo(), AdminScreen(), NavController (+4 more)

### Community 1 - "Employee"
Cohesion: 0.09
Nodes (10): EmployeeDao, Flow, StoreDao, Employee, Store, JSONObject, PhotoPayload, SummusPayloads (+2 more)

### Community 2 - "Tabela "Onde está cada item do MVP""
Cohesion: 0.08
Nodes (28): LEIA-ME.txt (Guia de Imagens), Configuração do Ícone do App, ic_launcher_background.xml (Cor de Fundo do Ícone), Configuração da Logo (logo.png), Scripts de Rebuild (dev.bat/apk.bat/gradlew assembleRelease), BackupManager.kt, CameraCapture.kt, PhotoStore.kt (+20 more)

### Community 3 - "Punch"
Cohesion: 0.05
Nodes (11): PunchDao, Punch, PunchType, IN, OUT, PunchRules, CsvExport, PunchRulesTest (+3 more)

### Community 4 - "OutboxDao"
Cohesion: 0.13
Nodes (3): OutboxDao, OutboxItem, SyncRulesTest

### Community 5 - "ChronoPass — Manual Operacional"
Cohesion: 0.11
Nodes (18): 10. Resumo em uma frase, 1. O que é o ChronoPass, 2. Instalação e primeira configuração, 3. Dia a dia: bater o ponto, 4. Área do gerente, 5. Relatórios, 6. Onde ficam as fotos e os dados, 7. Backup: exportar e restaurar (+10 more)

### Community 6 - "ChronoRepository"
Cohesion: 0.07
Nodes (13): Context, PhotoStore, ChronoRepository, photoHashKey(), Descida, Falha, Inativo, JaRodando (+5 more)

### Community 7 - "ChronoPass — Guia Técnico (codebase)"
Cohesion: 0.05
Nodes (37): 10. Ordem de implementação (modo Act), 1. Objetivo, 2. Configuração no app (feature em Configurações), 3. Identidade do aparelho, 4. Id externo das entidades — uid (UUID), 5. Lotes de envio, 6. Regras que evitam perda, 7. Fila de sincronização (`sync_outbox`) (+29 more)

### Community 8 - "PunchScreen.kt"
Cohesion: 0.10
Nodes (30): await(), CameraCapture(), Context, T, takePhoto(), ImageCapture, awaitOrNull(), distanceMeters() (+22 more)

### Community 9 - "FakeEmployeeDao"
Cohesion: 0.06
Nodes (13): SettingsDao, AppSetting, Pull, SummusEmployee, SummusPunchCorrection, SyncRules, ApplyFromSummusTest, FakeEmployeeDao (+5 more)

### Community 10 - "UpdateChecker"
Cohesion: 0.10
Nodes (17): App(), MainActivity, NavController, SettingsScreen(), ChronoTheme(), Context, UpdateAvailableDialog(), UpdateChecker (+9 more)

### Community 11 - "BackupManager"
Cohesion: 0.39
Nodes (3): BackupManager, Context, JSONObject

### Community 13 - ".parse"
Cohesion: 0.27
Nodes (7): Falha, JSONObject, T, Ok, PullPayloads, PullResult, StorePull

### Community 14 - "TimeUtil"
Cohesion: 0.09
Nodes (8): ReportPeriod, CUSTOM, LAST_30, LAST_7, LAST_MONTH, THIS_MONTH, TimeUtil, ReportPeriodTest

### Community 16 - "ReportsScreen.kt"
Cohesion: 0.13
Nodes (19): android, Bitmap, PdfExport, Row, EmployeePickerDialog(), AddPunchDialog(), NavController, Period (+11 more)

### Community 17 - "SummusClient"
Cohesion: 0.15
Nodes (12): Ack, Erro, GetResult, HttpError, Context, JSONObject, Ok, PostResult (+4 more)

### Community 19 - "ChronoDatabase"
Cohesion: 0.19
Nodes (9): backfillUid(), ChronoDatabase, Converters, ids(), Context, novoUid(), sanearUid(), RoomDatabase (+1 more)

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "OutboxPayloads"
Cohesion: 0.31
Nodes (3): JSONObject, OutboxPayloads, PhotoRef

### Community 43 - "inSampleSize"
Cohesion: 0.24
Nodes (3): inSampleSize(), PhotoCompressor, ImageScaleTest

## Knowledge Gaps
- **89 isolated node(s):** `IN`, `OUT`, `THIS_MONTH`, `LAST_MONTH`, `LAST_7` (+84 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Employee` connect `Employee` to `ChronoViewModel`, `Punch`, `OutboxDao`, `ChronoRepository`, `PunchScreen.kt`, `FakeEmployeeDao`, `BackupManager`, `ReportsScreen.kt`, `OutboxPayloads`, `SummusContractTest`?**
  _High betweenness centrality (0.170) - this node is a cross-community bridge._
- **Why does `ChronoViewModel` connect `ChronoViewModel` to `Employee`, `Punch`, `ChronoRepository`, `PunchScreen.kt`, `UpdateChecker`, `ReportsScreen.kt`?**
  _High betweenness centrality (0.149) - this node is a cross-community bridge._
- **Why does `Punch` connect `Punch` to `ChronoViewModel`, `Employee`, `OutboxDao`, `ChronoRepository`, `PunchScreen.kt`, `FakeEmployeeDao`, `BackupManager`, `ReportsScreen.kt`, `OutboxPayloads`, `SummusContractTest`?**
  _High betweenness centrality (0.146) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `ChronoRepository` (e.g. with `.applyFromSummus_naoEnfileiraNaOutbox()` and `.applyPullAvancaCursorEMarcaLojaGerida()`) actually correct?**
  _`ChronoRepository` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `IN`, `OUT`, `THIS_MONTH` to the rest of the system?**
  _89 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ChronoViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.09090909090909091 - nodes in this community are weakly interconnected._
- **Should `Employee` be split into smaller, more focused modules?**
  _Cohesion score 0.08695652173913043 - nodes in this community are weakly interconnected._