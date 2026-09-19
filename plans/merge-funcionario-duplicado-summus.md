# Consolidar funcionário duplicado entre Summus e ChronoPass

## Context

Quando o RH cria um funcionário no Summus, ele desce para o ChronoPass e aparece como
uma **segunda linha** ao lado do funcionário que a loja já tinha cadastrado no app — o que
já tem histórico de batidas. Vincular os dois na aba **Vínculos** não resolve: as duas
linhas continuam ativas na tela de ponto, atrapalhando o dia a dia da loja.

O vínculo hoje é uma anotação puramente server-side. É preciso fazer essa decisão
humana descer até o aparelho e consolidar as duas linhas em uma, **sem perder nenhuma
batida**.

### Causa raiz

O `uid` do funcionário é a ponte entre os dois sistemas (`SUMUS-INTEGRACAO.md` §8), mas
o vínculo nunca faz os uids baterem:

1. Funcionário nasce no app → `Employee(uid=<UUID aleatório>, origin=LOCAL)`, acumula batidas.
2. RH cria a mesma pessoa no Summus → `rh_employees(id=<uuid-summus>)`.
3. Pull desce `{uid: "<uuid-summus>"}`. Em `ChronoRepository.applyFromSummus` o casamento é
   **só por uid** (`employeeByUid`, sem fallback por nome) → não acha → `employees.insert(...)`.
   **A duplicata nasce aqui.**
4. Admin vincula na aba Vínculos → `ConfirmLink` faz um único
   `UPDATE rh_chrono_employees SET rh_employee_id = ?`. Não desativa, não mescla, não
   enfileira nada para o aparelho.
5. `pullEmployees` continua emitindo só `uid = rh_employees.id` — ignora por completo o
   `rh_chrono_employees.uid` que acabou de ser declarado a mesma pessoa.
6. `activeList()` (`WHERE active = 1 AND deleted = 0`) mostra as duas linhas.

Agravante: `PunchDao.lastFor(employeeId)` é por linha, então quem bate Entrada numa
linha e toca na outra recebe Entrada de novo.

### Decisões tomadas

- **Merge de verdade**, servidor + app (não o paliativo server-only).
- Linha absorvida **vai para a lixeira** (`softDelete`), não é excluída — vínculo errado
  continua recuperável.
- O problema separado de `rh_chrono_punches` não ter id de funcionário (só `employee_name`,
  agrupado por `ILIKE`) fica **registrado como dívida**, fora deste escopo.
- Pedido à parte, incluído aqui (item 8): a tela de cadastro do app passa a **barrar nome
  repetido** — prevenção na origem, independente do merge.

### Branches

Nada disso está nas branches ativas. O grafo do graphify está velho para esses arquivos.

| Repo | Branch de trabalho | Working tree hoje |
|---|---|---|
| `d:\BUSINESS\SummusBackoffice` | `feat/modulo-rh` | `development` (não tem o módulo `rh`) |
| `d:\BUSINESS\ChronoPass` | `feat/integração-summus` | `main` (não tem o pacote `sync/`) |

---

## Desenho

O pull passa a carregar, em cada funcionário, os **outros uids que o vínculo declarou
serem a mesma pessoa naquela loja**. O app move as batidas para a linha canônica e
recolhe a absorvida.

```
SERVIDOR  pullEmployees  ->  { uid: "<uuid-summus>", ..., mergeUids: ["<uid-local>"] }
                                                          ^ uids de rh_chrono_employees
                                                            vinculados a este rh_employee

APP       applyFromSummus
            canonica = acha-ou-cria por uid
            para cada uid em mergeUids:
                UPDATE punch SET employeeId = canonica WHERE employeeId = dup
                employees.softDelete(dup)
```

Duas propriedades que barateiam a mudança:

- **Aditivo no contrato.** `PullPayloads.parse` lê só as chaves que conhece e ignora
  campos desconhecidos — APK antigo não quebra com o servidor novo, e app novo contra
  servidor velho lê `mergeUids` como lista vazia. Dá para subir o servidor primeiro.
- **Sem migration de Room.** `punch.employeeId` já existe; a mudança é uma query de
  `UPDATE`, não uma coluna. O banco continua na **v5**.

---

## Servidor — `d:\BUSINESS\SummusBackoffice`, branch `feat/modulo-rh`

### 1. `apps/server/internal/modules/rh/pull.go` — emitir `mergeUids`

