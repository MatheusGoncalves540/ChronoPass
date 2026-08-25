# ChronoPass — App Android simples de registro de ponto

## 1. Objetivo

Aplicativo Android simples para uma pequena loja registrar a entrada e saída dos funcionários.

Cada marcação deve armazenar:

* funcionário;
* data e horário;
* localização GPS;
* foto tirada no momento da marcação;
* tipo da marcação: entrada ou saída.

O aplicativo funciona **100% offline**. Os dados ficam armazenados localmente no telefone.

Não haverá reconhecimento facial, servidor, sincronização online ou processamento de inteligência artificial.

---

## 2. Fluxo principal

O funcionamento deve ser extremamente simples:

1. Funcionário abre o aplicativo.
2. Seleciona seu nome.
3. O aplicativo mostra se a próxima marcação será **Entrada** ou **Saída**.
4. Funcionário toca em **Registrar ponto**.
5. O aplicativo solicita/acessa a localização.
6. Abre a câmera.
7. Funcionário tira uma foto.
8. O aplicativo salva a marcação.
9. A tela mostra a confirmação.

Exemplo:

> João da Silva
> Entrada
> 08:03
> 📍 Loja — localização registrada
> 📷 Foto registrada

A foto não precisa identificar automaticamente a pessoa. Ela existe como **evidência visual da marcação**.

---

## 3. Dados armazenados

Cada marcação deve possuir:

```text
punch
├── id
├── employeeId
├── timestamp
├── type
├── latitude
├── longitude
├── accuracy
├── photoPath
└── createdAt
```

### Funcionário

```text
employee
├── id
├── name
├── code
├── active
└── createdAt
```

### Tipo da marcação

```text
IN
OUT
```

A próxima marcação é determinada pela última marcação daquele funcionário.

Se não houver marcação no dia:

```text
→ ENTRADA
```

Se a última marcação for entrada:

```text
→ SAÍDA
```

Se a última for saída:

```text
→ ENTRADA
```

O sistema não precisa limitar a quantidade de marcações por dia.

---

## 4. Foto

A câmera deve ser aberta no momento da marcação.

A foto é vinculada diretamente ao registro:

```text
punch.photoPath
```

A imagem deve ficar armazenada no próprio telefone, dentro do armazenamento privado do aplicativo.

Exemplo:

```text
/files/punches/
    2026-08-25_08-03-12.jpg
    2026-08-25_17-42-31.jpg
```

Não é necessário manter uma foto de cadastro do funcionário.

Também não é necessário analisar ou comparar rostos.

A foto é apenas uma fotografia feita no momento do registro.

---

## 5. Localização

No momento da marcação, o aplicativo obtém a localização atual do telefone.

Devem ser armazenados:

```text
latitude
longitude
accuracy
```

A localização fica vinculada ao ponto.

Exemplo:

```text
João da Silva
Entrada
25/08/2026 08:03:12

Latitude: -23.XXXX
Longitude: -46.XXXX
Precisão: 8 metros
```

O aplicativo deve solicitar permissão de localização somente quando necessário.

Se não conseguir obter localização, o comportamento pode ser definido nas configurações:

* permitir registrar mesmo sem GPS; ou
* impedir a marcação até conseguir localização.

Para uma primeira versão, recomendo **permitir a marcação**, mas mostrar que a localização não foi obtida.

---

## 6. Localização da loja

O administrador pode cadastrar a localização da loja.

```text
store
├── id
├── name
├── latitude
├── longitude
└── radius
```

Exemplo:

```text
Loja Principal
Latitude: -23.XXXX
Longitude: -46.XXXX
Raio permitido: 100 metros
```

O aplicativo pode calcular a distância entre o telefone e a loja.

Se estiver dentro do raio:

```text
✓ Dentro da loja
```

Se estiver fora:

```text
⚠ Você está a 450 metros da loja
```

A primeira versão pode apenas registrar essa informação, sem bloquear o ponto.

---

## 7. Banco de dados

Não é necessário SQLCipher ou uma arquitetura complexa.

Pode ser utilizado:

