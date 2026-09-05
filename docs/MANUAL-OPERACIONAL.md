# ChronoPass — Manual Operacional

**Para quem usa o app no dia a dia** (dono, gerente e funcionários da loja).
Escrito em linguagem simples, sem termos técnicos. Para quem mexe no código do
app, existe o [Guia Técnico](tech-docs/GUIA-TECNICO.md).

---

## 1. O que é o ChronoPass

O ChronoPass é o **livro de ponto digital** da loja, usado no celular. Cada vez
que um funcionário registra **entrada** ou **saída**, o app guarda:

- **quem** registrou;
- **quando** (dia e horário);
- **onde** estava (posição do celular);
- **uma foto** tirada naquele momento, como comprovante.

Ele **não** reconhece rostos e **não** exige internet para funcionar: todos os
registros ficam salvos **dentro do próprio celular** e nada é enviado para fora.

> Importante: como os dados vivem no celular, **se o aparelho for perdido,
> quebrado ou trocado sem um backup, o histórico some**. Veja a seção
> [Backup](#7-backup-exportar-e-restaurar).

## 2. Instalação e primeira configuração

A instalação é feita uma única vez, por quem cuida do celular da loja, usando o
arquivo de instalação (`.apk`) fornecido pelo desenvolvedor. Durante a instalação,
o Android pode pedir autorização para "instalar apps de fora da loja de apps" —
é normal, é só permitir para este app.

Ao abrir o app pela primeira vez, ele pede permissões que **só são usadas na hora
de bater o ponto**:

| O app pede | Para quê |
|---|---|
| Câmera | Tirar a foto no momento do registro |
| Localização | Anotar onde o celular estava no momento do registro |
| Internet | Somente avisar quando existir uma versão nova do app |

### Primeira configuração (responsável pela loja)

1. Na tela inicial, entre na **área do gerente** (senha inicial: `1234`).
2. **Troque a senha** (Configurações → Nova senha) — a senha inicial é padrão e
   qualquer pessoa pode conhecê-la.
3. Cadastre os **funcionários** com nome e foto.
4. Em **Configurações**, informe a **localização da loja** e o **raio permitido**
   (a distância, em metros, em torno da loja considerada "dentro").

## 3. Dia a dia: bater o ponto

1. O funcionário abre o app e toca no **próprio nome**.
2. O app mostra qual será o próximo registro: **Entrada** ou **Saída** (ele
   alterna sozinho conforme o último registro do dia).
3. Toque em **Registrar ponto**.
4. O app abre a **câmera** — tire a foto.
5. Confirme. O registro é salvo com foto, horário e localização.

Na confirmação o app mostra se você está **dentro da loja** ou a **quantos metros**
está dela. Isso é só um aviso: mesmo longe ou sem conseguir a posição, o registro
pode ser feito — e o próprio app informa quando a localização **não** foi obtida.

**Regras simples que o app segue:**
- O primeiro registro do dia é sempre **Entrada**; depois os registros se
  alternam (Entrada → Saída → Entrada → ...).
- Não há limite de registros por dia (por exemplo, entrada, saída para o almoço,
  volta do almoço, saída).

### Almoço e pausa mínima

O app usa os registros para entender a **jornada** e o **intervalo (almoço)**:

- **Horas trabalhadas** = soma dos períodos entre uma entrada e uma saída.
- **Almoço** = soma dos períodos entre uma saída e a próxima entrada.
- O app considera a pausa **curta demais** quando ela fica abaixo do mínimo: **1
  hora** para jornadas acima de 6 horas, ou **30 minutos** para jornadas de 4 a 6
  horas.
- Quando a pausa fica abaixo desse mínimo, o relatório em PDF mostra um `*` no
  dia e um aviso no rodapé. Isso **não impede** o registro — é apenas um aviso
  visível no relatório.

> Dica: os registros usam o relógio do celular. Mantenha a data e a hora do
> aparelho corretas para os horários baterem certo.

## 4. Área do gerente

Na tela inicial existe um acesso reservado protegido por **senha**. Lá o gerente
pode administrar funcionários, registros, configurações e relatórios.

> A senha inicial é `1234`. Troque-a em **Configurações** — ela protege os dados
> da loja.

### Funcionários

- **Cadastrar** funcionário (nome + foto de identificação).
- **Editar** dados ou trocar a foto.
- **Desativar** (não aparece mais na lista para bater ponto, mas o histórico é
  mantido).
- **Excluir** — o funcionário vai para a **lixeira** e o histórico de registros
  continua guardado.

### Registros (marcações)

- **Ver** todos os registros, com data, horário, foto e localização.
- **Filtrar** por funcionário e por período (hoje, ontem, intervalo de datas...).
- **Corrigir** um registro — por exemplo, quando o funcionário esqueceu de
  marcar a saída. Toda correção exige um **motivo** e fica anotado **quem**
  corrigiu, **quando** e **por quê**.
- **Excluir** um registro errado.

### Configurações

- Trocar a **senha do gerente**.
- Definir a **localização da loja** e o **raio permitido** (em metros).

## 5. Relatórios

Os relatórios são por **funcionário** e por **período**. Períodos prontos: **este
mês, mês passado, últimos 7 dias, últimos 30 dias** — ou você escolhe as **datas**
exatas.

### PDF — o "espelho de ponto"

Um documento pronto para **imprimir e assinar**, com:

- a **logo da loja** (grande, no topo) e a **foto do funcionário**;
- dados do funcionário e do período escolhido;
- uma tabela por dia: **Data | Entrada | Saída | Almoço | Horas**, com o total;
- a lista de **alterações** feitas (com o motivo) e o aviso de almoço abaixo do
  mínimo (o `*`);
- blocos de **assinatura** (funcionário e gerente);
- numeração automática de páginas ("Página X de Y").

### Planilha (CSV) — para Excel

Um arquivo de planilha com **uma linha por registro**, pronto para abrir no Excel
e montar suas próprias análises. Colunas:

`Funcionário | Data | Tipo (Entrada/Saída) | Horário | Latitude | Longitude |
Precisão | Almoço | Almoço insuficiente | Motivo da alteração`

## 6. Onde ficam as fotos e os dados

Fotos e registros ficam **guardados dentro do app**, no espaço privado do celular:
eles **não** aparecem na galeria e **não** são enviados para nenhum serviço fora
do aparelho. Só o app consegue acessá-los — e o backup (abaixo) é a única forma
de levá-los para outro lugar.

## 7. Backup: exportar e restaurar

O backup junta **tudo** (funcionários, registros, configurações e fotos) em um
**único arquivo** que você escolhe onde guardar (no próprio celular, num e-mail,
num computador, num pendrive...).

- **Exportar backup** — gere o arquivo e guarde-o em um lugar seguro.
- **Restaurar backup** — o app **apaga os dados atuais** e recarrega tudo o que
  estava no arquivo. Só faça isso quando quiser mesmo voltar àquele estado (por
  exemplo, num celular novo).

> **Regra de ouro:** sem backup, celular perdido, quebrado ou trocado significa
> **histórico perdido**. Faça backups periódicos (por exemplo, uma vez por mês ou
> sempre que algo importante mudar).

## 8. Atualizações do app

Quando o celular está com internet, o app **verifica sozinho** (ao abrir) se
existe uma versão nova e oferece a instalação. O download e a instalação são
feitos de forma segura: se a internet cair no meio, nada de errado acontece — o
arquivo incompleto é descartado e nada é instalado pela metade.

## 9. Problemas comuns (perguntas e respostas)

| Situação | O que fazer |
|---|---|
| **Esqueci a senha do gerente** | Não existe recuperação dentro do app. Se houver um backup, **restaure-o**: os dados voltam e a senha volta a ser a que estava salva naquele backup. Sem backup, a alternativa é reinstalar o app — o que **apaga os dados atuais**. |
| **Apareceu "instalar app de fora da loja de apps"** | É o próprio ChronoPass se atualizando. Permita a instalação quando o aviso aparecer. |
| **O registro ficou "Sem localização"** | A posição do celular não foi encontrada (localização desligada, sem sinal...). O registro é salvo mesmo assim e fica marcado. Verifique se a localização do celular está ligada. |
| **Está dizendo que estou fora da loja** | O app compara a posição com o endereço e o raio cadastrados. É um aviso informativo — não bloqueia o registro. Se o aviso estiver errado, confira a localização e o raio em **Configurações** (área do gerente). |
| **Funcionário esqueceu de registrar** | O gerente adiciona/corrige o registro na área do gerente, informando o **motivo**. Fica registrado quem corrigiu, quando e por quê. |
| **Foto ou dado errado num registro** | Abra o registro na área do gerente e **corrija** (com motivo) ou **exclua**. |
| **Quero conferir a frequência de um funcionário** | Gere o **relatório** por funcionário e período — em PDF (para imprimir/assinar) ou em planilha (para Excel). |
| **Cadê as fotos? Não estão na galeria** | Por segurança, elas ficam guardadas **dentro do app**. Para vê-las, abra o registro na área do gerente. |
| **O celular da loja quebrou/perdeu-se** | Sem backup, o histórico se perde. Por isso a seção [Backup](#7-backup-exportar-e-restaurar) é tão importante. |

## 10. Resumo em uma frase

> **Quem registrou, quando registrou, onde estava e qual foto foi tirada naquele
> momento** — isso é o ChronoPass: um livro de ponto digital com prova em foto e
> localização, que funciona sem internet.
