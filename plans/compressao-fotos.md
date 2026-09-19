# Compressão de fotos: ChronoPass → Summus

## Context

Cada batida gera uma foto, que fica no aparelho e sobe para `rh_chrono_photos.bytes`
(BYTEA no Postgres do Summus). Com várias lojas, vários funcionários e várias batidas por
dia, o volume cresce sem parar. O objetivo é deixar cada foto **muito menor**, com perda
de qualidade aceitável, com a compressão acontecendo **no app** e o Summus só guardando o
que recebe.

### O que já existe (não é do zero)

A branch `feat/integração-summus` já comprime. O plano aperta o que existe, não troca a
arquitetura.

| Onde | Hoje |
|---|---|
| `camera/CameraCapture.kt` | `ImageCapture.Builder().build()` → **resolução cheia do sensor**, gravada como JPEG em disco (`OutputFileOptions`) |
| `camera/PhotoCompressor.kt` | Relê o JPEG do disco, decodifica com `inSampleSize`, grava **WebP lossy q80**, apaga o JPEG |
| `camera/ImageScale.kt` | `inSampleSize` = potência de 2 até o maior lado caber em `MAX_PHOTO_DIM = 1280` |
| Summus `IngestPhotos` | Decodifica o base64, confere magic bytes (`jpeg/png/webp`), teto de 5 MiB, grava como veio |
| Summus web (`EmployeesTab.tsx`) | Foto de cadastro do funcionário sobe **crua**, até 5 MB, sem redimensionar |

### Problemas encontrados no pipeline atual

1. **Tamanho de saída imprevisível.** `inSampleSize` só aceita potência de 2, então o
   "teto de 1280" na prática entrega entre **648 e 1280px** dependendo do sensor:
   4000px ÷ 4 = 1000; 2592px ÷ 4 = **648**; 2560px ÷ 2 = 1280. Não dá para calibrar o
   que não é constante.
2. **Captura desperdiçada.** A câmera tira a foto na resolução máxima (3–5 MB de JPEG),
   grava em disco, o app relê e joga fora 90% dos pixels. É o passo mais lento da batida.
3. **Foto provavelmente de lado.** Ao salvar em arquivo, o CameraX guarda a rotação no
   EXIF — a própria doc manda ler de `Exif.createFromFile(file).rotation`.
   `BitmapFactory.decodeFile` ignora EXIF, e `Bitmap.compress` descarta o EXIF. Resultado
   provável: o WebP sai com os pixels na orientação do sensor, que em celular em pé costuma
   estar 90° girado. **Confirmar no aparelho antes de mexer** (passo 1 da verificação).
4. **Foto de funcionário do Summus web entra crua** — até 5 MB por foto no banco, e o app
   baixa esse arquivo inteiro na descida (`SyncManager.baixarFotos`).

---

## Pesquisa: qual formato

| Formato | Tamanho vs JPEG | Encoder no Android (minSdk 26) | Exibe no backoffice web | Veredito |
|---|---|---|---|---|
| **WebP lossy** | ~25–35% menor | **Nativo** (`Bitmap.CompressFormat.WEBP_LOSSY`, API 30; `WEBP` antes) | Todos os navegadores | **Escolhido** |
| AVIF | ~50% menor; ~20–30% menor que WebP | **Nenhum.** `CompressFormat` só tem JPEG/PNG/WEBP. Exige lib nativa (libavif) | Chrome, Firefox, Safari 16+ | Descartado |
| HEIC | ~ AVIF | `androidx.heifwriter`, depende de encoder HEVC de hardware (API 28+) | Só Safari | Descartado |
| JPEG XL | bom | Nenhum | Chrome não exibe | Descartado |

**Por que não AVIF, que comprime mais:**

- Não existe encoder na plataforma — entraria uma lib nativa (vários MB de `.so` por ABI no APK).
- Encode **10–20× mais lento que WebP**, e isso roda **dentro do fluxo da batida**, num
  aparelho de loja que tende a ser barato.
- Android 8–11 não decodifica AVIF nativamente. O `PdfExport` usa `BitmapFactory` —
  o espelho de ponto quebraria nesses aparelhos (minSdk é 26).