**Room + SQLite**

Tabelas:

```text
employee
punch
store
app_settings
```

O banco fica exclusivamente no telefone.

Não haverá servidor.

---

## 8. Segurança

Como o aplicativo é simples, a segurança pode ser proporcional ao projeto.

Não é necessário implementar:

* Argon2id;
* MobileFaceNet;
* embeddings;
* liveness;
* anti-spoofing;
* reconhecimento facial;
* sistema complexo de chaves;
* sincronização;
* arquitetura de múltiplos módulos.

Porém, as fotos e os dados do aplicativo devem ficar no armazenamento privado do Android e não devem ser salvos na galeria pública.

Também é recomendável:

* impedir que o aplicativo seja facilmente acessado por outros funcionários;
* possuir uma área administrativa protegida por senha;
* não registrar dados pessoais em logs;
* permitir excluir funcionário e seus dados.

---

## 9. Área do funcionário

A tela inicial deve ser praticamente um terminal de ponto.

### Tela inicial

```text
ChronoPass

[ Buscar funcionário ]

João da Silva
Maria Oliveira
Carlos Santos
Ana Souza
```

Ao selecionar:

```text
João da Silva

Próxima marcação:

ENTRADA

[ REGISTRAR PONTO ]
```

Depois:

```text
Tire uma foto para registrar o ponto.

[ CÂMERA ]

[ TIRAR FOTO ]
```

Após tirar:

```text
Confirmar marcação?

João da Silva
Entrada
08:03:12

Localização encontrada
Precisão: 8m

[ CONFIRMAR ]
[ TIRAR NOVA FOTO ]
```

Depois:

```text
✓ Ponto registrado

João da Silva
Entrada
08:03

Foto salva
Localização salva
```

---

## 10. Área administrativa

A área administrativa pode ser acessada através de um botão discreto ou gesto na tela inicial.

Proteção:

```text
Senha do administrador
```

O administrador poderá:

### Funcionários

* cadastrar;
* editar;
* desativar;
* excluir.

### Marcações

Visualizar:

```text
Data
Funcionário
Entrada/Saída
Horário
Localização
Foto
```

Filtros:

* hoje;
* ontem;
* período;
* funcionário.

Ao abrir uma marcação:

```text
João da Silva

Entrada
25/08/2026 08:03:12

Localização:
-23.XXXX, -46.XXXX

Precisão:
8 metros

[ FOTO ]

[ VER NO MAPA ]
```

---

## 11. Correção de marcações

O administrador poderá corrigir uma marcação.

Exemplo:

```text
João esqueceu de registrar a saída.

[ ADICIONAR MARCAÇÃO ]
```

Ao fazer uma alteração, guardar:

```text
alterado por
data da alteração
motivo
```

Não é necessário criar inicialmente um sistema completo de auditoria.

Um simples histórico de alterações já é suficiente.

---

## 12. Relatórios

O aplicativo deve permitir visualizar o ponto por:

* funcionário;
* dia;
* semana;
* mês;
* período personalizado.

Exemplo:

```text
João da Silva

25/08/2026

08:03 — Entrada
17:42 — Saída

Total: 09:39
```

Também deve calcular:

* primeira entrada;
* última saída;
* quantidade de marcações;
* total de horas registradas.

O sistema apenas registra e soma os horários.

Não deve tentar calcular regras trabalhistas complexas nesta primeira versão.

---

## 13. Exportação

O administrador poderá exportar os dados.

### CSV

Para utilização em Excel ou outro sistema.

Exemplo:

```csv
Funcionário,Data,Tipo,Horário,Latitude,Longitude
João da Silva,25/08/2026,Entrada,08:03:12,-23.XXXX,-46.XXXX
João da Silva,25/08/2026,Saída,17:42:31,-23.XXXX,-46.XXXX
```

### PDF

Um relatório simples:

```text
CHRONOPASS
Espelho de ponto

Funcionário: João da Silva
Período: 01/08/2026 - 31/08/2026

Data       Entrada    Saída       Horas
01/08      08:02      17:41       09:39
02/08      08:05      17:38       09:33
...
```

