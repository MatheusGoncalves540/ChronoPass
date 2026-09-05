# ChronoPass → SummusBackoffice — Plano de integração

Documento de planejamento da sincronização entre o app ChronoPass e o SummusBackoffice
(backoffice interno). Decisões fechadas com o usuário. Nenhuma implementação feita ainda —
este arquivo é o contrato a ser seguido.

---

## 1. Objetivo

Enviar ao Summus, por fila de sincronização persistente, os registros de ponto (e dados de
funcionário) gerados no aparelho, **sem perder nada** mesmo com internet caindo no meio do
caminho. Todo o histórico passa a morar no Summus; o app mantém apenas o que ainda não foi
sincronizado.

## 2. Configuração no app (feature em Configurações)

- Nova seção **"SummusBackoffice"** na tela de Configurações (área administrativa).
- Dois campos: **URL do endpoint** e **api-key**.
- Após definidos, ambos ficam **trancados e ocultos** (api-key com máscara; URL pode aparecer,
  api-key não). A única operação possível depois é **sobrescrever** (ex.: reautenticar com uma
  nova api-key).
- Dados guardados em `app_settings` (chaves `summus_url` e `summus_api_key`).
- Api-key viaja no **header HTTP** (`Authorization: Bearer <api-key>`), nunca no corpo.
- Sem URL/api-key definidas, a sincronização fica inativa (fila acumula, nada é perdido).

## 3. Identidade do aparelho

- `device.id = "chronopass-" + Settings.Secure.ANDROID_ID`
- **Persiste à desinstalação** (mesma assinatura do APK). Muda apenas em factory reset.
- Sem permissão especial. É o máximo que o Android permite; armazenamento privado não
  sobrevive à desinstalação.
- Nada de endereço MAC (Android 6+ bloqueia leitura; Android 10+ randomiza por rede).
- `device.model = Build.MODEL` (informativo).

## 4. Id externo das entidades — uid (UUID)

Decisão: **(a) uid por entidade** (não composite com id local).

- Cada `employee` e cada `punch` ganha um campo `uid` (UUID v4), gerado na criação e único
  para sempre — sobrevive a reinstalações, restaurações de backup e reenvios.
- O id Long interno (autoincrement) **continua sendo a PK do Room**; o `uid` é o id externo
  usado no payload ao Summus.
- Motivo: id local reinicia em 1 após reinstalação; com `device.id` persistente, um id local
  reusado sobrescreveria histórico antigo no Summus. O uid elimina a colisão e já deixa o
  gancho pronto para a ponte de ids do Summus (`summusEmployeeId`, item 8).
- Mudança de schema: colunas `uid` nullable em `employee` e `punch` + **backfill** em uma
  migração única (UPDATE onde `uid IS NULL`), preenchendo UUIDs para linhas existentes.
- Dedupe/idempotência no Summus pela chave `uid`: reenvio após timeout nunca duplica.

## 5. Lotes de envio

Dois envios separados por tipo:

### Lote 1 — Metadados (`employees` + `punches`)

```jsonc
{
  "schemaVersion": 1,
  "app": "chronopass",
  "appVersion": "2.2.0",
  "device": { "id": "chronopass-<ANDROID_ID>", "model": "<Build.MODEL>" },
  "store": { "id": 1, "name": "Loja Principal" },
  "summary": {
    "punchCount": 42, // qtde de pontos NESTE lote
    "uniqueEmployeeCount": 7, // funcionários distintos nesses pontos
  },
  "exportedAt": 1756188492000, // epoch ms UTC — montagem do lote
  "employees": [
    {
      "uid": "<uuid>",
      "name": "João da Silva",
      "code": "JS-001",
      "active": true,
      "deleted": false, // lixeira do app — nunca some no Summus
      "createdAt": 1700000000000,
    },
  ],
  "punches": [
    {
      "uid": "<uuid>", // chave única no Summus
      "employeeUid": "<uuid>", // vínculo
      "employee": {
        // denormalizado p/ exibição; join real é por uid
        "uid": "<uuid>",
        "name": "João da Silva",
        "code": "JS-001",
      },
      "type": "IN", // "IN" | "OUT"
      "timestampUtc": 1756188492000, // epoch ms = instante absoluto
      "tzOffsetMinutes": -180, // fuso no momento da marcação (UTC−3 → −180)
      "latitude": -23.55052,
      "longitude": -46.6333,
      "accuracyMeters": 8.0,
      "editedBy": null, // quem corrigiu (admin)
      "editedAt": null, // epoch ms da correção
      "editReason": null, // motivo obrigatório
      "deleted": false, // exclusão chega como flag
      "photo": {
        // só referência — bytes vão no Lote 2
        "key": "<uuid-do-ponto>",
        "fileName": "2026-08-25_08-03-12.jpg",
        "contentType": "image/jpeg",
      },
    },
  ],
}
```

> **Adendo — Divergência contrato real (2026-09).** O bloco acima é o rascunho
> original; o servidor SummusBackoffice (fonte: `apps/server/internal/modules/rh/chrono.go`)
> define o shape autoritativo e o app agora emite ELE. Diferenças aplicadas no `SummusPayloads`:
> `punchType` `"in"`/`"out"` (não `type`); `timestampUtc`/`editedAt`/`exportedAt` em RFC3339
> (não epoch ms); `employee` denorm só `{name,role}` (sem uid/code); `photoKey` no lugar de
> `photo{}`; `store.id` string; `employees[]` sem `code`/`createdAt`; lote 1 sem `exportedAt`.