- O servidor não tem saída para re-encodar depois: Go não tem encoder WebP/AVIF na stdlib
  e a imagem é `scratch` com `CGO_ENABLED=0`.

**O que realmente encolhe a foto é resolução e qualidade, não o codec.** Bytes escalam com
pixels: 1280×960 = 1,23 MP; 960×720 = 0,69 MP (**−44%**); 800×600 = 0,48 MP (**−61%**).
Para uma foto de evidência — um rosto na frente do aparelho — 960px no maior lado
identifica a pessoa com folga.

---

## Desenho

```
HOJE      sensor cheio → JPEG 3-5 MB em disco → relê → inSampleSize → WebP q80 (648-1280px, de lado?)

PROPOSTO  câmera já captura ~1280×960 → em memória (ImageProxy) → escala exata 960px
          + aplica rotationDegrees → WebP q70 → arquivo final
```

Nada muda no contrato nem no servidor para as fotos de batida: continua WebP, continua
`contentType: "image/webp"`, `sniffImageType` já aceita. APK antigo e APK novo convivem.

As constantes são **o botão de calibração** — ajustar depois de medir em aparelho real,
não no escuro.

---

## App — `d:\BUSINESS\ChronoPass`, branch `feat/integração-summus`

### 1. `app/src/main/java/com/chronopass/app/camera/ImageScale.kt` — escala exata

Troca `inSampleSize` (potência de 2) por uma função que devolve a dimensão exata, mantendo
proporção e **nunca ampliando**. Continua 100% JVM, testável em JUnit puro:

```kotlin
// ponytail: calibração — medir em aparelho real antes de mexer. 800 corta ~30% a mais.
const val MAX_PHOTO_DIM = 960

/** Maior lado vira [max], proporção mantida; imagem já menor volta intacta (nunca amplia). */
fun targetSize(width: Int, height: Int, max: Int = MAX_PHOTO_DIM): Pair<Int, Int> {
    val maior = maxOf(width, height)
    if (max <= 0 || maior <= max) return width to height
    val f = max.toDouble() / maior
    return maxOf(1, (width * f).roundToInt()) to maxOf(1, (height * f).roundToInt())
}
```

`inSampleSize` sai — o único chamador é o `PhotoCompressor`.

### 2. `app/src/main/java/com/chronopass/app/camera/PhotoCompressor.kt` — Bitmap em memória

Recebe o `Bitmap` e a rotação em vez de um arquivo. Escala e rotação numa matriz só, uma
alocação:

```kotlin
object PhotoCompressor {
    // ponytail: calibração — q70 é onde o WebP para de ganhar bytes visivelmente.
    private const val QUALITY = 70

    fun compress(src: Bitmap, rotationDegrees: Int, out: File): File {
        val (w, h) = targetSize(src.width, src.height)
        val m = Matrix().apply {
            postScale(w / src.width.toFloat(), h / src.height.toFloat())
            postRotate(rotationDegrees.toFloat())
        }
        val img = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        FileOutputStream(out).use { img.compress(format, QUALITY, it) }
        if (img !== src) img.recycle()
        src.recycle()
        return out
    }
}
```

`filter = true` é bilinear. Bilinear serrilha em reduções grandes (4000→960), mas como a
câmera passa a entregar ~1280px (item 3), a redução fica em ~1,33× e bilinear basta.
**Os itens 2 e 3 dependem um do outro por isso.**

O comentário `ponytail:` sobre AVIF que já existe no arquivo continua válido — manter.

### 3. `app/src/main/java/com/chronopass/app/camera/CameraCapture.kt` — capturar pequeno, em memória

A câmera já entrega perto do tamanho final. CameraX 1.4.1 (já instalado) tem
`ResolutionSelector` e `ImageProxy.toBitmap()`:

```kotlin
val imageCapture = remember {
    ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 960),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                )
                .build()
        )
        .build()
}
```

1280×960 (acima dos 960 finais) deixa folga para a redução ter qualidade.
`CLOSEST_HIGHER_THEN_LOWER` escolhe o tamanho suportado mais próximo **acima**, e só desce
se o sensor não tiver.

Em `takePhoto`, troca `OnImageSavedCallback` + arquivo por `OnImageCapturedCallback`:

```kotlin
imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
    override fun onCaptureSuccess(image: ImageProxy) {
        scope.launch(Dispatchers.IO) {
            val out = image.use {
                PhotoCompressor.compress(
                    it.toBitmap(), it.imageInfo.rotationDegrees,
                    PhotoStore.newPhotoFile(context, tag),
                )
            }
            withContext(Dispatchers.Main) { onPhoto(out) }
        }
    }
    override fun onError(exc: ImageCaptureException) { /* ponytail: retry via button */ }
})
```

Isso resolve de uma vez: sem JPEG de 3–5 MB em disco, sem decodificar duas vezes, e
**`rotationDegrees` aplicado nos pixels** (problema 3 do Context).

### 4. `app/src/main/java/com/chronopass/app/camera/PhotoStore.kt`

`newRawFile` (`.jpg`) vira `newPhotoFile` (`.webp`) — o arquivo agora já nasce final. O
comentário "CameraX only captures straight to JPEG" deixa de ser verdade: remover.
`saveEmployeePhoto` não muda (já copia um `.webp` capturado).

Chamadores de `CameraCapture` (`PunchScreen.kt`, `EmployeesScreen.kt`) **não mudam**: a
assinatura `onPhoto: (File) -> Unit` é a mesma.

---

## Summus — `d:\BUSINESS\SummusBackoffice`, branch `feat/modulo-rh`

### 5. Fotos de batida: nada muda no servidor

Deliberado. `sniffImageType` já aceita WebP e o servidor grava o que recebe.

**Não baixar o teto de 5 MiB** (`maxChronoPhotoBytes`). `ErrFotoGrandeDemais` derruba o
lote de fotos inteiro; a outbox do app retenta para sempre e a foto nunca chega — viola o
"nada se perde" do projeto. O teto fica como proteção contra lixo, não como política de
tamanho. Quem garante o tamanho é o app.

### 6. `apps/client/src/modules/rh/EmployeesTab.tsx` — redimensionar no navegador antes de subir

Única foto que não passa pelo app. Volume baixo (uma por funcionário), mas hoje chega a
5 MB no banco e desce inteira para cada aparelho.

Recurso nativo do navegador, sem dependência nova. `createImageBitmap` já aplica a
orientação do EXIF por padrão:

```ts
// ponytail: JPEG e não WebP — Safari cai para PNG no canvas.toBlob('image/webp'),
// que sairia MAIOR. Foto de cadastro é uma por funcionário; o formato não pesa, a resolução pesa.
async function reduzirFoto(file: File, max = 512): Promise<Blob> {
  const bmp = await createImageBitmap(file);
  const f = Math.min(1, max / Math.max(bmp.width, bmp.height));
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(bmp.width * f);
  canvas.height = Math.round(bmp.height * f);
  canvas.getContext("2d")!.drawImage(bmp, 0, 0, canvas.width, canvas.height);
  bmp.close();
  return new Promise((ok, err) =>
    canvas.toBlob(
      (b) => (b ? ok(b) : err(new Error("Falha ao processar a foto."))),
      "image/jpeg",
      0.85,
    ),
  );
}
```

No submit, `uploadRHEmployeePhoto(targetId, await reduzirFoto(photoFile), token)`.
`uploadRHEmployeePhoto` já recebe `Blob` — **não muda**. 512px cobre com folga o uso real
(avatar no backoffice e cabeçalho do PDF no app).

A validação "máximo 5 MB" no `onChange` pode ficar: vale para o arquivo original, antes de
reduzir.

---

## Estimativa de ganho

Números **estimados** — a foto real de loja (luz interna, câmera frontal barata, ruído)
pode sair mais pesada. O passo 2 da verificação mede de verdade.

| Configuração | Pixels | Foto de batida |
|---|---|---|
| Hoje: potência de 2 até 1280, q80 | 648 a 1280px, varia por sensor | ~40–150 KB |
| **Proposto: 960 exato, q70** | 960×720 | **~40–70 KB, previsível** |
| Botão mais agressivo: 800, q65 | 800×600 | ~25–45 KB |

Projeção por loja, **10 funcionários × 4 batidas/dia × 26 dias × 12 meses ≈ 12.500 fotos/ano**:

