# Fotos de ponto fora do banco: disco da VM + exportação ZIP + exclusão

## Context

As fotos de batida hoje vivem em `rh_chrono_photos.bytes` (BYTEA) no Postgres do Summus.
Elas não devem ficar no banco com o tempo. O objetivo:

1. **Tirar as fotos do banco** — elas passam a morar no volume de cold storage da VM.
2. **Exportar por período** — um ZIP com pastas legíveis, para guardar fora do sistema.
3. **Excluir o que foi exportado** — com aviso claro de que, depois disso, o ZIP é a única
   cópia. É isso que impede o esgotamento de espaço.

Hoje: **8 lojas, 1 usando o ponto.**

### Onde as coisas estão (confirmado no código)

| Peça | Onde |
|---|---|
| Postgres | **Externo**, outra máquina (`134.65.17.135:5432`, `k8s/README.md`) |
| Cold storage | Disco da VM, hostPath `/opt/summus-backoffice/archives` → `COLD_STORAGE_DIR=/app/data/archives` |
| Proxy | nginx na VM (TLS) → `127.0.0.1:3001` |
| Fotos de batida | Módulo `rh`, branch `feat/modulo-rh`: `IngestPhotos` (`chrono.go`), leitura `GET /api/rh/chronopass/photos/{key}` (`chrono_handlers.go`), exibidas no `PunchesTab` |
| Metadados da batida | `rh_chrono_punches.photo_key` → `rh_chrono_photos.key` (loja, funcionário, horário, tipo) |

---

## Decisões de desenho

### 1. Fotos novas vão direto para o disco — nem entram no banco

A ideia inicial era "o cold-storage tem uma configuração que manda as fotos do banco para
lá". Tem um caminho mais simples com o mesmo resultado: **o ingest grava o arquivo no disco
e o banco guarda só a referência** (chave, caminho, tamanho, hash).

Não existe ganho em deixar a foto N dias no banco antes de mover. Ler do disco é tão rápido
quanto ler do banco, e cortar essa etapa elimina o job de movimentação, a configuração de
dias e a janela em que a foto está nos dois lugares. As fotos que **já estão** no banco
saem por uma migração única (item 3).

### 2. "No disco consome menos espaço?" — pouco. O que libera espaço é exportar e excluir

A WebP já é comprimida, então no disco ela ocupa praticamente os mesmos bytes que no banco.
O que muda ao sair do Postgres:

- o banco para de pagar TOAST, WAL e inchaço até o `VACUUM`;
- **backup e restore do banco deixam de carregar as fotos** — é o ganho principal;
- como o Postgres fica em outra máquina, o peso muda de máquina: sai do servidor do banco
  e vai para o disco da VM.

O espaço total só cai quando as fotos são **exportadas e excluídas** (itens 5 e 6). Mover
tira a pressão do banco; exportar e excluir resolve o disco.

### 3. O código fica no módulo `rh`, não no módulo `cold-storage`

O módulo `cold-storage` é genérico: conhece `Adapter`s que viram JSONL + gzip por mês, e o
único adapter é `access_logs`. Foto não cabe nesse molde (base64 dentro de JSONL fica 33%
maior, gzip não comprime WebP, e achar uma foto obrigaria a ler o mês inteiro). E se o
`cold-storage` importasse o `rh`, quebraria a regra de módulos do `CLAUDE.md` (módulo fora
de `config/modules.json` não compila).

Então: o **módulo `rh`** é dono das fotos e reaproveita a **infra compartilhada**
`internal/coldstorage/storage` (o `storage.Local`, com checagem de path traversal) apontando
para o **mesmo volume** persistente da VM, numa subpasta própria.

### 4. Exportação por link assinado, com download nativo do navegador

O client autentica por header `Bearer` (sem cookie), então um `<a href>` comum não
autentica. Baixar via `fetch` → `Blob` carrega o ZIP inteiro na memória da aba — e um ano
de uma loja passa de 600 MB. Por isso:

