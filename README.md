# ChronoPass

Livro de ponto digital para celular: o funcionário registra **entrada e saída**
com **foto** tirada na hora e **localização** (GPS). Funciona **100% sem internet** —
os dados ficam guardados no próprio aparelho, nunca num servidor.

A cada marcação, o app responde a quatro perguntas: **quem registrou, quando,
onde estava e qual foto foi tirada naquele momento.** Não é um sistema de
reconhecimento facial — é um livro de ponto com prova em foto e localização.

| | |
|---|---|
| Versão atual | 2.2.0 (versionCode 4) |
| Requer | Android 8.0 ou superior (sem internet para o uso diário) |
| Documentação | ver abaixo |

## Documentação (escolha pelo seu papel)

| Documento | Para quem | Conteúdo |
|---|---|---|
| [**Manual Operacional**](MANUAL-OPERACIONAL.md) | Dono, gerente e funcionários da loja | Como usar o app no dia a dia em linguagem simples, sem termos técnicos: bater ponto, cadastrar funcionário, relatórios, backup, dúvidas frequentes |
| [**Guia Técnico**](tech-docs/GUIA-TECNICO.md) | Desenvolvedores | Estrutura do código, build, testes, permissões, assinatura de release e documentos de especificação |

### Documentos complementares (histórico e planos)

- [`docs/PLANO.md`](docs/PLANO.md) — especificação original e checklist do MVP (versão 1).
- [`docs/PLANO-FUTURO.md`](docs/PLANO-FUTURO.md) — visão futura (reconhecimento facial, criptografia, auditoria).
- [`SUMUS-INTEGRACAO.md`](SUMUS-INTEGRACAO.md) — plano de sincronização com o SummusBackoffice (contrato técnico, nada implementado ainda).

---

Em poucas palavras, o ChronoPass é:

- **Simples para o funcionário** — escolher o nome e registrar o ponto em poucos toques.
- **Com prova** — foto do momento + coordenadas de onde estava.
- **Seguro para a loja** — área do gerente protegida por senha; correções ficam registradas com motivo.
- **Confiável** — dados no armazenamento privado do app (não vão para a galeria) e backup em um arquivo só.