Campo novo em `PullEmployee` (sempre um slice, nunca `nil`, para o contrato ficar estável):

```go
type PullEmployee struct {
	...
	MergeUids []string `json:"mergeUids"`
}
```

Uma consulta só para a loja inteira (não N+1), agrupada em Go. A linha canônica
(`uid == rh_employee_id`, que o `autoLinkRHEmployeeID` vincula sozinho) é excluída — só
sobram os uids a absorver:

```go
func mergeUidsPorEmployee(gdb *gorm.DB, storeID string) (map[string][]string, error) {
	var rows []struct{ UID, RHEmployeeID string }
	err := gdb.Table("rh_chrono_employees").Select("uid, rh_employee_id").
		Where("store_id = ? AND rh_employee_id IS NOT NULL AND deleted = ?", storeID, false).
		Scan(&rows).Error
	if err != nil {
		return nil, err
	}
	out := map[string][]string{}
	for _, r := range rows {
		if r.UID == r.RHEmployeeID {
			continue // a própria linha canônica, não é duplicata
		}
		out[r.RHEmployeeID] = append(out[r.RHEmployeeID], r.UID)
	}
	return out, nil
}
```

`pullEmployees` chama uma vez e preenche `MergeUids: ouVazio(porEmployee[e.ID])`.

Escopo por `store_id` (não por device) segue o escopo que `pullEmployees` já usa. Um uid
de outro aparelho da mesma loja desce inofensivo: o app não acha localmente e pula.

### 2. `apps/server/internal/modules/rh/bridge.go` — soltar a guarda de duplicidade

`ConfirmLink` recusa com **409 `ErrVinculoDuplicadoAparelho`** quando outro
chrono-employee do **mesmo aparelho** já aponta para aquele `rh_employee`. Como a linha
que desceu do RH é auto-vinculada por `autoLinkRHEmployeeID` assim que sobe, essa guarda
hoje **bloqueia exatamente o vínculo que resolveria a duplicata**.

Remover o bloco do `SELECT ... Count(&dup)` + `if dup > 0`. Dois chrono-employees do mesmo
aparelho apontando para a mesma pessoa deixou de ser erro — é a declaração de duplicata
que dispara o merge, e é reversível por `UnlinkChrono`.

`ErrVinculoDuplicadoAparelho`, `DescVinculoDuplicadoAparelho` e o branch correspondente em
`writeLinkErr` ficam órfãos: remover junto (`errors.go`).

### 3. Opcional — `EmployeePhotoForStore` (mesmo arquivo, 1 linha)

Bug adjacente encontrado na exploração: a rota escopa **só** pela ponte
(`id IN (SELECT rh_employee_id FROM rh_chrono_employees ...)`), sem o `OR store_id = ?`
que `pullEmployees` ganhou na migration 0008. Funcionário novo, nunca vinculado, vem com
`photoHash` no pull mas recebe **404** ao baixar a foto — e `SyncManager.baixarFotos`
retenta a cada rodada de sync, para sempre.

Alinhar o `WHERE` ao de `pullEmployees`. **Descartável sem prejuízo para este plano.**

---

## App — `d:\BUSINESS\ChronoPass`, branch `feat/integração-summus`

### 4. `app/src/main/java/com/chronopass/app/sync/SyncRules.kt`

Campo novo com default, para não tocar em nenhum call site existente:

```kotlin
data class SummusEmployee(
        ...
        val photoHash: String? = null,
        val mergeUids: List<String> = emptyList(),
)
```

### 5. `app/src/main/java/com/chronopass/app/sync/PullPayloads.kt`

Em `employee(o)`, ler o array. Ausente → lista vazia (servidor velho não quebra o app novo):

```kotlin
mergeUids = o.optJSONArray("mergeUids")?.let { a ->
    (0 until a.length()).map { a.getString(it) }
} ?: emptyList(),
```

### 6. `app/src/main/java/com/chronopass/app/data/dao/Daos.kt`

Uma query em `PunchDao`. `punch.employeeId` é um `Long` solto — não há `@ForeignKey` nem
`ON DELETE` no schema, então o `UPDATE` é direto:

```kotlin
@Query("UPDATE punch SET employeeId = :para WHERE employeeId = :de")
suspend fun repointEmployee(de: Long, para: Long)
```

### 7. `app/src/main/java/com/chronopass/app/data/repo/ChronoRepository.kt`