---

## 14. Backup

Como os dados ficam somente no telefone, deve existir uma opção simples de backup.

O administrador poderá:

```text
[ EXPORTAR BACKUP ]
```

O aplicativo gera um arquivo contendo:

* funcionários;
* marcações;
* configurações;
* fotos.

Exemplo:

```text
chronopass-backup-2026-08-25.zip
```

O usuário poderá salvar o arquivo em outro local.

Também deve existir:

```text
[ RESTAURAR BACKUP ]
```

Não é necessário implementar nuvem nesta versão.

---

## 15. Tecnologia

### Android

```text
Kotlin
Jetpack Compose
Material 3
```

### Banco

```text
Room
SQLite
```

### Câmera

```text
CameraX
```

### Localização

```text
Fused Location Provider
```

### Relatórios

```text
CSV
PdfDocument
```

### Arquivos

Armazenamento privado do aplicativo.

Não utilizar:

* servidor;
* Firebase;
* reconhecimento facial;
* ML Kit Face Detection;
* TensorFlow Lite;
* MobileFaceNet;
* Redis;
* API externa.

---

## 16. Estrutura do projeto

Para uma aplicação pequena, não é necessário criar quatro módulos Gradle.

Uma estrutura simples é suficiente:

```text
app/
├── data/
│   ├── database/
│   ├── dao/
│   ├── entities/
│   └── repositories/
│
├── camera/
│
├── location/
│
├── reports/
│
├── backup/
│
├── admin/
│
├── ui/
│   ├── home/
│   ├── punch/
│   ├── employees/
│   ├── records/
│   └── settings/
│
└── MainActivity.kt
```

---

## 17. Fora de escopo

A primeira versão não terá:

* reconhecimento facial;
* cadastro de biometria;
* inteligência artificial;
* servidor;
* login online;
* sincronização;
* múltiplas lojas;
* folha de pagamento;
* banco de horas complexo;
* escalas;
* horas extras;
* integração com sistemas externos;
* AFD / Portaria 671;
* notificações;
* painel web.

Esses recursos podem ser adicionados posteriormente sem mudar o conceito principal do aplicativo.

---

## 18. MVP

A primeira versão funcional deve possuir somente:

### Funcionários

* [ ] Cadastrar funcionário
* [ ] Editar funcionário
* [ ] Desativar funcionário

### Registro

* [ ] Selecionar funcionário
* [ ] Identificar entrada/saída
* [ ] Tirar foto
* [ ] Obter localização
* [ ] Salvar horário
* [ ] Salvar foto
* [ ] Salvar localização

### Consulta

* [ ] Ver marcações
* [ ] Filtrar por funcionário
* [ ] Filtrar por período
* [ ] Visualizar foto
* [ ] Visualizar localização

### Administração

* [ ] Senha de administrador
* [ ] Corrigir marcações
* [ ] Excluir marcações
* [ ] Configurar localização da loja

### Relatórios

* [ ] Exportar CSV
* [ ] Gerar PDF
* [ ] Fazer backup
* [ ] Restaurar backup

---

## 19. Fluxo completo do MVP

```text
                 ┌──────────────────┐
                 │      ABRIR       │
                 │      APP         │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │ Selecionar       │
                 │ funcionário      │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │ Entrada ou Saída │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │ Tirar fotografia │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │ Obter localização│
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │    Confirmar     │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │ Salvar marcação  │
                 │ + foto           │
                 │ + localização    │
                 │ + horário        │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │     Concluído    │
                 └──────────────────┘
```

## 20. Princípio do projeto

O aplicativo deve ser tratado como um **livro de ponto digital com evidência fotográfica e geográfica**, e não como um sistema biométrico.

A cada marcação, o sistema simplesmente responde:

> **Quem registrou, quando registrou, onde estava e qual foto foi tirada naquele momento?**

Essa abordagem elimina praticamente toda a complexidade do projeto original e deixa uma base pequena o suficiente para ser implementada, testada e utilizada por uma loja real.
