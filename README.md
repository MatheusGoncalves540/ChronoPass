# ChronoPass

Livro de ponto digital **offline** com evidência fotográfica e geográfica.

Android · Kotlin · Jetpack Compose (Material 3) · Room · CameraX · Fused Location · Coil.
Sem servidor, sem IA, sem dependência de internet para o uso diário.

> O app responde a quatro perguntas a cada marcação: **quem registrou, quando,
> onde estava e qual foto foi tirada naquele momento.** Não é um sistema
> biométrico — é um livro de ponto com evidência.

| | |
|---|---|
| Versão | 2.2.0 (versionCode 4) |
| SDK | minSdk 26 (Android 8.0) · targetSdk 35 · compileSdk 35 |
| Build | Kotlin 1.x · JDK 17 · Gradle wrapper |
| Stack | Jetpack Compose (BOM 2024.12.01) · Room 2.6.1 · CameraX 1.4.1 · play-services-location 21.3.0 · Coil 2.7.0 |
| Estado | 100% offline, dados no armazenamento privado do aparelho |

---

## Funcionalidades

### Registro de ponto
- Seleção de funcionário na tela inicial (terminal de ponto).
- A próxima marcação é **Entrada** ou **Saída**, alternada pela última marcação do dia (sem marcação → Entrada).
- Fluxo: escolher funcionário → tirar foto → obter localização → confirmar → salvar.
- Foto da marcação via CameraX (evidência visual), guardada no armazenamento privado — nunca na galeria.
- Localização GPS com latitude, longitude e precisão (Fused Location Provider).
- Loja cadastrada com raio permitido; o app informa se você está dentro ou a quantos metros está.

### Regras de negócio (testadas em JVM)
- **Horas trabalhadas** = soma dos intervalos Entrada→Saída. Marcação sem par (entrada pendente ou saída solta) é ignorada, não descarta o resto do dia.
- **Almoço** = soma dos intervalos Saída→Entrada do dia.
- **Intervalo mínimo CLT**: 1h para jornada > 6h, 30 min para 4–6h. Abaixo disso, o dia ganha um `*` no PDF e um aviso no rodapé.
- Correções manuais de marcações são normalizadas para nunca casar pares errados.

### Área administrativa (senha)
- Acesso por senha na tela inicial (padrão `1234` — troque em **Configurações**).
- **Funcionários**: cadastrar, editar, adicionar foto de cadastro, desativar, excluir (vai para a lixeira; as marcações do histórico são mantidas).
- **Marcações**: listar e filtrar por funcionário/período, visualizar foto e localização, **corrigir com motivo obrigatório** (registra quem alterou, quando e por quê) e excluir.
- **Configurações**: senha do administrador, localização da loja e raio permitido.

### Relatórios (por funcionário + período)
- **CSV** — linha por marcação: `Funcionario,Data,Tipo,Horario,Latitude,Longitude,Precisao,Almoco,Almoco Insuficiente,Motivo da Alteracao` (pronto para Excel/pivotar).
- **PDF** (espelho de ponto) — gerado com `PdfDocument` do SDK, zero dependência:
  - Cabeçalho com **logo da loja grande e centralizada**, **foto do colaborador** (canto superior direito, com center-crop para nunca distorcer) e dados do funcionário/período;
  - Tabela `Data | Entrada | Saída | Almoço | Horas` agregada por dia, com total;
  - Seções de alterações (motivo) e aviso de almoço abaixo do mínimo CLT;
  - Blocos de assinatura (funcionário/gerente);
  - Paginação automática com "Página X de Y".
- Períodos: este mês, mês passado, 7 dias, 30 dias ou datas personalizadas.

### Backup e restauração
- **Exportar backup**: um `.zip` com `data.json` (funcionários, marcações, loja e configurações) + todas as fotos, salvo onde você escolher (SAF).
- **Restaurar backup**: apaga os dados atuais e recarrega tudo do zip.
- Sem backup, celular quebrado = histórico perdido — faça backups periódicos.

### Atualização automática
- Na abertura, o app consulta o **GitHub Releases** (`MatheusGoncalves540/ChronoPass`) e oferece atualização quando existe versão nova.
- Baixa o APK para o armazenamento privado com barra de progresso e instala via FileProvider (pede a permissão de "instalar apps de fora da Play Store" quando necessário).
- Download em `.part` + rename: nunca instala um arquivo pela metade.

---

## Estrutura do projeto

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

## Onde está cada item

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

---

## Build

Não há SDK Android neste ambiente — abra em **Android Studio** (Giraffe+ / JDK 17):

```
Open project  →  d:/BUSINESS/ChronoPass
```

O Studio gera o `gradle-wrapper.jar`. Ou, com Gradle instalado: `gradle wrapper && ./gradlew assembleDebug`.

### Scripts (`scripts/`)

| Script | O que faz |
|---|---|
| `dev.bat` | Build + instala no emulador `chrono` (inicia se preciso) e abre o app |
| `apk.bat` | Gera o APK release em `app/build/outputs/apk/release/app-release.apk` |
| `test.bat` | Roda os testes de lógica (não precisa de emulador) |
| `release.bat` | Roda testes, builda o APK release, faz o bump de versão (tag vX.Y.Z) e publica no GitHub Releases (branch `production`) |

### Testes

```
./gradlew test
```

Testes de lógica pura na JVM (sem emulador):

| Teste | Cobre |
|---|---|
| `PunchRulesTest` | Alternância entrada/saída, soma de horas (incluindo turno que vira a meia-noite e dia sem par), almoço e mínimo CLT |
| `ReportPeriodTest` | Intervalos de "este mês", "mês passado", 7/30 dias |
| `UpdateCheckerTest` | Comparação de versões do auto-update |

### Chave de release

A assinatura de release usa `keystore/keystore.properties` + `keystore/chronopass-release.jks`
(ambos gitignorados). Num clone novo sem esses arquivos, o build continua funcionando, mas
cai para a chave de **debug** e avisa.

---

## Imagens trocáveis (build-time)

| Imagem | Arquivo | Onde aparece |
|---|---|---|
| Logo da loja | `app/src/main/assets/logo.png` | Topo da tela inicial e **cabeçalho do PDF** (grande, centralizada) |
| Ícone do app | `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png` | Ícone na área de trabalho do Android |

Troque o arquivo e recompile (`scripts/dev.bat` / `scripts/apk.bat`). Instruções detalhadas
(formato, margens) em `app/src/main/assets/LEIA-ME.txt`.

---

## Permissões

| Permissão | Para quê |
|---|---|
| `CAMERA` | Foto da marcação e foto de cadastro do funcionário |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Registrar lat/lon/precisão no ponto |
| `INTERNET` | Somente o auto-update (GitHub Releases) |
| `REQUEST_INSTALL_PACKAGES` | Instalar o APK baixado na atualização |

Fotos e dados ficam no armazenamento privado do app (`android:allowBackup="false"`), nunca na galeria.

---

## Fora de escopo (ver `docs/PLANO-FUTURO.md`)

Reconhecimento facial, servidor, sincronização online, múltiplas lojas, folha de pagamento,
banco de horas complexo, AFD/Portaria 671 e notificações — deliberadamente não implementados.
O modelo de dados atual não impede adicioná-los depois.

Documentos: `docs/PLANO.md` (especificação original e checklist do MVP) e
`docs/PLANO-FUTURO.md` (visão com reconhecimento facial, criptografia e auditoria).