- `POST` (autenticado) cria a exportação e devolve uma **URL assinada com HMAC**, válida por
  15 minutos, usando o `AUTH_TOKEN_SECRET` que já existe;
- o navegador abre essa URL e o **download nativo grava direto no disco do usuário**;
- o servidor faz o **streaming** do ZIP para a resposta: nada é montado em arquivo
  temporário na VM.

### 5. Excluir só o que comprovadamente saiu, e só o que existia na hora

- A exclusão só é liberada para uma exportação cujo ZIP **terminou de ser enviado sem erro**
  (`downloaded_at`).
- A exportação congela um **`snapshot_at`**. Download e exclusão usam o **mesmo filtro**,
  mais `received_at <= snapshot_at`. Um aparelho que ficou offline e sincroniza uma foto
  antiga **depois** da exportação não perde essa foto, porque ela não estava no ZIP.
- Exclusão deixa um **tombstone**: a linha continua no banco, sem bytes e sem arquivo, com
  `purged_at`. O backoffice mostra "foto exportada e removida", e um reenvio do app não
  ressuscita a foto.
- **Os registros de batida não são tocados** — horário, local, funcionário e revisões
  continuam no sistema. Só a foto sai.

---

## Estrutura do ZIP

```
fotos-ponto_2026-01-01_a_2026-03-31.zip
└── fotos-ponto_2026-01-01_a_2026-03-31/
    ├── LEIA-ME.txt
    ├── indice.csv
    ├── Loja Centro/
    │   ├── 2026-01/
    │   │   ├── João Silva/
    │   │   │   ├── 2026-01-15_08-02-11_entrada.webp
    │   │   │   ├── 2026-01-15_12-00-40_saida.webp
    │   │   │   └── 2026-01-15_13-01-02_entrada.webp
    │   │   └── Maria Souza/
    │   └── 2026-02/
    └── Loja Norte/
```

- **Loja → mês → funcionário → arquivo.** Responde direto às perguntas mais comuns
  ("as fotos do João em março") e cada nível tem poucas entradas.
- **Data completa no nome do arquivo** — uma foto copiada para fora da pasta continua
  dizendo de quando e de quê é. Ordem alfabética = ordem cronológica.
- **Horário local da loja**: `timestamp_utc + tz_offset_minutes` (fallback −180 quando
  nulo). O filtro de período também usa a data local.
- Batida excluída logicamente (`deleted = true`) **entra**, com sufixo `_excluida` — é
  evidência também.
- Nomes com caracteres inválidos para arquivo (`/ \ : * ? " < > |`) são trocados por `_`;
  acentos ficam (o `archive/zip` do Go marca UTF-8). Colisão no mesmo segundo ganha `_2`.

**`indice.csv`** — uma linha por foto, separador `;` e BOM UTF-8 (abre direto no Excel em
pt-BR):

```
data;hora;tipo;loja;funcionario;cargo;latitude;longitude;editado_por;motivo_edicao;batida_excluida;arquivo;sha256;id_batida
```

**`LEIA-ME.txt`** — período, loja(s), data da geração, quantidade, tamanho total, como
navegar, e o aviso: *"Se as fotos deste período forem excluídas do Summus, este arquivo é a
única cópia."*

---

## Servidor — `d:\BUSINESS\SummusBackoffice`, branch `feat/modulo-rh`

### 1. `apps/server/internal/db/migrations/0009_rh_photos_disk.sql`

Idempotente, no padrão de `0005`–`0008`:

```sql
-- Foto sai do banco: bytes vira opcional (NULL = está no disco ou foi excluída).
ALTER TABLE "rh_chrono_photos" ALTER COLUMN "bytes" DROP NOT NULL;
ALTER TABLE "rh_chrono_photos" ADD COLUMN IF NOT EXISTS "path" TEXT;        -- relativo ao storage
ALTER TABLE "rh_chrono_photos" ADD COLUMN IF NOT EXISTS "size_bytes" BIGINT;
ALTER TABLE "rh_chrono_photos" ADD COLUMN IF NOT EXISTS "sha256" TEXT;
ALTER TABLE "rh_chrono_photos" ADD COLUMN IF NOT EXISTS "purged_at" TIMESTAMP(3);
ALTER TABLE "rh_chrono_photos" ADD COLUMN IF NOT EXISTS "purge_export_id" UUID;

CREATE TABLE IF NOT EXISTS "rh_photo_exports" (
    "id" UUID NOT NULL,
    "date_from" DATE NOT NULL,
    "date_to" DATE NOT NULL,
    "store_id" UUID,                          -- NULL = todas as lojas
    "snapshot_at" TIMESTAMP(3) NOT NULL,      -- só fotos recebidas até aqui
    "photo_count" INTEGER NOT NULL,
    "total_bytes" BIGINT NOT NULL,
    "created_by" UUID NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "downloaded_at" TIMESTAMP(3),             -- ZIP enviado inteiro, sem erro
    "error" TEXT,                             -- última falha de download
    "purged_at" TIMESTAMP(3),
    "purged_by" UUID,
    "purged_count" INTEGER,
    CONSTRAINT "rh_photo_exports_pkey" PRIMARY KEY ("id")
);
```

Espelhar as colunas novas em `RHChronoPhoto` e criar `RHPhotoExport` em
`apps/server/internal/db/models.go` — **sem `gorm:"default:..."`** (bug conhecido do `CLAUDE.md`).

### 2. `apps/server/internal/coldstorage/storage/local.go` — `Delete` e `fsync`

Infra compartilhada, duas mudanças pequenas:

- `Delete(relativePath string) error` — a exclusão precisa apagar arquivo, e o provider não
  tem essa operação. Passa pelo mesmo `resolvePath` (path traversal). Arquivo inexistente
  não é erro (idempotente).
- `f.Sync()` antes de fechar em `Write`/`WriteStream`. Depois do ack o app tira a foto da
  fila e não reenvia; sem `fsync`, uma queda de energia na VM logo depois do ack perde a
  foto. Beneficia também o arquivamento de `access_logs`.

### 3. `apps/server/internal/modules/rh/photos.go` (novo) — gravação, leitura, migração

Um arquivo só para o ciclo de vida da foto no disco.

**Storage:** `storage.New(filepath.Join(ctx.Env.ColdStorageDir, "rh-photos"))`, criado no
`Register` do módulo. Independe de `COLD_STORAGE_ENABLED`: aquela flag liga o *agendador* de
`access_logs`, não o volume — e foto de batida tem que ser gravada de qualquer jeito.

**Layout interno** (não é o que o usuário vê): `rh-photos/AAAA/MM/<key>.webp`, particionado
pelo mês de **recebimento**. A foto chega no lote 2, independente do lote 1, então o horário
da batida pode ainda não ser conhecido no ingest. O caminho fica gravado no banco, e o
layout pode mudar sem afetar nada.

**Gravação** — `IngestPhotos` (`chrono.go`) passa a:

1. decodificar + `sniffImageType` + teto de 5 MiB (**como hoje**);
2. calcular `sha256`, gravar o arquivo com `storage.Write` (com `fsync`);
3. upsert da linha **sem bytes**: `path`, `size_bytes`, `sha256`, `content_type`, `received_at`.

Ordem arquivo → banco: se o upsert falhar, sobra um arquivo órfão inofensivo, e o reenvio
com a mesma chave sobrescreve. O conflito ganha a guarda
`... DO UPDATE ... WHERE rh_chrono_photos.purged_at IS NULL` — foto excluída **não volta**.

Nome do arquivo: a chave com qualquer caractere fora de `[A-Za-z0-9._-]` trocado por `_`
(as chaves reais são UUID ou `employee.<uid>`).

**Leitura** — `ChronoPhotoByKey` + handler `chronoPhoto`:

| Estado da linha | Resposta |
|---|---|
| `purged_at` preenchido | **410 Gone** + data da exclusão |
| `path` preenchido | stream do arquivo (`storage.OpenRead`) |
| só `bytes` | bytes do banco (janela da migração) |

