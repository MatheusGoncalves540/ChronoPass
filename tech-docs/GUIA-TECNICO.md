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

Aplicativo Android nativo (Kotlin + Jetpack Compose, Material 3), **100% offline**,
sem servidor e sem dependência de internet para o uso diário. Arquitetura simples em
pacotes por responsabilidade, com estado compartilhado em um `ViewModel` único
(`ui/ChronoViewModel.kt`). Persistência local com Room/SQLite; fotos no armazenamento
privado do app (`android:allowBackup="false"`), nunca na galeria.

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
| Loja + raio | `data/entities` (Store), `ui/screens/SettingsScreen.kt` |
| Banco Room | `data/database`, `data/dao`, `data/repo` |
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

## 12. Documentos relacionados

| Documento | Natureza |
|---|---|
| `../docs/PLANO.md` | Especificação original e checklist do MVP (versão 1) |
| `../docs/PLANO-FUTURO.md` | Visão futura: reconhecimento facial, criptografia, auditoria |
| `../SUMUS-INTEGRACAO.md` | Contrato de sincronização com o SummusBackoffice — **nada implementado ainda** (schema de payloads, fila `sync_outbox`, ordem de implementação) |
| `../MANUAL-OPERACIONAL.md` | Manual do usuário final (sem linguagem técnica) |

## 13. Fora de escopo (ver `../docs/PLANO-FUTURO.md`)

Reconhecimento facial, servidor, sincronização online, múltiplas lojas, folha de pagamento,
banco de horas complexo, AFD/Portaria 671 e notificações — deliberadamente não implementados.
O modelo de dados atual não impede adicioná-los depois.