### Lote 2 — Imagens (envio separado)

```jsonc
{
  "schemaVersion": 1,
  "loteType": "photos",
  "device": { "id": "chronopass-<ANDROID_ID>", "model": "<Build.MODEL>" },
  "store": { "id": 1, "name": "Loja Principal" },
  "exportedAt": 1756188492000,
  "photos": [
    {
      "key": "<uuid-do-ponto>", // bate com punch.photo.key do Lote 1
      "fileName": "2026-08-25_08-03-12.jpg",
      "contentType": "image/jpeg",
      "dataBase64": "<base64>",
    },
  ],
}
```

- Lote de metadados pequeno e rápido; lote de imagens pode falhar sozinho sem perder dados.
- Associação no Summus: `device.id` (contexto) + `key`/`uid`.

## 6. Regras que evitam perda

| Regra                     | Detalhe                                                                             |
| ------------------------- | ----------------------------------------------------------------------------------- |
| Nada é apagado no destino | `deleted:true` vira exclusão lógica no Summus (como a lixeira do app)               |
| Edição chega inteira      | correção com `editedBy/editedAt/editReason` no mesmo registro                       |
| Nulos são explícitos      | `null` (nunca omitido) distingue "sem GPS" de "campo ausente"                       |
| Idempotência              | chave única por `uid`; reenviar o mesmo lote não duplica                            |
| Sem cursor                | dedupe por `uid` + remoção do evento após ack dispensam `lastPunchId`/`lastEventId` |

## 7. Fila de sincronização (`sync_outbox`)

Tabela Room persistente — **nada se perde com a internet caindo**.

```
sync_outbox(
  id          INTEGER PK AUTOINCREMENT,
  tipo        TEXT,        -- EMPLOYEE | PUNCH | PHOTO
  refUid      TEXT,        -- uid da entidade (null p/ foto de employee → "employee.<uid>")
  payload     TEXT,        -- JSON do estado atual da entidade
  status      TEXT,        -- PENDING | FAILED | DONE
  tentativas  INTEGER,
  ultimoErro  TEXT,
  createdAt   INTEGER      -- epoch ms
)
```

- **Enfileiramento (pontos de escrita):** novo/corrigir/excluir ponto → evento PUNCH;
  funcionário criado/editado/desativado → EMPLOYEE; foto de marcação nova → PHOTO.
- **Compactação (não cresce pra sempre):** antes de enviar, apaga eventos PENDING antigos da
  mesma entidade (`tipo`+`refUid`), mantendo só o mais novo — muitas correções do mesmo ponto
  viram um evento só; funcionário renomeado 5× = 1 evento.
- **Drenagem:** agrupa PENDING de metadados (EMPLOYEE+PUNCH) → Lote 1; PENDING de PHOTO →
  Lote 2. Calcula `summary` (punchCount, uniqueEmployeeCount) sobre o que está sendo enviado.
- **Ack:** resposta HTTP 2xx → evento vira DONE e é **removido** da fila. Histórico completo
  fica no Summus; a fila só contém o que ainda não sincronizou.
- **Falha:** volta a PENDING (ou FAILED com contador) com retry/backoff (ex.: 30s → 1min →
  5min).
- **Gatilhos de envio (sem WorkManager por enquanto):** abertura do app / retorno ao
  foreground, após cada ponto registrado, e botão **"Sincronizar agora"** no admin.
  Upgrade futuro: trocar o gatilho por WorkManager (androidx.work) sem tocar na fila.

## 8. Ponte futura de ids do Summus (reservado)

- O Summus terá um `employeeId` próprio; nada implementado ainda.
- Quando a ponte existir: entra um campo `summusEmployeeId` no registro do employee + um
  mapeamento persistido no app (tabela própria).
- O `uid` já nasce como chave externa estável que permite esse casamento sem migração de
  dados. Nada no contrato atual muda quando isso acontecer.

## 9. Fora do contrato

- `store` (lat/lon/raio): contexto fixo, vai no envelope; não tem eventos próprios.
- `app_settings`: fica fora — guarda segredos (senha do admin e a própria api-key) que não
  trafegam.
- Senha do administrador jamais sai do aparelho.

## 10. Ordem de implementação (modo Act)

1. **Migração Room**: colunas `uid` em `employee` e `punch` (com backfill) + tabela
   `sync_outbox`. Geração de uid na criação de entidades.
2. **Configurações**: seção "SummusBackoffice" (URL + api-key), trancadas após definir, só
   sobrescrever; api-key oculta; persistência em `app_settings`.
3. **SummusClient**: montagem dos 2 lotes + POST com api-key no header; parsing de ack 2xx.
4. **Fila/drenagem**: enfileirar nos pontos de escrita, gatilhos de envio, retry com backoff,
   compactação e delete-no-ack.
5. **Testes** (padrão do projeto, JVM pura): serialização dos lotes, compactação da fila,
   delete-no-ack, geração/backfill de uid.