**Migração das fotos antigas** — `migrarFotosParaDisco`, em lotes de 100 linhas com
`bytes IS NOT NULL AND path IS NULL`: grava o arquivo, confere o hash, atualiza
`path/size/sha256` e zera `bytes`. Idempotente e retomável: roda em background no boot (mesmo
padrão do `startupRun` do cold-storage, esperando o banco) até não sobrar linha.

Depois que terminar, rodar **uma vez, manualmente**, no Postgres externo:

```sql
VACUUM FULL rh_chrono_photos;
```

Sem isso o arquivo da tabela não diminui — o `VACUUM` comum só marca o espaço como
reutilizável. Com uma loja só, a tabela é pequena e o lock dura segundos; rodar fora do
horário mesmo assim.

### 4. `apps/server/internal/modules/rh/photo_exports.go` (novo) — exportar e excluir

**Consulta única** (usada pela contagem, pelo download e pela exclusão — tem que ser a mesma):

```sql
SELECT ph.key, ph.path, ph.sha256, ph.size_bytes, p.*, s.name AS store_name
FROM rh_chrono_photos ph
JOIN rh_chrono_punches p ON p.photo_key = ph.key
LEFT JOIN rh_stores s ON s.id = p.store_id
WHERE ph.purged_at IS NULL
  AND ph.path IS NOT NULL
  AND ph.received_at <= :snapshot_at
  AND (p.timestamp_utc + make_interval(mins => COALESCE(p.tz_offset_minutes, -180)))::date
      BETWEEN :date_from AND :date_to
  AND (:store_id IS NULL OR p.store_id = :store_id)
ORDER BY ph.key          -- paginação por keyset, não OFFSET
```

- O `JOIN` com batida exclui sozinho as fotos de cadastro (`employee.<uid>`), que não são
  foto de ponto.
- `ph.path IS NOT NULL` deixa de fora fotos que a migração ainda não moveu. A tela avisa
  quando existe migração pendente.

**Rotas** (em `module.go`):

| Rota | Permissão | O que faz |
|---|---|---|
| `POST /api/rh/chronopass/photo-exports` | `rh:admin` | Body `{dateFrom, dateTo, storeId?}`. Grava `snapshot_at = now`, conta fotos e bytes, devolve a exportação + URL assinada (15 min) |
| `GET /api/rh/chronopass/photo-exports` | `rh:admin` | Lista as exportações (período, loja, quantidade, tamanho, quem gerou, baixado?, excluído?) |
| `POST /api/rh/chronopass/photo-exports/{id}/link` | `rh:admin` | Nova URL assinada, para baixar de novo |
| `GET /api/rh/chronopass/photo-exports/{id}/download?exp=&sig=` | **assinatura HMAC** (sem sessão) | Stream do ZIP |
| `POST /api/rh/chronopass/photo-exports/{id}/purge` | **super admin** (`ctx.AuthenticateSuperAdmin`) | Exclui as fotos daquela exportação |

**Assinatura:** `sig = HMAC-SHA256(AUTH_TOKEN_SECRET, id + "|" + exp)`, comparada com
`hmac.Equal`. A expiração só é checada **no início** — download de 1 GB pode passar de 15 min.

**Download** — streaming direto na resposta, stdlib `archive/zip`:

```go
w.Header().Set("Content-Type", "application/zip")
w.Header().Set("Content-Disposition", `attachment; filename="fotos-ponto_2026-01-01_a_2026-03-31.zip"`)
w.Header().Set("X-Accel-Buffering", "no") // nginx não bufferiza em arquivo temporário na VM
zw := zip.NewWriter(w)
```

- Fotos com `Method: zip.Store` — WebP não comprime, então deflate seria só CPU gasta.
  `indice.csv` e `LEIA-ME.txt` com `zip.Deflate`.
- Cada foto: `storage.OpenRead` → `io.Copy` na entrada do ZIP, **recalculando o sha256** no
  caminho. Arquivo ausente ou hash divergente **aborta a exportação**: grava `error`, fecha a
  conexão, e o navegador acusa falha. ZIP incompleto nunca libera a exclusão.
