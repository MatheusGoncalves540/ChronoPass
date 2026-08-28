# Graph Report - ChronoPass  (2026-08-28)

## Corpus Check
- 39 files · ~23,518 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 386 nodes · 555 edges · 28 communities (19 shown, 9 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 41 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `11a517b5`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ChronoViewModel
- Store
- Tabela "Onde está cada item do MVP"
- Punch
- ChronoPass — App Android simples de registro de ponto
- ChronoPass — App Android de ponto por reconhecimento facial
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
- gradlew
- ic_launcher_foreground.png
- Boi do Forte Logo (Center Carnes)
- AGENTS.md
- PhotoCompressor
- CLAUDE.md
- UpdateCheckerTest

## God Nodes (most connected - your core abstractions)
1. `Punch` - 29 edges
2. `ChronoRepository` - 26 edges
3. `ChronoViewModel` - 25 edges
4. `ChronoPass — App Android simples de registro de ponto` - 21 edges
5. `Employee` - 20 edges
6. `Tabela "Onde está cada item do MVP"` - 19 edges
7. `TimeUtil` - 14 edges
8. `ChronoPass — App Android de ponto por reconhecimento facial` - 13 edges
9. `EmployeeDao` - 12 edges
10. `PunchDao` - 12 edges

## Surprising Connections (you probably didn't know these)
- `Configuração da Logo (logo.png)` --conceptually_related_to--> `logo.png (Logo da Loja)`  [INFERRED]
  app/src/main/assets/LEIA-ME.txt → README.md
- `Configuração do Ícone do App` --conceptually_related_to--> `ic_launcher_foreground.png (Ícone do App)`  [INFERRED]
  app/src/main/assets/LEIA-ME.txt → README.md
- `App()` --calls--> `EmployeesScreen()`  [INFERRED]
  app/src/main/java/com/chronopass/app/MainActivity.kt → app/src/main/java/com/chronopass/app/ui/screens/EmployeesScreen.kt
- `App()` --calls--> `PunchScreen()`  [INFERRED]
  app/src/main/java/com/chronopass/app/MainActivity.kt → app/src/main/java/com/chronopass/app/ui/screens/PunchScreen.kt
- `App()` --calls--> `RecordsScreen()`  [INFERRED]
  app/src/main/java/com/chronopass/app/MainActivity.kt → app/src/main/java/com/chronopass/app/ui/screens/RecordsScreen.kt

## Import Cycles
- None detected.

## Communities (28 total, 9 thin omitted)

### Community 0 - "ChronoViewModel"
Cohesion: 0.07
Nodes (18): AndroidViewModel, App(), MainActivity, ChronoViewModel, AdminScreen(), NavController, HomeScreen(), NavController (+10 more)

### Community 1 - "Store"
Cohesion: 0.07
Nodes (14): SettingsDao, StoreDao, ChronoDatabase, Converters, get(), Context, migrate(), AppSetting (+6 more)

### Community 2 - "Tabela "Onde está cada item do MVP""
Cohesion: 0.08
Nodes (28): LEIA-ME.txt (Guia de Imagens), Configuração do Ícone do App, ic_launcher_background.xml (Cor de Fundo do Ícone), Configuração da Logo (logo.png), Scripts de Rebuild (dev.bat/apk.bat/gradlew assembleRelease), BackupManager.kt, CameraCapture.kt, PhotoStore.kt (+20 more)

### Community 3 - "Punch"
Cohesion: 0.09
Nodes (5): PunchDao, Punch, PunchRules, CsvExport, PunchRulesTest

### Community 4 - "ChronoPass — App Android simples de registro de ponto"
Cohesion: 0.05
Nodes (39): 10. Área administrativa, 11. Correção de marcações, 12. Relatórios, 13. Exportação, 14. Backup, 15. Tecnologia, 16. Estrutura do projeto, 17. Fora de escopo (+31 more)

### Community 5 - "ChronoPass — App Android de ponto por reconhecimento facial"
Cohesion: 0.14
Nodes (13): ChronoPass — App Android de ponto por reconhecimento facial, Contexto, Criptografia, Fases de implementação, Fora de escopo (por enquanto), Interfaces que crescem sem limite, Modelo de dados, Módulos Gradle (+5 more)

### Community 7 - "ChronoRepository"
Cohesion: 0.06
Nodes (4): EmployeeDao, Employee, ChronoRepository, Flow

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
Cohesion: 0.38
Nodes (5): Bitmap, Context, LogoAsset, rememberLogo(), ImageBitmap

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

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **86 isolated node(s):** `IN`, `OUT`, `THIS_MONTH`, `LAST_MONTH`, `LAST_7` (+81 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Punch` connect `Punch` to `ChronoViewModel`, `Store`, `ChronoRepository`, `PunchScreen`, `.loadJson`, `PdfExport`, `RecordsScreen`?**
  _High betweenness centrality (0.154) - this node is a cross-community bridge._
- **Why does `ChronoViewModel` connect `ChronoViewModel` to `Store`, `ChronoRepository`, `PunchScreen`, `CameraCapture`, `RecordsScreen`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Why does `PunchScreen()` connect `PunchScreen` to `ChronoViewModel`, `PdfExport`, `Punch`, `CameraCapture`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Punch` (e.g. with `PunchScreen()` and `.handlesDuplicateAdjacentPunch_withoutLosingTheGap()`) actually correct?**
  _`Punch` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `IN`, `OUT`, `THIS_MONTH` to the rest of the system?**
  _86 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ChronoViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.07130124777183601 - nodes in this community are weakly interconnected._
- **Should `Store` be split into smaller, more focused modules?**
  _Cohesion score 0.06606606606606606 - nodes in this community are weakly interconnected._