# ChronoPass → SummusBackoffice — Plano de integração

Documento de planejamento da sincronização entre o app ChronoPass e o SummusBackoffice
(backoffice interno). Decisões fechadas com o usuário. **Subida (§1-7) e descida (§8)
implementadas e testadas** — este arquivo documenta o contrato tal como o app usa.

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

## 8. Ponte de ids e canal de descida (implementado)

Bidirecional: além da subida (§1-7, inalterada), o app agora **puxa** cadastro, loja e
correções de ponto do Summus.

### Identidade — sem campo `summusEmployeeId` separado

O rascunho original previa um campo `summusEmployeeId` + tabela de mapeamento própria. Não foi
isso que foi implementado: o `uid` do employee (item 4) **é a própria ponte**. Quando o RH cria
um funcionário para a loja, o `uid` que o app recebe na descida é o `rh_employees.id` do
Summus — não existe coluna `summusEmployeeId` nem tabela de mapeamento, o casamento é o mesmo
campo que já existia para dedupe/idempotência.

- `employee.origin` (`SUMMUS` | `LOCAL`) substitui a ideia de "reservado" do rascunho antigo:
  diz quem é dono do cadastro. Funcionário criado no Summus chega com `origin=SUMMUS` e o
  Summus passa a mandar em nome/cargo/status; funcionário cadastrado no próprio app nasce
  `origin=LOCAL` e **continua funcionando offline normalmente** — ele só sobe na subida e
  aparece do lado do Summus como sugestão de vínculo (confirmação humana, nunca casamento
  automático).
- `employee.role` (cargo) é um campo novo, só preenchido pelo Summus.

### `GET /api/integrations/chronopass/pull?since=<cursor>`

Atrás do mesmo `apiKeyAuth` da subida. Parser: `sync/PullPayloads.kt` (100% JVM, org.json).
Envelope:

```jsonc
{
  "schemaVersion": 1,
  "serverTime": "2026-09-05T14:03:12.000Z",
  "store": { "uid": "<rh_stores.id>", "name": "...", "latitude": -23.5, "longitude": -46.6, "radiusMeters": 150.0 },
  "employees": [
    { "uid": "<rh_employees.id>", "name": "...", "role": null, "active": true, "deleted": false, "photoHash": "<sha256|null>", "mergeUids": ["<uid-local-duplicado>"] }
  ],
  "punchCorrections": [
    { "uid": "...", "punchType": "in", "timestampUtc": "...", "editedBy": "...", "editedAt": "...",
      "editReason": "...", "deleted": false, "revision": 3 }
  ],
  "newPunches": [
    { "uid": "<uuid-da-batida>", "employeeUids": ["<uid-local-vinculado>", "<rh_employees.id>"],
      "punchType": "in", "timestampUtc": "...", "tzOffsetMinutes": -180, "editedBy": "<user id>",
      "editedAt": "...", "editReason": "esqueceu de bater", "deleted": false, "revision": 1 }
  ]
}
```

- `serverTime` é o cursor: guardado em `app_settings` (`summus_pull_since`), volta como
  `?since=` no próximo pull. Cursor OPACO — nenhuma aritmética de data do lado do app, sem
  risco de relógio do aparelho.
- `since` vazio (primeiro pull, ou cursor perdido) = janela completa. Sem paginação de
  cadastro/loja (roster de uma loja é pequeno); só `punchCorrections` é recortado pelo `since`.
- `mergeUids` (aditivo; ausente = lista vazia): uids de outros cadastros da MESMA loja que o
  vínculo (aba Vínculos do Summus) declarou serem esta pessoa. O app junta os cadastros numa
  linha **visível** (`SyncRules.escolherSobrevivente`): o cadastro LOCAL da loja sobrevive e recebe
  nome/cargo do Summus; as batidas das demais linhas passam para ele e as duplicatas ativas (em geral
  a criada pela descida do RH) vão para a lixeira (`softDelete`). Nunca se absorve para dentro de
  linha da lixeira (isso escondia o funcionário — bug da v2.2.2); se tudo já está na lixeira, respeita
  a lixeira. Foto/hash continuam chegando pelo uid do RH e vão para a linha que sobrou (alias em
  `app_settings`, `summus_alias.<uid>`). Tudo dentro do seam anti-eco (nada é enfileirado),
  idempotente, sem migration de Room.
- `deleted: true` em `employees[]` (funcionário **excluído** no Summus, só depois de desativado): item
  sintético `{uid, name, active:false, deleted:true, mergeUids:[]}` que a loja recebe a cada pull (tombstone
  `rh_employee_deletions`). O app **desativa** a linha que tiver com esse uid (nunca apaga; lixeira local
  continua sendo escolha do aparelho) e ignora quem nunca conheceu. Os vínculos com cadastros locais já
  foram desfeitos no servidor; as batidas ficam.
- `newPunches` (aditivo; ausente = lista vazia): batidas **criadas no backoffice** depois de `since`
  (corte por instante de criação, não pelo horário da batida). O aparelho não as tem: cada uma é
  inserida como `Punch` (uid = o da batida, `serverRevision` = `revision`, sem foto/GPS, com
  `editedBy`/`editReason` de quem lançou e por quê). O dono é o **primeiro** de `employeeUids` com
  linha visível (cadastros locais vinculados primeiro, uid do RH por último); só lixeira → a primeira
  que existir; nenhuma → ignora. Depois do cadastro e antes das correções no mesmo pull; idempotente
  pelo uid; dentro do seam anti-eco (nada é enfileirado). O app nunca sobe essas batidas — o Summus já
  as tem (`device_id = "backoffice"`, reservado: um aparelho não pode usar esse id).
- Envelope inválido, `schemaVersion` diferente do esperado ou `punchType` desconhecido derrubam
  o pull inteiro sem aplicar nada — cursor não avança, servidor reenvia a mesma janela.

### `GET /api/integrations/chronopass/employee-photo/{uid}`

Foto de funcionário fica fora do corpo do pull. `photoHash` diferente do último aplicado
(guardado em `app_settings`, uma chave por uid) dispara este GET, um funcionário por vez,
resumível e com teto de 5 MiB.

### Regras de aplicação — nada de marcação se perde

- Toda escrita da descida passa por `ChronoRepository.applyFromSummus` / `applyPull` — grava
  direto nos DAOs **sem** enfileirar na `sync_outbox` (o seam anti-eco: sem isso, a mudança
  recebida do servidor voltaria para cima no próximo sync).
- Correção de ponto é sempre **update** atrás de uma guarda de revisão: só aplica se a
  `revision` recebida for maior que `punch.serverRevision` local — reenvio da mesma correção
  não reaplica, correção fora de ordem não regride o ponto.
- `deleted:true` (funcionário ou ponto) é sempre soft-delete — a linha continua no banco.
- A migration que introduziu o índice único de `uid` (Room v5) resolve duplicata legada
  **reatribuindo** um uid novo à linha mais nova, nunca com `DELETE`.
- `store.managedBySummus = true` (setado quando o pull traz `store`) trava latitude, longitude
  e raio como somente-leitura na tela de Configurações do app — esses campos vêm do backoffice
  e seriam sobrescritos no próximo pull mesmo se editados localmente.

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