O merge fica **dentro do seam anti-eco** — escreve nos DAOs sem `enfileirar()`. Sair do
seam devolveria o merge para cima no próximo sync.

`applyFromSummus` passa a levar a linha canônica adiante em vez de descartá-la
(`employees.insert` já devolve `Long`):

```kotlin
for (s in funcionarios) {
    val local = employees.employeeByUid(s.uid)
    val canonica = when {
        local != null -> SyncRules.mergeEmployee(local, s).also { employees.update(it) }
        s.deleted -> continue
        else -> SyncRules.novoEmployee(s).let { it.copy(id = employees.insert(it)) }
    }
    absorver(canonica, s.mergeUids)
}
```

E o método novo, privado:

```kotlin
// Vínculo confirmado no Summus: as batidas da linha duplicada passam para a canônica e a
// duplicada vai para a lixeira (nunca DELETE — vínculo errado continua recuperável).
private suspend fun absorver(canonica: Employee, uids: List<String>) {
    var foto = canonica.photoPath
    for (uid in uids) {
        val dup = employees.employeeByUid(uid) ?: continue
        if (dup.id == canonica.id || dup.deleted) continue
        punches.repointEmployee(dup.id, canonica.id)
        if (foto == null) foto = dup.photoPath
        employees.softDelete(dup.id)
    }
    if (foto != canonica.photoPath) employees.update(canonica.copy(photoPath = foto))
}
```

Pontos que o código acima resolve de propósito:

- **Idempotente.** `dup.deleted` corta na segunda passada. Como o merge não sobe
  (seam anti-eco), `rh_chrono_employees.deleted` continua `false` no servidor e o uid
  segue vindo em `mergeUids` todo pull — sem a guarda seriam duas queries no-op por
  funcionário por sync.
- **Ordem.** A canônica é criada/atualizada antes de absorver. Se o vínculo foi feito
  antes do primeiro pull, a canônica é inserida e a local é absorvida na mesma passada.
- **Foto.** Se o RH não tem foto e a linha local tem, a foto de cadastro é carregada para a
  canônica — senão o rosto sumiria do PDF.
- **Cursor.** `applyPull` só avança o cursor depois das escritas. Falha no meio → o
  servidor reenvia a mesma janela.

---

### 8. `app/src/main/java/com/chronopass/app/ui/screens/EmployeesScreen.kt` — barrar nome repetido no cadastro

Pedido à parte, independente do merge: não deixar criar pela tela um funcionário com o
mesmo nome de um que já existe.

A tela já carrega a lista (`val employees by vm.allEmployees.collectAsState()`), então
isso **não precisa de DAO, query nem mudança no ViewModel** — é a lista que já está na
composição.

`EmployeeDialog` ganha um parâmetro e a comparação:

```kotlin
private fun EmployeeDialog(
        employee: Employee?,
        nomesEmUso: List<String>,
        ...
) {
    ...
    val nomeRepetido = nomesEmUso.any { it.trim().equals(name.trim(), ignoreCase = true) }
```

No campo Nome, `isError = nomeRepetido` + `supportingText` com
`"Já existe um funcionário com esse nome"`; e o botão vira
`enabled = name.isNotBlank() && !nomeRepetido`.

Nos dois call sites, a edição exclui a própria linha para não se acusar ao renomear:

```kotlin
EmployeeDialog(null, nomesEmUso = employees.map { it.name }) { ... }            // criar
EmployeeDialog(e, nomesEmUso = employees.filter { it.id != e.id }.map { it.name }) { ... }  // editar
```

Simplificações deliberadas, marcar com comentário `ponytail:`:

- **Só `trim` + `ignoreCase`**, sem dobra de acento — "João" e "Joao" passam como nomes
  distintos. O `normalizeNameForMatch` do servidor (`bridge.go`) faz a dobra completa;
  trazer isso para o app é código novo para um caso que a tela de vínculos já resolve.
- **`vm.allEmployees` não inclui a lixeira** (`deleted = 0`), de propósito: bloquear por um
  nome invisível seria confuso.
- **Só a tela.** A descida do Summus continua podendo trazer um homônimo — é o que o
  vínculo + merge (itens 1-7) existe para resolver. Não há validação equivalente no
  `applyFromSummus`, e nem deve haver: casar por nome automaticamente é exatamente a
  decisão que o projeto rejeitou ("nada casa sozinho").

