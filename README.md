# ChronoPass

Livro de ponto digital offline com evidência fotográfica e geográfica.
Android · Kotlin · Jetpack Compose · Room · CameraX · Fused Location. Sem servidor, sem IA.

## Build

Não há SDK Android neste ambiente — abra em **Android Studio** (Giraffe+ / JDK 17):

```
Open project  →  d:/BUSINESS/ChronoPass
```

O Studio gera o `gradle-wrapper.jar`. Ou, com Gradle instalado: `gradle wrapper && ./gradlew assembleDebug`.

- `minSdk 26`, `targetSdk 35`.
- Testes de lógica pura: `./gradlew test` (regra entrada/saída + soma de horas).

## Onde está cada item do MVP

| Requisito | Arquivo |
|---|---|
| Selecionar funcionário / próxima marcação | `ui/screens/HomeScreen.kt`, `PunchScreen.kt` |
| Regra entrada↔saída | `data/PunchRules.kt` |
| Câmera + foto privada | `camera/CameraCapture.kt`, `camera/PhotoStore.kt` |
| Localização (lat/lon/precisão) | `location/LocationHelper.kt` |
| Loja + raio | `data/entities` (Store), `SettingsScreen.kt` |
| Banco Room | `data/database`, `data/dao`, `data/repo` |
| Área admin (senha) | `ui/screens/AdminScreen.kt` (padrão `1234`, troque em Configurações) |
| Funcionários CRUD | `EmployeesScreen.kt` |
| Marcações + correção + excluir | `RecordsScreen.kt` |
| CSV / PDF | `reports/CsvExport.kt`, `reports/PdfExport.kt` |
| Backup / Restaurar (zip) | `backup/BackupManager.kt`, `ReportsScreen.kt` |

## Fora de escopo (ver PLANO-FUTURO.md)

Reconhecimento facial, servidor, sincronização, múltiplas lojas, folha de ponto — deliberadamente não implementados.