- As linhas do CSV acumulam em memória e são escritas no fim, junto com o `LEIA-ME.txt`
  (entradas de ZIP são sequenciais; a ordem no arquivo não importa para quem abre).
  `// ponytail: CSV em memória, ~200 B/linha — 100 mil fotos ≈ 20 MB. Arquivo temporário se passar disso.`
- ZIP > 4 GB ou > 65.535 entradas: o `archive/zip` usa zip64 sozinho.
- Só depois do `zw.Close()` sem erro: `downloaded_at = now`.

**Exclusão** — pré-condições, senão 409:

- `downloaded_at IS NOT NULL` e `purged_at IS NULL`;
- body `{confirmacao}` igual ao texto do período (ex.: `"2026-01-01 a 2026-03-31"`) — o
  usuário **digita**, não só clica.

Em lotes de 200, com a mesma consulta do download:

1. transação: `UPDATE rh_chrono_photos SET path = NULL, bytes = NULL, purged_at = now, purge_export_id = :id`;
2. commit;
3. `storage.Delete` de cada arquivo.

Ordem banco → disco: se o `Delete` falhar, sobra um arquivo órfão (espaço perdido, dado
nenhum) e o erro vai para o log. O inverso deixaria linhas apontando para arquivos que não
existem. No fim: `purged_at`, `purged_by`, `purged_count` na exportação.

**Registro de auditoria:** criar exportação, baixar e excluir passam pelo `audit-logs` que já
existe no backoffice. Exportar fotos faciais em massa é tratamento de dado pessoal (LGPD) e
tem que ter rastro de quem fez.

---

## Client — `d:\BUSINESS\SummusBackoffice\apps\client`, branch `feat/modulo-rh`

### 5. `src/modules/rh/PhotosTab.tsx` (nova aba "Fotos" em `RhModule.tsx`)

**Topo — ocupação:** fotos no disco (quantidade + tamanho), fotos ainda no banco (migração
pendente) e o período mais antigo que ainda não foi exportado. É o que diz "hora de
exportar".

**Nova exportação:** período (`<input type="date">` × 2) + loja (todas ou uma). "Gerar" →
mostra "N fotos, X MB" → **Baixar ZIP** abre a URL assinada (`window.location.assign`), e o
download nativo grava no disco.

**Lista de exportações:** período, loja, fotos, tamanho, quem gerou, quando, status
(`Gerada` / `Baixada` / `Falhou: …` / `Excluída em …`). Ações: **Baixar de novo** (pede novo
link) e **Excluir fotos do sistema** (só aparece para super admin e só com status `Baixada`).

**Diálogo de exclusão** — o aviso é o ponto central deste item:

> **Excluir 1.284 fotos (61 MB) de 01/01/2026 a 31/03/2026 — Loja Centro**
>
> As fotos serão **apagadas definitivamente** do Summus. O sistema não guarda outra cópia.
>
> Se essas fotos ainda forem necessárias para controle — conferência de ponto, fiscalização
> ou processo trabalhista (que pode questionar até 5 anos de contrato) — **confirme que o
> arquivo ZIP foi salvo em local seguro fora da VM** (Google Drive, HD externo) e que ele
> abre, antes de continuar.
>
> Os registros de batida (horário, local, funcionário) **não são apagados** — só as fotos.
>
> Para confirmar, digite: `2026-01-01 a 2026-03-31`

Botão desabilitado até o texto bater.

### 6. `src/modules/rh/PunchesTab.tsx`

Quando `GET /photos/{key}` responde **410**, mostrar "Foto exportada e removida em DD/MM/AAAA"
no lugar da imagem, e não o ícone genérico de erro do `AuthImage`.

---

## App ChronoPass

**Nada muda.** O contrato do lote 2 é o mesmo e o app não reenvia foto já confirmada (a
PHOTO só entra na outbox em `addPunch` e na troca de foto do funcionário).

---

## Volume esperado

Premissa: ~10 funcionários por loja, 4 batidas/dia, 26 dias/mês, ~50 KB por foto (depois do
plano `compressao-fotos.md`). Ajustar com os números reais.