Sem teste: é um predicado de uma linha dentro do Composable.

---

## Testes

| Arquivo | O que cobrir |
|---|---|
| `apps/server/internal/modules/rh/pull_module_test.go` | `mergeUids` traz o uid local vinculado e **não** traz o uid canônico; funcionário sem vínculo vem com lista vazia |
| `apps/server/internal/modules/rh/bridge_test.go` / `chrono_autolink_test.go` | O teste que hoje afirma **409** no vínculo duplicado por aparelho passa a afirmar sucesso — é a mudança de comportamento do item 2 |
| `app/src/test/.../data/repo/ApplyFromSummusTest.kt` | Batidas movidas para a canônica; duplicata com `deleted=1`; **nada na outbox** (mesmo padrão de `applyFromSummus_naoEnfileiraNaOutbox`); segunda aplicação do mesmo pull não muda nada; foto da local herdada quando a canônica não tem |
| `app/src/test/.../sync/PullPayloadsTest.kt` | `mergeUids` parseado; **ausente → lista vazia** (compat com servidor velho) |

Os fakes puros de DAO que `ApplyFromSummusTest` já usa (via o
`ChronoRepository internal constructor(employees, punches, stores, settings, outbox)`)
precisam ganhar `repointEmployee`. Sem Room, sem Robolectric.

---

## Verificação

**Servidor** (`d:\BUSINESS\SummusBackoffice`)

```
task go:test                 # unitários
task db:dev:up && task go:test:it   # integração contra Postgres real
task check                   # o que o CI roda
```

**App** (`d:\BUSINESS\ChronoPass`)

```
scripts\test.bat             # ou ./gradlew test — JVM pura, sem emulador
scripts\contract-test.bat    # contrato de subida/descida
```

**Ponta a ponta** — reproduz o cenário relatado:

1. `task db:dev:up`, sobe o servidor, cria loja + api-key com `store_id`.
2. App (`scripts\dev.bat`): cadastra "João Silva", **bate uma Entrada e uma Saída**,
   sincroniza. Confere no Summus: `rh_chrono_employees` com `rh_employee_id = NULL`.
3. Summus: cria "João Silva" na mesma loja. Sincroniza o app.
   → confirma o bug: **duas linhas** na tela de ponto.
4. **Bate ponto na linha nova também** — é o caso que exige mover batidas, não só esconder linha.
5. Aba **Vínculos**: vincula a linha local ao "João Silva" do RH.
   → antes do item 2 isso devolvia **409**; agora tem que passar.
6. Sincroniza o app.
   → **uma linha só** na tela de ponto, com **as batidas dos dois passos (2 e 4) juntas**.
7. Relatório PDF do João: confere que aparecem todas as marcações.
8. Sincroniza de novo → nada muda (idempotência).
9. Lixeira do admin: a linha absorvida está lá, recuperável.
10. **Validação de nome (item 8):** em Funcionários, tenta criar outro "João Silva" — o botão
    SALVAR fica desabilitado e o campo mostra o erro. Tenta "joão silva  " (caixa e espaços)
    → mesmo bloqueio. Abre a edição do próprio João e salva sem mudar o nome → **tem que
    deixar** (não pode se acusar).

**Ordem de deploy:** servidor primeiro (aditivo — APK antigo ignora `mergeUids`), APK
depois. O app tem auto-update por GitHub Releases, então `scripts\release.bat` distribui.

---

## Dívida registrada (fora deste escopo)

`rh_chrono_punches` não guarda id de funcionário — só `employee_name`/`employee_role`
denormalizados, sem FK. `ListChronoPunches` e o `PunchesTab` filtram por
`employee_name ILIKE '%...%'`.

Consequências, que **já existem hoje e não pioram com o merge**:

- Se as duas linhas tinham nomes ligeiramente diferentes ("João Silva" × "Joao da Silva"),
  o relatório do Summus continua partido em dois mesmo depois do merge no app. O relatório
  **do app** fica correto, porque lá as batidas foram movidas de verdade.
- As batidas já sincronizadas mantêm o `employee_name` congelado. Só convergem quando a
  batida é editada (a correção sobe com o nome novo e `archiveAndUpdatePunch` arquiva a
  revisão anterior).

Correção futura: coluna `employee_uid` em `rh_chrono_punches`, `employeeUid` no contrato de
subida, e agrupamento pela ponte em vez de por nome — migration + contrato + duas telas do
backoffice.
