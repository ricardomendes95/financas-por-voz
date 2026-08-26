# CLAUDE.md — Regras do projeto "Finanças por Voz"

App Android nativo de finanças pessoais com entrada por voz em português brasileiro.
Uso pessoal, single-user, offline-first, sideload (sem Play Store).

Leia `spec-app-financas-voz.md` para o requisito completo. Este arquivo contém as
regras que valem para **toda** sessão de trabalho.

---

## Contexto do dispositivo-alvo

Galaxy S23, One UI 8.5 (Android 16), Brasil, pt-BR. Gemini é o assistente ativo
e deve continuar sendo. Consequência: **o app nunca depende do Gemini para
funcionar**. AppFunctions é bônus, não fundação.

---

## Stack (não alterar sem discussão)

- Kotlin 2.x + KSP
- minSdk 26, targetSdk 36, compileSdk 36
- Jetpack Compose + Material 3 (nada de XML layouts, nada de Fragments)
- Room + DataStore
- Hilt
- Coroutines + Flow (StateFlow na UI; **nunca** LiveData)
- Glance para widget
- WorkManager
- java.time (disponível nativo desde API 26 — não precisa desugaring)

**Proibido:** RxJava, Retrofit, Firebase, MPAndroidChart, React Native, qualquer
dependência de rede no caminho crítico.

---

## Estrutura de módulos

```
:app                      DI, navegação, MainActivity, tema
:core:model               data classes puras
:core:database            Room, DAOs, migrations
:core:data                repositories, mappers
:core:designsystem        tokens e componentes
:core:common              utils, formatadores, Clock
:feature:dashboard
:feature:transactions
:feature:reports
:feature:categories
:feature:budgets
:feature:settings
:feature:voice            captura + QuickEntryGateway + as 8 rotas
:feature:widget
:nlu                      parser pt-BR — Kotlin puro, ZERO Android
:integration:appfunctions
:integration:notifications
```

---

## Regras invioláveis

### Dados
1. **Dinheiro é `Long` em centavos.** Nunca `Double`, nunca `Float`, nunca `BigDecimal`
   persistido. Formatação só na camada de UI.
2. `occurredAt` (quando o fato aconteceu) ≠ `createdAt` (quando foi registrado).
   Todos os relatórios usam `occurredAt`.
3. `yearMonth: Int` no formato `YYYYMM` é coluna desnormalizada e indexada.
   Consultas mensais **nunca** usam funções de data no SQL.
4. Migrations versionadas desde a v1. `fallbackToDestructiveMigration()` é proibido em release.
5. Toda agregação acontece em SQL (`SUM`, `GROUP BY`), nunca em Kotlin.

### Performance
6. Nenhuma I/O em `Application.onCreate()`.
7. Todo `Flow` exposto por ViewModel usa `.distinctUntilChanged()` e
   `.stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)`.
8. Todo `items()` de `LazyColumn` tem `key` estável.
9. Cálculo derivado dentro de composable usa `derivedStateOf`.
10. Antes de dar merge numa query de relatório, rode `EXPLAIN QUERY PLAN`.
    Nenhuma pode fazer `SCAN TABLE`.

### Voz
11. **Todas as rotas de entrada chamam `QuickEntryGateway.ingest(rawText, source)`.**
    Nenhuma rota implementa parsing próprio. Se você está escrevendo regex fora
    do módulo `:nlu`, está errado.
12. Lançamento por voz **nunca abre tela de confirmação**. Grava direto e emite
    notificação com Desfazer / Editar / Categoria.
13. O módulo `:nlu` não importa nada de `android.*`. Ele roda em JVM pura e é
    testado com JUnit sem Robolectric.

### Privacidade
14. Zero rede por padrão. Nenhum valor monetário, descrição ou texto de
    notificação bancária em `Log.*` em build de release.
15. `android:allowBackup="false"`.
16. Notificação bancária **nunca** vira lançamento automático — só sugestão pendente.

---

## Ordem de trabalho (roadmap)

| Fase | Entrega |
|---|---|
| 1 | `:core:model` + `:core:database` + CRUD manual + Dashboard básico |
| 2 | `:nlu` completo **com testes escritos antes do parser** |
| 3 | `QuickEntryGateway` + widget + QS tile + notificação RemoteInput + deep link |
| 4 | `SpeechRecognizer` próprio + papel de assistente (opt-in) |
| 5 | Relatórios + `InsightEngine` |
| 6 | Orçamentos + fechamento mensal |
| 7 | AppFunctions + App Actions (timebox: meio dia) |
| 8 | Notification listener bancário |
| 9 | Baseline profile, acessibilidade, animações |

Ao concluir uma fase: liste o que foi feito, rode os testes, e **peça confirmação
antes de seguir para a próxima**.

---

## Convenções de código

- Nomes de classes, funções e variáveis em **inglês**. Strings de UI em **pt-BR**
  via `strings.xml`.
- Um `UiState` imutável por tela, exposto como `StateFlow`.
- Eventos one-shot via `Channel` → `receiveAsFlow()`, nunca dentro do `UiState`.
- Repositories devolvem `Flow`, nunca `suspend fun` que retorna lista estática,
  exceto para operações pontuais de escrita.
- Nenhum `Context` acima da camada `:core:data`.
- `internal` por padrão nos módulos; `public` só no que cruza fronteira de módulo.

## Testes

- `:nlu` — JUnit5 puro. O corpus de `FinanceTextParserTest` é o contrato.
  **Meta: ≥ 92% de acerto.** Se uma mudança derrubar o corpus, a mudança está errada.
- `:core:database` — Room in-memory, testar cada migration.
- ViewModels — Turbine.
- Compose — apenas nas telas de Dashboard e Lançamentos.

## Comandos úteis

```bash
./gradlew :nlu:test                    # rodar corpus do parser
./gradlew assembleDebug
./gradlew lint
adb shell cmd app_function list-app-functions | grep -A 12 br.com.financas
adb shell am start -a android.intent.action.VIEW -d "financas://add?text=20%20reais%20de%20pastel"
```