| Tamanho médio | Por loja/ano |
|---|---|
| 100 KB | ~1,25 GB |
| 60 KB | ~750 MB |
| 40 KB | ~500 MB |

Foto de cadastro: de até 5 MB para ~30–60 KB.

---

## Testes

| Arquivo | O que cobrir |
|---|---|
| `app/src/test/java/com/chronopass/app/camera/ImageScaleTest.kt` | Reescrever para `targetSize`: 4000×3000 → 960×720; retrato 3000×4000 → 720×960; menor que o teto volta intacta (**nunca amplia**); `max <= 0` não trava; lado mínimo nunca vira 0 |

`PhotoCompressor` e `CameraCapture` dependem de `android.graphics` e da câmera — sem teste
JVM; ficam cobertos pela verificação em aparelho. `reduzirFoto` é chamada de API do
navegador, sem lógica própria que valha teste.

---

## Verificação

**1. Linha de base, ANTES de mexer** — confirma o bug de rotação e mede o tamanho atual:

```
scripts\dev.bat
# bater um ponto com o celular EM PÉ, depois:
adb shell run-as com.chronopass.app ls -l files/punches
adb exec-out run-as com.chronopass.app cat files/punches/<arquivo>.webp > antes.webp
```

Abrir `antes.webp`: **está de lado?** Anotar tamanho e dimensões.

**2. Depois** — mesmo procedimento, `depois.webp`:
- em pé, **não** de lado;
- maior lado **960px**;
- tamanho dentro da faixa estimada. Repetir em 5–10 batidas reais para ter uma média.

**3.** Fluxo da batida: o tempo entre "TIRAR FOTO" e a tela de confirmação tem de cair
visivelmente (sem JPEG cheio em disco).

**4.** Relatório PDF: a foto do colaborador aparece em pé.

**5.** Sincroniza. No Summus, a batida mostra a foto no `PunchesTab`, e:

```sql
SELECT key, content_type, length(bytes) FROM rh_chrono_photos ORDER BY received_at DESC LIMIT 20;
```

**6.** Backoffice web: subir como foto de funcionário um JPEG de celular de ~4 MB, **tirado
em pé**. Conferir `SELECT length(photo) FROM rh_employees WHERE id = '...'` na faixa de
dezenas de KB, foto em pé no backoffice, e o app baixando e exibindo.

**7.** Suítes:

```
scripts\test.bat                      # ChronoPass, JVM
task check                            # Summus (CI)
```

---

## Fora do escopo

- **Fotos já gravadas.** Não re-encodar: WebP → WebP perde qualidade de novo, e o servidor
  não tem encoder (Go sem WebP na stdlib, imagem `scratch` sem CGO). Elas já são WebP
  ≤1280px; o ganho não compensa.
- **Base64 no lote 2** (+33% só no tráfego — o servidor decodifica antes de gravar, então o
  banco não paga). Trocar para multipart muda o contrato por um ganho só de rede.
- **`SET STORAGE EXTERNAL`** em `rh_chrono_photos.bytes`. WebP não comprime de novo, então
  o TOAST desiste do pglz rápido. Ganho marginal de CPU.

### Dívida registrada: fotos no Postgres

Mesmo comprimidas, as fotos crescem na ordem de **centenas de MB por loja por ano** dentro
do Postgres — backup, restore e réplica carregam tudo. A compressão compra tempo, não
resolve a escala. Quando isso pesar, o próximo passo não é codec, é **tirar os bytes do
banco**: object storage (ou arquivar fotos antigas, no molde do módulo `cold-storage` que
já existe), deixando em `rh_chrono_photos` só a chave.

---

## Fontes

- [CameraX use case rotations — Android Developers](https://developer.android.com/media/camera/camerax/orientation-rotation)
- [Bitmap.CompressFormat — Android Developers](https://developer.android.com/reference/android/graphics/Bitmap.CompressFormat)
- [WebP vs AVIF — SpeedVitals](https://speedvitals.com/blog/webp-vs-avif/)
- [AVIF vs WebP: Speed, Quality, and Browser Support — Crystallize](https://crystallize.com/blog/avif-vs-webp)
