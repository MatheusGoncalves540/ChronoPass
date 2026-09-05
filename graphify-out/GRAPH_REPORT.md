# Graph Report - ChronoPass  (2026-09-04)

## Corpus Check
- 47 files · ~29,067 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 490 nodes · 872 edges · 33 communities (24 shown, 9 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 24 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d2a161a3`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ChronoViewModel
- Store
- Tabela "Onde está cada item do MVP"
- PunchType
- ChronoPass — App Android simples de registro de ponto
- ChronoPass — App Android de ponto por reconhecimento facial
- ChronoRepository
- Employee
- PunchScreen
- TimeUtil
- UpdateChecker
- BackupManager
- rememberLogo
- Modifier
- ReportPeriod
- ChronoPass → SummusBackoffice — Plano de integração
- PdfExport
- SummusClient
- Punch
- ChronoDatabase
- gradlew
- OutboxPayloads
- ic_launcher_foreground.png
- PunchRulesTest
- RecordsScreen.kt
- OutboxPayloadsTest
- Boi do Forte Logo (Center Carnes)
- AGENTS.md
- PhotoCompressor
- CLAUDE.md

## God Nodes (most connected - your core abstractions)
1. `Punch` - 53 edges
2. `Employee` - 44 edges
3. `ChronoViewModel` - 38 edges
4. `ChronoRepository` - 32 edges
5. `ChronoPass — App Android simples de registro de ponto` - 21 edges
6. `Tabela "Onde está cada item do MVP"` - 19 edges
7. `TimeUtil` - 18 edges
8. `PunchType` - 17 edges
9. `Store` - 16 edges
10. `UpdateChecker` - 14 edges

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

## Communities (33 total, 9 thin omitted)

### Community 0 - "ChronoViewModel"
Cohesion: 0.08
Nodes (17): AndroidViewModel, Falha, Inativo, JaRodando, Context, Ocioso, Ok, SyncManager (+9 more)

### Community 1 - "Store"
Cohesion: 0.20
Nodes (6): StoreDao, Store, JSONObject, PhotoPayload, SummusPayloads, TimeZone

### Community 2 - "Tabela "Onde está cada item do MVP""
Cohesion: 0.08
Nodes (28): LEIA-ME.txt (Guia de Imagens), Configuração do Ícone do App, ic_launcher_background.xml (Cor de Fundo do Ícone), Configuração da Logo (logo.png), Scripts de Rebuild (dev.bat/apk.bat/gradlew assembleRelease), BackupManager.kt, CameraCapture.kt, PhotoStore.kt (+20 more)

### Community 3 - "PunchType"
Cohesion: 0.16
Nodes (5): PunchType, IN, OUT, PunchRules, CsvExport

### Community 4 - "ChronoPass — App Android simples de registro de ponto"
Cohesion: 0.05
Nodes (39): 10. Área administrativa, 11. Correção de marcações, 12. Relatórios, 13. Exportação, 14. Backup, 15. Tecnologia, 16. Estrutura do projeto, 17. Fora de escopo (+31 more)

### Community 5 - "ChronoPass — App Android de ponto por reconhecimento facial"
Cohesion: 0.14
Nodes (13): ChronoPass — App Android de ponto por reconhecimento facial, Contexto, Criptografia, Fases de implementação, Fora de escopo (por enquanto), Interfaces que crescem sem limite, Modelo de dados, Módulos Gradle (+5 more)

### Community 7 - "Employee"
Cohesion: 0.16
Nodes (5): EmployeeDao, Employee, EmployeePickerDialog(), UidTest, Flow

### Community 8 - "PunchScreen"
Cohesion: 0.20
Nodes (17): awaitOrNull(), distanceMeters(), Fix, freshLocation(), getCurrentFix(), Context, T, lastLocation() (+9 more)

### Community 10 - "UpdateChecker"
Cohesion: 0.10
Nodes (17): App(), MainActivity, NavController, SettingsScreen(), ChronoTheme(), Context, UpdateAvailableDialog(), UpdateChecker (+9 more)

### Community 11 - "BackupManager"
Cohesion: 0.22
Nodes (5): BackupManager, Context, JSONObject, Context, PhotoStore

### Community 12 - "rememberLogo"
Cohesion: 0.36
Nodes (7): Bitmap, Context, LogoAsset, rememberLogo(), HomeScreen(), NavController, ImageBitmap

### Community 13 - "Modifier"
Cohesion: 0.21
Nodes (13): await(), CameraCapture(), Context, T, takePhoto(), ImageCapture, CameraDialog(), EmployeeDialog() (+5 more)

### Community 14 - "ReportPeriod"
Cohesion: 0.16
Nodes (7): ReportPeriod, CUSTOM, LAST_30, LAST_7, LAST_MONTH, THIS_MONTH, ReportPeriodTest

### Community 15 - "ChronoPass → SummusBackoffice — Plano de integração"
Cohesion: 0.14
Nodes (13): 10. Ordem de implementação (modo Act), 1. Objetivo, 2. Configuração no app (feature em Configurações), 3. Identidade do aparelho, 4. Id externo das entidades — uid (UUID), 5. Lotes de envio, 6. Regras que evitam perda, 7. Fila de sincronização (`sync_outbox`) (+5 more)

### Community 16 - "PdfExport"
Cohesion: 0.37
Nodes (5): android, Bitmap, PdfExport, Row, Paint

### Community 17 - "SummusClient"
Cohesion: 0.24
Nodes (7): Ack, HttpError, Context, JSONObject, PostResult, SummusClient, TransportError

### Community 18 - "Punch"
Cohesion: 0.17
Nodes (3): PunchDao, Punch, SummusPayloadsTest

### Community 19 - "ChronoDatabase"
Cohesion: 0.07
Nodes (10): OutboxDao, SettingsDao, backfillUid(), ChronoDatabase, Converters, Context, AppSetting, OutboxItem (+2 more)

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "OutboxPayloads"
Cohesion: 0.31
Nodes (3): JSONObject, OutboxPayloads, PhotoRef

### Community 24 - "RecordsScreen.kt"
Cohesion: 0.27
Nodes (9): AddPunchDialog(), NavController, Period, ALL, TODAY, WEEK, YESTERDAY, PunchDetailDialog() (+1 more)

## Knowledge Gaps
- **100 isolated node(s):** `IN`, `OUT`, `THIS_MONTH`, `LAST_MONTH`, `LAST_7` (+95 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ChronoViewModel` connect `ChronoViewModel` to `Store`, `PunchType`, `ChronoRepository`, `Employee`, `PunchScreen`, `UpdateChecker`, `rememberLogo`, `Modifier`, `RecordsScreen.kt`?**
  _High betweenness centrality (0.168) - this node is a cross-community bridge._
- **Why does `Punch` connect `Punch` to `ChronoViewModel`, `Store`, `PunchType`, `ChronoRepository`, `Employee`, `PunchScreen`, `BackupManager`, `PdfExport`, `ChronoDatabase`, `OutboxPayloads`, `PunchRulesTest`, `RecordsScreen.kt`, `OutboxPayloadsTest`?**
  _High betweenness centrality (0.162) - this node is a cross-community bridge._
- **Why does `Employee` connect `Employee` to `ChronoViewModel`, `Store`, `PunchType`, `ChronoRepository`, `PunchScreen`, `BackupManager`, `Modifier`, `Punch`, `ChronoDatabase`, `OutboxPayloads`, `RecordsScreen.kt`, `OutboxPayloadsTest`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **What connects `IN`, `OUT`, `THIS_MONTH` to the rest of the system?**
  _100 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ChronoViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.07823613086770982 - nodes in this community are weakly interconnected._
- **Should `Tabela "Onde está cada item do MVP"` be split into smaller, more focused modules?**
  _Cohesion score 0.082010582010582 - nodes in this community are weakly interconnected._
- **Should `ChronoPass — App Android simples de registro de ponto` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._