| Cenário | Fotos/ano | Espaço/ano | ZIP de 1 trimestre |
|---|---|---|---|
| **Hoje: 1 loja** | ~12.500 | **~600 MB** | ~150 MB |
| 8 lojas usando | ~100.000 | ~5 GB | ~1,2 GB |

Com uma loja, uma rotina **trimestral** de exportar + excluir mantém o disco da VM
praticamente parado.

---

## Testes

| Arquivo | O que cobrir |
|---|---|
| `internal/coldstorage/storage/local_test.go` | `Delete` apaga; arquivo inexistente não é erro; path traversal recusado |
| `modules/rh/photos_module_test.go` (Postgres real, `task go:test:it`) | Ingest grava arquivo e linha **sem bytes**; reenvio da mesma chave sobrescreve; reenvio de chave **excluída** não ressuscita; leitura 410 / disco / banco; migração move, zera `bytes` e é idempotente |
| `modules/rh/photo_exports_module_test.go` | ZIP válido com a estrutura de pastas, `indice.csv` e `LEIA-ME.txt`; horário local no nome; filtro por loja e período; **foto recebida depois do `snapshot_at` não entra nem é excluída**; assinatura vencida/adulterada → 403; arquivo faltando aborta e **não** marca `downloaded_at`; exclusão recusada sem download completo ou com confirmação errada; exclusão marca tombstone e apaga os arquivos |
| `modules/rh/photo_exports_test.go` (puro) | Nome de pasta/arquivo: caracteres inválidos, acentos, colisão `_2`, sufixo `_excluida` |

A regressão importante é a do `snapshot_at`: é ela que garante que nenhuma foto nunca
exportada seja excluída.

---

## Verificação

```
task go:test
task db:dev:up && task go:test:it
task check
```

**Ponta a ponta** (dev local):

1. Banco com fotos antigas em `bytes`. Subir o servidor → log da migração → conferir
   `SELECT count(*) FROM rh_chrono_photos WHERE bytes IS NOT NULL` chegando a 0 e os
   arquivos em `data/archives/rh-photos/`.
2. Bater pontos no app e sincronizar → arquivo novo no disco, linha com `bytes IS NULL`.
   Foto aparece normalmente no `PunchesTab`.
3. Aba **Fotos** → exportar um período → baixar. Abrir o ZIP no Windows Explorer: pastas por
   loja/mês/funcionário, nomes com acento corretos, `indice.csv` abrindo certo no Excel,
   `sha256` do CSV batendo com o arquivo.
4. **Antes de excluir**, sincronizar do app uma batida antiga **dentro** do período. Excluir
   com o texto de confirmação → as fotos exportadas somem do disco, o `PunchesTab` mostra
   "exportada e removida", e **a batida que chegou depois continua com foto**.
5. Tentar excluir sem ter baixado → recusado. Cancelar um download no meio → status não vira
   `Baixada`.
6. Reenviar uma foto excluída pelo endpoint do app → continua excluída.
7. Em produção, depois da migração: `VACUUM FULL rh_chrono_photos;` e conferir o tamanho da
   tabela com `SELECT pg_size_pretty(pg_total_relation_size('rh_chrono_photos'));`.

---

## Fora do escopo

- **Fotos no aparelho.** O app **nunca apaga** a foto local depois do envio (só o restore de
  backup limpa a pasta). O disco do celular cresce do mesmo jeito. Plano separado, quando pesar.
- **Foto de cadastro do funcionário** (`rh_employees.photo` e chaves `employee.<uid>`): uma
  por pessoa, volume irrelevante — continua como está e fica fora da exportação.
- **Exclusão automática por idade.** Deliberado: excluir sem exportação comprovada contraria o
  "nada se perde". Se um dia fizer sentido, é uma regra em cima da lista de exportações
  baixadas, não um apagador cego.
- **Object storage.** O disco da VM + exportação periódica cobre o volume de 1 loja por
  anos. Com as 8 lojas usando, reavaliar.
