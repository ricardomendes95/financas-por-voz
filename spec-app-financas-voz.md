# Especificação Técnica Completa — App de Finanças Pessoais com Entrada por Voz (Android)

> **Documento de requisitos para implementação por IA.**
> Este documento é auto-suficiente. Implemente exatamente o que está aqui. Onde houver ambiguidade, siga a regra: *o caminho que exige menos toques do usuário vence*.

---

## 0. CONTEXTO CRÍTICO — leia antes de qualquer decisão de arquitetura

### 0.1 O problema do "OK Google"

O Google Assistant está sendo removido dos dispositivos Android a partir de **4 de setembro de 2026**, substituído pelo Gemini. Consequências diretas:

| Tecnologia | Status | Viabilidade |
|---|---|---|
| App Actions (`shortcuts.xml` + BII) | Legado, deixa de funcionar com a migração para Gemini | ⚠️ Implementar como fallback, não como base |
| Custom Intents do Assistant | Não reconhecidos pelo Gemini | ❌ Não usar |
| AppFunctions (`androidx.appfunctions`) | Alpha, Android 16+, integração com Gemini em preview restrito | ⚠️ Implementar, mas assumir indisponível |
| BIIs de Finanças (`CREATE_MONEY_TRANSFER`) | Semântica de transferência bancária, não de lançamento contábil; locale pt-BR não garantido | ❌ Semanticamente errado |
| Custom MCP no Gemini (Connected Apps) | Exige Gemini Spark, EUA, inglês, servidor HTTPS público | ❌ Indisponível no Brasil |

### 0.3 Dispositivo-alvo: Galaxy S23 (One UI 8.5 / Android 16), Brasil, pt-BR

Restrições reais confirmadas para este aparelho:

- **Android 16 presente** → a API de plataforma do AppFunctions existe. `compileSdk 36` é viável.
- **As features de IA de ponta do Galaxy S26 não foram portadas para o S23.** A capacidade do Gemini de descobrir e invocar AppFunctions de apps de terceiros não é garantida neste aparelho.
- **Custom MCP do Gemini está fora de alcance** (restrito a EUA/inglês/Spark, e exigiria expor um servidor público — o que quebraria o requisito offline-first).
- **O botão lateral já está mapeado para o Gemini** e o usuário quer manter assim.

**Consequência de projeto — não negociável:**

> O Gemini é tratado como um **canal oportunista**, não como dependência. As rotas 4, 5, 6 e 7 (widget, tile, notificação, deep link) precisam entregar a experiência completa sozinhas. Se o AppFunctions funcionar no S23, é bônus.

**Teste de realidade obrigatório antes da Fase 7.** Depois de implementar o AppFunctions, rode no aparelho conectado:

```bash
# 1. As funções foram indexadas pelo sistema?
adb shell cmd app_function list-app-functions | grep -A 12 com.seuapp.financas

# 2. Execução direta, sem passar pelo Gemini
adb shell cmd app_function execute \
  --package com.seuapp.financas \
  --id addExpense \
  --string naturalLanguageInput "20 reais de pastel no dia 24"
```

- Se **(1) falha** → o AppFunctions não está disponível no aparelho. Pare, não invista mais nessa rota.
- Se **(1) e (2) passam mas o Gemini não chama** → sua implementação está correta; falta o Gemini liberar a invocação de terceiros neste aparelho. Deixe o código pronto e siga em frente.
- **Não gaste mais de meio dia depurando isso.** É uma variável fora do seu controle.

### 0.2 Decisão arquitetural obrigatória: GATILHO DESACOPLADO

**NÃO** delegue a interpretação da fala ao assistente. O assistente (qualquer um) serve apenas como **transporte de texto bruto**. Toda a inteligência de parsing vive dentro do app.

```
┌─────────────────────────────────────────────────────────┐
│  8 ROTAS DE ENTRADA (todas convergem para o mesmo ponto) │
├─────────────────────────────────────────────────────────┤
│ 1. AppFunctions (@AppFunction)            → Gemini       │
│ 2. App Actions (shortcuts.xml capability) → Assistant    │
│ 3. VoiceInteractionService / ACTION_ASSIST → botão power │
│ 4. Widget de tela inicial com botão de microfone         │
│ 5. Quick Settings Tile (puxar barra de status)           │
│ 6. Notificação persistente com RemoteInput (ditado)      │
│ 7. Deep link: financas://add?text=...                    │
│ 8. Broadcast intent (Tasker/MacroDroid) + PROCESS_TEXT   │
└────────────────────────┬────────────────────────────────┘
                         ▼
        ┌────────────────────────────────────┐
        │   QuickEntryGateway (único ponto)  │
        │   fun ingest(rawText, source)      │
        └────────────────┬───────────────────┘
                         ▼
        ┌────────────────────────────────────┐
        │   NLU Engine (PT-BR, offline)      │
        │   texto → TransactionDraft         │
        └────────────────┬───────────────────┘
                         ▼
        ┌────────────────────────────────────┐
        │  Persistência + Notificação de     │
        │  confirmação com AÇÃO DESFAZER     │
        └────────────────────────────────────┘
```

**Regra de ouro:** o app nunca abre uma tela para confirmar um lançamento por voz. Ele grava direto e mostra uma notificação com "Desfazer" e "Editar". Confirmação por diálogo mata a experiência hands-free.

---

## 1. VISÃO GERAL

### 1.1 Objetivo do produto

App de controle financeiro pessoal, **single-user, offline-first**, cujo diferencial é registrar gastos e receitas em menos de 3 segundos por voz, e cujo propósito final é **identificar desperdício mensal**.

### 1.2 Objetivos mensuráveis (metas de aceite)

| Métrica | Meta |
|---|---|
| Tempo do fim da fala até lançamento persistido | < 800 ms |
| Acurácia do parser em frases naturais PT-BR | ≥ 92% em 200 frases de teste |
| Cold start até Dashboard interativo | < 700 ms |
| Frame drops na rolagem de 5.000 lançamentos | 0 janks (jankStats) |
| Tamanho do APK | < 15 MB |
| Funcionamento sem internet | 100% das features core |

### 1.3 Fora de escopo (não implementar)

- Sincronização em nuvem, contas de usuário, login social
- Open Finance / integração com API bancária oficial
- Multiusuário, compartilhamento familiar
- Investimentos, criptomoedas, câmbio
- Anúncios, telemetria de terceiros, analytics

---

## 2. STACK TÉCNICA OBRIGATÓRIA

```kotlin
// Linguagem e build
Kotlin 2.x, KSP
minSdk 26   // Android 8.0 — cobre notification listener e QS Tile
targetSdk 36
compileSdk 36  // exigido pelo AppFunctions

// UI
Jetpack Compose (BOM mais recente)
Material 3 (androidx.compose.material3) + Material 3 Expressive
Navigation Compose (type-safe routes)
androidx.glance:glance-appwidget  // widget

// Dados
Room (com KSP) + SQLite FTS4 para busca de descrições
DataStore Preferences (configurações)
kotlinx.serialization
kotlinx.datetime

// Assíncrono
Coroutines + Flow (StateFlow na UI, nunca LiveData)

// DI
Hilt

// Voz / agentes
androidx.appfunctions:appfunctions + appfunctions-compiler (KSP)
androidx.core:core-google-shortcuts  // App Actions legado
android.speech.SpeechRecognizer      // reconhecimento próprio, on-device

// Trabalho em background
WorkManager

// Performance
androidx.profileinstaller + Baseline Profile gerado
androidx.metrics (JankStats) apenas em debug

// Testes
JUnit5, Turbine, Room testing, Compose UI Test, Robolectric
```

**Proibido:** RxJava, Fragments/XML layouts, Retrofit (não há backend), Firebase, Google Play Services além do necessário.

### 2.1 Por que NÃO React Native / Expo neste projeto

Expo SDK 54 managed workflow é excelente para apps de tela + dados + rede. Este app não é isso. O diferencial dele está quase todo em APIs de sistema que o Expo não expõe:

| Requisito | Existe em RN/Expo? | Realidade |
|---|---|---|
| AppFunctions (`@AppFunction` + KSP + AIDL) | ❌ | Exige classe Kotlin com anotação processada em build time. Sem equivalente. |
| `VoiceInteractionService` (virar assistente do sistema) | ❌ | Serviço nativo, sem lib. |
| `NotificationListenerService` (ler notificação do banco) | ❌ | Serviço nativo, sem lib estável. |
| `TileService` (Quick Settings) | ❌ | Nativo. |
| Widget de tela inicial | ⚠️ | `react-native-android-widget` existe, mas renderiza via RemoteViews limitado e é frágil. Glance é superior. |
| `shortcuts.xml` + capabilities | ⚠️ | Só editando o manifesto/res nativo via config plugin. |
| `SpeechRecognizer` com `EXTRA_PREFER_OFFLINE` | ⚠️ | Libs de terceiros não expõem essa flag. |
| Lista de 5.000 itens sem jank | ⚠️ | FlashList ajuda, mas a bridge/JSI custa. |
| Cold start < 700 ms | ❌ | Bundle JS + Hermes raramente chega lá. |

**O cálculo decisivo:** para fazer os 8 itens acima em Expo você precisaria de **prebuild + dev client + módulos nativos escritos em Kotlin + config plugins**. Ou seja: você escreveria todo o código difícil em Kotlin **de qualquer forma**, e ainda pagaria o custo da ponte, do build híbrido e da manutenção de plugins que quebram a cada SDK.

Isso é o pior dos dois mundos. Vá 100% nativo.

### 2.2 Nota para quem vem de React/Angular

Compose é mais próximo do que parece:

| Conceito web | Equivalente Compose |
|---|---|
| Componente funcional | `@Composable fun` |
| `useState` / `signal()` | `remember { mutableStateOf() }` |
| `useMemo` / `computed()` | `remember(key) { }` / `derivedStateOf` |
| `useEffect` | `LaunchedEffect(key)` |
| Context / Provider | `CompositionLocalProvider` |
| Zustand / NGXS store | `ViewModel` + `StateFlow` |
| React Query | Repository devolvendo `Flow` do Room |
| `key` em lista | `key` no `items()` do `LazyColumn` |
| CSS-in-JS | `Modifier` encadeado |
| Reconciliação por virtual DOM | Recomposição por leitura de estado |

A diferença mental principal: em Compose a recomposição é **granular por leitura de estado**, não por re-render de árvore. Ler `state.value` dentro de um `Text` faz só aquele `Text` recompor. Por isso a regra do §11: nunca leia estado num escopo mais alto do que o necessário.

**Curva realista para um dev sênior de React/Angular: 3 a 5 dias** até estar produtivo.

---

## 3. MODELO DE DADOS (Room)

### 3.1 Entidades

```kotlin
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurredAt"]),
        Index(value = ["yearMonth"]),                 // crítico p/ consultas mensais
        Index(value = ["categoryId"]),
        Index(value = ["yearMonth", "type"]),         // índice composto p/ resumos
        Index(value = ["recurrenceGroupId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,                 // UUID
    val amountCents: Long,                      // SEMPRE em centavos, NUNCA Double
    val type: TransactionType,                  // EXPENSE | INCOME
    val description: String,                    // texto normalizado exibível
    val rawInput: String?,                      // frase original ditada (auditoria)
    val categoryId: String,
    val accountId: String,                      // padrão: "default"
    val occurredAt: Long,                       // epoch millis — data do FATO
    val createdAt: Long,                        // epoch millis — data do REGISTRO
    val yearMonth: Int,                         // 202608 — coluna desnormalizada
    val dayOfWeek: Int,                         // 1..7 — desnormalizado p/ relatórios
    val paymentMethod: PaymentMethod?,          // PIX|DEBITO|CREDITO|DINHEIRO|BOLETO|TRANSFERENCIA
    val source: EntrySource,                    // VOICE|MANUAL|NOTIFICATION|WIDGET|IMPORT|RECURRING
    val confidence: Float?,                     // 0.0–1.0 quando origem = VOICE/NOTIFICATION
    val needsReview: Boolean = false,           // true se confidence < 0.75
    val isRecurring: Boolean = false,
    val recurrenceGroupId: String?,             // agrupa assinaturas detectadas
    val merchantNormalized: String?,            // "IFOOD" a partir de "IFD *PEDIDO 4421"
    val note: String?,
    val excludeFromReports: Boolean = false     // p/ transferências internas, reembolsos
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,                           // nome do ícone Material
    val colorArgb: Int,
    val type: TransactionType,                  // ou BOTH
    val parentId: String?,                      // suporta subcategorias (1 nível)
    val isSystem: Boolean,                      // não pode ser deletada
    val sortOrder: Int,
    val archivedAt: Long?                       // soft delete
)

@Entity(tableName = "category_rules", indices = [Index("keyword", unique = true)])
data class CategoryRuleEntity(
    @PrimaryKey val id: String,
    val keyword: String,                        // normalizado: minúsculo, sem acento
    val categoryId: String,
    val weight: Int,                            // desempate quando 2 regras batem
    val isUserDefined: Boolean,                 // regras aprendidas > regras de fábrica
    val hitCount: Int = 0,                      // usado para reordenar por relevância
    val lastUsedAt: Long?
)

@Entity(tableName = "budgets", indices = [Index(value = ["yearMonth", "categoryId"], unique = true)])
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String?,                    // null = orçamento geral do mês
    val yearMonth: Int,
    val limitCents: Long,
    val rollover: Boolean                       // sobra do mês anterior acumula?
)

@Entity(tableName = "tags")
data class TagEntity(@PrimaryKey val id: String, val name: String, val colorArgb: Int)

@Entity(tableName = "transaction_tags", primaryKeys = ["transactionId", "tagId"])
data class TransactionTagCrossRef(val transactionId: String, val tagId: String)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,                           // "Nubank", "Carteira", "Inter"
    val kind: AccountKind,                      // CHECKING|CREDIT_CARD|CASH|SAVINGS
    val openingBalanceCents: Long,
    val closingDay: Int?,                       // cartão: dia de fechamento
    val dueDay: Int?,                           // cartão: dia de vencimento
    val colorArgb: Int
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val templateDescription: String,
    val amountCents: Long,
    val categoryId: String,
    val type: TransactionType,
    val dayOfMonth: Int,
    val active: Boolean,
    val detectedAutomatically: Boolean,         // sugerida pelo detector de assinaturas
    val confirmedByUser: Boolean
)

@Fts4(contentEntity = TransactionEntity::class)
@Entity(tableName = "transactions_fts")
data class TransactionFts(val description: String, val note: String?, val merchantNormalized: String?)
```

### 3.2 Regras invioláveis de dados

1. **Dinheiro sempre em `Long` de centavos.** Nunca `Double`/`Float`. Formatação apenas na camada de UI.
2. **`occurredAt` ≠ `createdAt`.** Ao dizer "gastei 20 no dia 24", `occurredAt` = dia 24, `createdAt` = agora. Todos os relatórios usam `occurredAt`.
3. **`yearMonth` desnormalizado** (Int no formato `YYYYMM`) permite consultas mensais sem funções de data no SQL — ganho enorme de performance.
4. **Soft delete** em categorias; hard delete só em lançamentos, com janela de desfazer de 10 s.
5. **Migrations versionadas** desde a v1. Nunca `fallbackToDestructiveMigration()` em release.

### 3.3 Categorias padrão (seed obrigatório)

| Categoria | Ícone | Cor sugerida | Palavras-chave iniciais |
|---|---|---|---|
| Alimentação | `restaurant` | `#F97316` | pastel, lanche, almoço, janta, mercado, padaria, açaí, pizza, ifood, rappi, comida, café, marmita, feira, hortifruti |
| Transporte | `directions_car` | `#3B82F6` | uber, 99, gasolina, combustível, ônibus, passagem, estacionamento, pedágio, mecânico, óleo, pneu |
| Moradia | `home` | `#8B5CF6` | aluguel, condomínio, energia, luz, água, gás, iptu, internet, faxina |
| Saúde | `favorite` | `#EF4444` | farmácia, remédio, consulta, exame, dentista, plano de saúde, academia |
| Entretenimento | `movie` | `#EC4899` | netflix, spotify, cinema, jogo, steam, assinatura, disney, prime, youtube premium, bar, cerveja, show |
| Educação | `school` | `#14B8A6` | curso, livro, faculdade, mensalidade, udemy, alura |
| Compras | `shopping_bag` | `#F59E0B` | roupa, tênis, celular, eletrônico, shopee, mercado livre, amazon, presente |
| Serviços | `build` | `#64748B` | assinatura, mensalidade, seguro, banco, tarifa, anuidade |
| Pets | `pets` | `#84CC16` | ração, veterinário, petshop |
| Impostos e Taxas | `receipt_long` | `#78716C` | imposto, das, inss, multa, taxa, cartório |
| Outros | `more_horiz` | `#94A3B8` | *(fallback)* |
| **Salário** (receita) | `payments` | `#22C55E` | salário, pagamento, holerite |
| **Freelance** (receita) | `work` | `#10B981` | freela, freelance, projeto, bico, pj |
| **Reembolso** (receita) | `undo` | `#06B6D4` | reembolso, estorno, devolução |
| **Outras Receitas** | `add_circle` | `#22D3EE` | — |

---

## 4. MOTOR DE INTERPRETAÇÃO (NLU) — O CORAÇÃO DO APP

Módulo isolado, puro Kotlin, **sem dependência de Android** (testável em JVM).

```kotlin
interface TransactionParser {
    fun parse(rawText: String, referenceDate: Instant): ParseResult
}

data class ParseResult(
    val draft: TransactionDraft?,
    val confidence: Float,
    val ambiguities: List<Ambiguity>,   // campos que o app deve destacar p/ revisão
    val trace: List<String>             // log do pipeline, exibido em modo debug
)
```

### 4.1 Pipeline de parsing (executar nesta ordem)

**Etapa 1 — Normalização**
- Minúsculas, remoção de acentos para *matching* (preservar original para exibição)
- Colapsar espaços múltiplos, remover pontuação exceto `,` `.` em números
- Expandir contrações comuns: "pra" → "para", "tô" → "estou", "pro" → "para o"

**Etapa 2 — Extração de VALOR**

Deve reconhecer, em ordem de prioridade:

| Padrão | Exemplo | Resultado |
|---|---|---|
| Símbolo monetário | `R$ 20,50` / `RS20.50` / `20 reais` | 2050 |
| Numeral com centavos falados | `vinte reais e cinquenta centavos` | 2050 |
| Numeral por extenso | `cento e vinte` / `mil e duzentos` | 12000 / 120000 |
| Gíria | `20 conto` / `20 pila` / `20 mangos` | 2000 |
| Abreviação | `1,5k` / `1.5 mil` | 150000 |
| Número solto (fallback) | `adicione 20 de pastel` | 2000 |

Regex base (ajustar conforme necessário):
```regex
(?:r\$\s*)?(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?|\d+(?:[.,]\d{1,2})?)\s*(?:reais?|conto|pila|mango|k|mil)?
```

Conversor de numerais por extenso PT-BR obrigatório (0–999.999), incluindo: `um..dez, onze..dezenove, vinte..noventa, cem/cento, duzentos..novecentos, mil, milhão`, com conectivo `e`.

**Etapa 3 — Extração de DATA**

| Expressão | Resolução |
|---|---|
| *(ausente)* | agora |
| `hoje` | hoje, hora atual |
| `ontem` / `anteontem` | D-1 / D-2 |
| `dia 24` | dia 24 do **mês corrente**; se 24 > hoje, usar **mês anterior** |
| `dia 24 de julho` | 24/07 do ano corrente |
| `24/07` / `24-07` / `24 do 7` | idem |
| `segunda passada` / `sexta passada` | último dia-da-semana correspondente |
| `semana passada` | mesmo dia-da-semana, D-7 |
| `mês passado` | mesmo dia, mês anterior (clamp para último dia do mês) |
| `há 3 dias` / `3 dias atrás` | D-3 |
| `no início do mês` | dia 1 |

**Regra crítica:** "dia 24" quando hoje é dia 10 → interpretar como **24 do mês anterior**, porque o usuário está registrando algo passado. Nunca lançar no futuro, exceto se a frase contiver marcador explícito de futuro (`amanhã`, `dia 30 que vem`, `próxima terça`).

**Etapa 4 — Extração de TIPO (entrada/saída)**

```
SAÍDA  (default): gastei, paguei, comprei, despesa, saiu, débito, torrei, gasto
ENTRADA:          recebi, entrou, ganhei, receita, salário, caiu, entrada, crédito, me pagaram, vendi
```
Se ambíguo → **default EXPENSE** (95% dos lançamentos são gastos), `confidence -= 0.1`.

**Etapa 5 — Extração de FORMA DE PAGAMENTO**
```
pix | no pix                    → PIX
crédito | no cartão | parcelado → CREDITO
débito | no débito              → DEBITO
dinheiro | espécie | à vista    → DINHEIRO
boleto                          → BOLETO
```

**Etapa 6 — Extração de DESCRIÇÃO**

Remover da frase todos os tokens já consumidos (verbo de ação, valor, data, forma de pagamento, palavras funcionais) e usar o restante. Preposições de ligação removidas: `de, com, no, na, em, para, pra, do, da`.

```
"adicione 20 reais no dia 24 gasto com pastel"
 → tokens consumidos: [adicione][20 reais][no dia 24][gasto][com]
 → descrição: "pastel"
```

**Etapa 7 — Classificação de CATEGORIA (motor de regras)**

Ordem de tentativa, parando na primeira que atingir o limiar:

1. **Regra de usuário exata** (`CategoryRuleEntity.isUserDefined = true`) → confidence 1.0
2. **Match exato de merchant já visto** (histórico: mesma `merchantNormalized` usada ≥ 2× na mesma categoria) → 0.95
3. **Regra de fábrica exata** (keyword completa presente) → 0.90
4. **Match parcial / stem** ("mercadinho" → "mercado") → 0.75
5. **Fuzzy (Levenshtein normalizado ≤ 0.25)** para lidar com erro de transcrição ("pastél", "netiflix") → 0.65
6. **Fallback LLM opcional** (§4.3) → 0.80 se disponível
7. **Categoria "Outros"** + `needsReview = true` → 0.30

Após o usuário corrigir manualmente uma categoria, **criar automaticamente** uma `CategoryRuleEntity` com `isUserDefined = true` usando a palavra-chave mais significativa da descrição. O app aprende sozinho.

**Etapa 8 — Cálculo de confiança agregada**

```
confidence = w_valor(0.40) + w_categoria(0.30) + w_data(0.15) + w_tipo(0.15)
```
- `confidence ≥ 0.85` → grava e mostra notificação simples de confirmação
- `0.60 ≤ confidence < 0.85` → grava, marca `needsReview`, notificação destaca o campo incerto
- `confidence < 0.60` → grava como rascunho e abre bottom sheet de correção rápida
- **Valor não encontrado** → não grava; devolve erro falado/notificado "Não entendi o valor"

### 4.2 Casos de teste obrigatórios do parser

Implemente estes como testes unitários (referência: hoje = 25/08/2026, 14:30):

| Entrada | valor | tipo | categoria | occurredAt |
|---|---|---|---|---|
| `adicione 20 reais de despesa de pagamento da assinatura de tal app` | 2000 | EXPENSE | Entretenimento | 25/08 14:30 |
| `adicione 20 reais no dia 24 gasto com pastel` | 2000 | EXPENSE | Alimentação | 24/08 12:00 |
| `gastei 45,90 no mercado ontem` | 4590 | EXPENSE | Alimentação | 24/08 |
| `recebi 3500 de salário dia 5` | 350000 | INCOME | Salário | 05/08 |
| `paguei 89 reais de internet` | 8900 | EXPENSE | Moradia | 25/08 |
| `uber 23 e 50` | 2350 | EXPENSE | Transporte | 25/08 |
| `torrei cento e vinte reais numa cerveja sexta passada` | 12000 | EXPENSE | Entretenimento | 21/08 |
| `netflix 55 no crédito` | 5500 | EXPENSE | Entretenimento | 25/08 (CREDITO) |
| `caiu 800 de freela` | 80000 | INCOME | Freelance | 25/08 |
| `farmácia 32,80 pix` | 3280 | EXPENSE | Saúde | 25/08 (PIX) |
| `1,5k de aluguel dia 10` | 150000 | EXPENSE | Moradia | 10/08 |
| `gasolina 200 no dia 24 de julho` | 20000 | EXPENSE | Transporte | 24/07 |

Meta: **≥ 92% de acerto em todos os 4 campos** num corpus de 200 frases (crie o corpus).

### 4.3 Fallback opcional por LLM (desligado por padrão)

Configuração avançada onde o usuário cola uma API key própria (Gemini API). Quando `confidence < 0.60` **e** houver rede **e** a opção estiver ligada:

```kotlin
// System prompt fixo, resposta SOMENTE em JSON
"""
Você extrai lançamentos financeiros de frases em português brasileiro.
Data de referência: {ISO_DATE}. Categorias disponíveis: {LISTA}.
Responda APENAS com JSON, sem markdown, sem explicação:
{"amountCents":int,"type":"EXPENSE|INCOME","description":string,
 "categoryId":string,"occurredAtIso":string,"paymentMethod":string|null}
"""
```
Timeout de 2,5 s. Se falhar, usa o resultado offline. **Nunca** bloquear a gravação esperando a rede.

---

## 5. CAMADA DE GATILHOS DE VOZ — implementação detalhada

### 5.1 Ponto de entrada único

```kotlin
@Singleton
class QuickEntryGateway @Inject constructor(
    private val parser: TransactionParser,
    private val repo: TransactionRepository,
    private val notifier: EntryNotifier,
    private val clock: Clock
) {
    suspend fun ingest(rawText: String, source: EntrySource): IngestOutcome
}
```
Todas as 8 rotas chamam **apenas** este método. Nenhuma rota implementa parsing próprio.

### 5.2 Rota 1 — AppFunctions (Android 16+, futuro-prova)

```gradle
implementation("androidx.appfunctions:appfunctions:1.0.0-alpha10")
ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha10")
```

**Ponto crítico:** o KDoc das funções anotadas **é** o contrato lido pelo agente para decidir se chama seu app. Escreva-o como se fosse um prompt.

```kotlin
class FinanceAppFunctions {

    /**
     * Registra uma despesa (saída de dinheiro) nas finanças pessoais do usuário.
     * Use quando o usuário disser que gastou, pagou, comprou algo ou quer
     * adicionar uma despesa. Aceita a frase completa em linguagem natural —
     * o app extrai valor, categoria e data automaticamente.
     *
     * @param naturalLanguageInput A frase completa dita pelo usuário, sem
     *   modificações. Ex.: "20 reais de pastel no dia 24".
     * @return Resumo do lançamento criado, para ser lido de volta ao usuário.
     */
    @AppFunction(isDescribedByKdoc = true)
    suspend fun addExpense(
        appFunctionContext: AppFunctionContext,
        naturalLanguageInput: String
    ): EntryResult

    /**
     * Registra uma receita (entrada de dinheiro). Use quando o usuário disser
     * que recebeu, ganhou, ou que algo caiu na conta.
     */
    @AppFunction(isDescribedByKdoc = true)
    suspend fun addIncome(
        appFunctionContext: AppFunctionContext,
        naturalLanguageInput: String
    ): EntryResult

    /**
     * Consulta quanto o usuário gastou num período e/ou categoria.
     * Ex.: "quanto gastei com comida esse mês", "qual meu saldo".
     */
    @AppFunction(isDescribedByKdoc = true)
    suspend fun querySpending(
        appFunctionContext: AppFunctionContext,
        naturalLanguageQuery: String
    ): SpendingSummary

    /** Cria uma nova categoria de gastos. */
    @AppFunction(isDescribedByKdoc = true)
    suspend fun createCategory(
        appFunctionContext: AppFunctionContext,
        name: String
    ): CategoryResult
}
```

Registrar a factory no `Application`, declarar o serviço no manifest conforme a doc do AppFunctions, e validar com:
```bash
adb shell cmd app_function list-app-functions | grep -A 10 <seu.package>
```

### 5.3 Rota 2 — App Actions legado (`shortcuts.xml`)

Implementar mesmo sabendo que morre, porque cobre dispositivos que ainda não migraram.

```xml
<!-- res/xml/shortcuts.xml -->
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">

    <capability android:name="custom.actions.intent.ADD_EXPENSE">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.seuapp.financas"
            android:targetClass="com.seuapp.financas.voice.VoiceIngestActivity">
            <parameter android:name="expense.text" android:key="text" android:required="true"/>
        </intent>
    </capability>

    <!-- Atalhos estáticos: aparecem no long-press do ícone e no Gemini "Apps" -->
    <shortcut
        android:shortcutId="quick_add_expense"
        android:enabled="true"
        android:icon="@drawable/ic_shortcut_expense"
        android:shortcutShortLabel="@string/sc_expense_short"
        android:shortcutLongLabel="@string/sc_expense_long">
        <intent android:action="android.intent.action.VIEW"
                android:targetPackage="com.seuapp.financas"
                android:targetClass="com.seuapp.financas.voice.VoiceCaptureActivity"/>
    </shortcut>
    <!-- + quick_add_income, view_reports -->
</shortcuts>
```

Adicionalmente, **publicar dynamic shortcuts** via `ShortcutManagerCompat.pushDynamicShortcut()` com `capabilityBinding`, ranqueados por uso (as 4 categorias mais usadas viram atalhos "Adicionar em Alimentação" etc.).

### 5.4 Rota 3 — Assumir o papel de assistente do sistema (a mais confiável)

Esta rota **não depende do Google**. Permite que segurar o botão de energia / gesto de assistente abra a captura de voz do seu app.

```xml
<service android:name=".voice.FinanceVoiceInteractionService"
    android:label="@string/app_name"
    android:permission="android.permission.BIND_VOICE_INTERACTION"
    android:exported="true">
    <meta-data android:name="android.voice_interaction"
               android:resource="@xml/voice_interaction_service"/>
    <intent-filter>
        <action android:name="android.service.voice.VoiceInteractionService"/>
    </intent-filter>
</service>

<activity android:name=".voice.VoiceCaptureActivity"
    android:theme="@style/Theme.Transparent"
    android:excludeFromRecents="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.ASSIST"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
</activity>
```

O onboarding deve oferecer: *"Quer registrar gastos sem abrir o app? Defina o Finanças como seu app de assistente."* → abrir `Settings.ACTION_VOICE_INPUT_SETTINGS`.

**⚠️ Conflito direto no Galaxy S23:** o botão lateral já invoca o Gemini. Assumir o papel de assistente **substitui o Gemini nesse gesto**. Como o usuário quer manter o Gemini, esta rota deve ser:

- **Desativada por padrão**
- Oferecida uma única vez em Configurações → Voz, com o aviso explícito: *"Isso substituirá o Gemini ao segurar o botão lateral. Você pode reverter a qualquer momento."*
- Nunca sugerida de novo se recusada

**Alternativa recomendada no S23, sem conflito:** mapear o gesto de **duplo toque na traseira** (One UI: Configurações → Recursos avançados → Movimentos e gestos) ou um **botão do fone Bluetooth** para o deep link `financas://capture`. Isso dá o mesmo hands-free sem tocar no Gemini.

Documente ambos os caminhos na tela de ajuda do app.

### 5.5 Rota 4 — Widget de tela inicial (Glance)

Widget 4×1 e 4×2 com:
- Saldo do mês (grande)
- Botão **🎤** (abre `VoiceCaptureActivity` direto no modo escuta)
- Botão **−** (despesa rápida manual)
- Botão **+** (receita rápida manual)
- Barra de progresso do orçamento do mês

Atualização via `updateAppWidgetState` disparada por Flow do Room. Nunca fazer query síncrona no `provideGlance`.

### 5.6 Rota 5 — Quick Settings Tile

```kotlin
@AndroidEntryPoint
class VoiceEntryTileService : TileService() {
    override fun onClick() {
        startActivityAndCollapse(
            PendingIntent.getActivity(this, 0,
                Intent(this, VoiceCaptureActivity::class.java)
                    .putExtra(EXTRA_AUTOSTART_LISTENING, true),
                FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
        )
    }
}
```
Um swipe + um toque = microfone escutando. Funciona com a tela bloqueada se configurado.

### 5.7 Rota 6 — Notificação persistente com ditado

Notificação de baixa prioridade, sempre visível, canal `quick_entry`:
- Mostra: saldo do mês + gasto de hoje
- Ação **"Lançar"** com `RemoteInput` → permite digitar **ou usar o microfone do teclado**
- Ao enviar, `QuickEntryBroadcastReceiver` chama `ingest(text, EntrySource.VOICE)`

Esta é a rota mais resiliente: funciona em 100% dos aparelhos, sem permissão especial, sem depender de assistente algum.

### 5.8 Rota 7 — Deep links

```xml
<intent-filter android:autoVerify="false">
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data android:scheme="financas"/>
</intent-filter>
```

Esquema:
```
financas://add?text=20%20reais%20de%20pastel
financas://add?amount=2000&type=expense&category=alimentacao&date=2026-08-24
financas://report?month=2026-08
```

### 5.9 Rota 8 — Automação externa (Tasker / MacroDroid) + texto selecionado

```xml
<receiver android:name=".voice.ExternalIngestReceiver" android:exported="true"
    android:permission="com.seuapp.financas.permission.INGEST">
    <intent-filter>
        <action android:name="com.seuapp.financas.ACTION_INGEST"/>
    </intent-filter>
</receiver>

<!-- Selecionar texto em qualquer app → menu "Lançar no Finanças" -->
<activity android:name=".voice.ProcessTextActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:mimeType="text/plain"/>
    </intent-filter>
</activity>
```

### 5.10 Reconhecimento de voz próprio (`VoiceCaptureActivity`)

```kotlin
RecognizerIntent.EXTRA_LANGUAGE_MODEL → LANGUAGE_MODEL_FREE_FORM
RecognizerIntent.EXTRA_LANGUAGE → "pt-BR"
RecognizerIntent.EXTRA_PREFER_OFFLINE → true   // Android 12+, reconhecimento on-device
RecognizerIntent.EXTRA_PARTIAL_RESULTS → true  // feedback visual em tempo real
```

**UI da captura (activity transparente, sem chrome):**
- Fundo com scrim `Color.Black.copy(alpha = 0.6f)`
- Círculo pulsante reagindo a `onRmsChanged` (amplitude → escala 1.0–1.4, spring animation)
- Transcrição parcial em texto grande no centro
- Ao finalizar: animação de checkmark + haptic `CONFIRM` + fecha em 400 ms
- Timeout de silêncio: 1,8 s
- **Modo contínuo opcional:** após gravar, reabre o mic por 3 s para lançar vários gastos seguidos ("...e mais 15 de uber")

### 5.11 Feedback de confirmação (obrigatório em todas as rotas)

Após gravar, **notificação heads-up de 4 segundos**:

```
┌──────────────────────────────────────┐
│ 🍔  Despesa registrada               │
│     R$ 20,00 · Pastel                │
│     Alimentação · 24/08              │
│  [DESFAZER]  [EDITAR]  [CATEGORIA ▾] │
└──────────────────────────────────────┘
```
- **DESFAZER**: remove o lançamento, sem diálogo
- **EDITAR**: abre bottom sheet
- **CATEGORIA**: chips inline com as 4 categorias mais prováveis; escolher cria regra de aprendizado
- Se `needsReview = true`, o campo incerto aparece destacado em âmbar

Adicionalmente: **TTS opcional** (desligado por padrão) lendo *"Vinte reais em alimentação, dia vinte e quatro"* — útil quando o celular está no bolso.

---

## 6. TELAS E FLUXOS

### 6.1 Dashboard (Home)

Ordem vertical dos blocos:

1. **Header do mês** — seletor com setas ‹ Agosto 2026 › + swipe horizontal entre meses. Meses passados são navegáveis infinitamente (histórico completo).
2. **Card de saldo** (hero) — número grande, tabular figures, animado com `animateIntAsState`
   - Linha 1: `Saldo do mês` — verde se positivo, vermelho se negativo
   - Linha 2 (secundária): `↓ R$ 4.200 entradas` · `↑ R$ 3.150 saídas`
   - Linha 3: comparação — `12% mais gastos que julho` com seta colorida
3. **Barra de orçamento** — progresso do mês com marcador de "ritmo esperado" (linha vertical na posição `diaAtual/diasDoMês`). Se a barra passou do marcador, cor de alerta.
4. **Faixa de insights** — carrossel horizontal de cards acionáveis (§7.3). Máximo 3 visíveis.
5. **Top 5 categorias do mês** — barras horizontais com valor, %, e delta vs. mês anterior.
6. **Últimos lançamentos** — 8 itens, agrupados por dia, com header sticky ("HOJE", "ONTEM", "24 DE AGOSTO"). Botão "Ver todos".
7. **FAB expandido**: pressão longa = microfone; toque = lançamento manual.

### 6.2 Lançamentos (lista completa)

- `LazyColumn` com `key = transaction.id` e `contentType` uniforme
- Paginação com Paging 3 (`PagingSource` do Room), page size 40
- Header sticky por dia, com subtotal do dia à direita
- **Swipe left** → deletar (com undo de 10 s); **swipe right** → editar
- Barra de filtros persistente: `[Mês ▾] [Categoria ▾] [Tipo ▾] [Conta ▾] [Só revisar]`
- Busca full-text (FTS4) com debounce de 250 ms
- Chip "⚠️ 3 precisam de revisão" quando houver `needsReview = true`
- Seleção múltipla (long press) → recategorizar em lote, deletar em lote

### 6.3 Relatórios

Abas: **Visão Geral · Categorias · Evolução · Comparar**

**Visão Geral**
- Donut de despesas por categoria (toque na fatia → drilldown)
- Cards: maior gasto único, média diária, dias sem gastar, projeção de fim de mês
- Distribuição por dia da semana (barras) — revela "sexta é seu dia caro"
- Distribuição por forma de pagamento

**Categorias**
- Lista ordenada por valor, com barra proporcional, %, contagem de lançamentos, ticket médio
- Delta vs. média dos últimos 3 meses (badge `+38%` em vermelho)
- Drilldown: todos os lançamentos + gráfico de linha da categoria nos últimos 6 meses

**Evolução**
- Gráfico de linha/área: entradas vs. saídas nos últimos 12 meses
- Linha de saldo acumulado
- Marcadores nos meses com maior gasto

**Comparar**
- Seletor de dois meses lado a lado
- Tabela: categoria | mês A | mês B | Δ R$ | Δ %
- Ordenado pelo maior aumento absoluto — é aqui que a "brecha" aparece

**Renderização de gráficos:** usar `Canvas` do Compose com `drawPath`/`drawArc` nativo, ou Vico. **Não** usar MPAndroidChart (AndroidView + XML, mata a performance).

### 6.4 Categorias e Tags

- Grid de categorias com ícone, cor, total do mês
- Criar/editar: nome, seletor de ícone (grid de 40 ícones Material), color picker (paleta de 16)
- **Aba "Regras"**: lista de palavras-chave → categoria; editável; mostra `hitCount`
- Reordenar por drag and drop
- Arquivar categoria (soft delete) com realocação dos lançamentos existentes

### 6.5 Orçamentos

- Definir limite por categoria e/ou geral, por mês
- Opção "copiar do mês anterior"
- Opção rollover (sobra acumula)
- Notificações em 80% e 100% do limite
- Sugestão automática: "Baseado nos últimos 3 meses, sugerimos R$ 850 para Alimentação"

### 6.6 Onboarding

Máximo 4 telas, todas puláveis:
1. Boas-vindas + seleção de categorias que interessam
2. Saldo inicial (opcional)
3. **Permissões, uma de cada vez, com justificativa** (§9)
4. **Tutorial de voz interativo:** "Experimente dizer: *gastei 20 reais de café*" com microfone já aberto. Mostra o resultado do parse em tempo real.

---

## 7. INTELIGÊNCIA — "onde eu desperdicei este mês"

Esta seção é o motivo do app existir. Implementar como `InsightEngine`, recalculado ao abrir o Dashboard e diariamente via WorkManager.

### 7.1 Detector de assinaturas e recorrências

Algoritmo:
1. Agrupar lançamentos por `merchantNormalized` ou descrição normalizada
2. Grupo é candidato se: ≥ 3 ocorrências, em meses distintos consecutivos, com valor variando ≤ 15%, e intervalo entre ocorrências de 28±5 dias
3. Marcar com `recurrenceGroupId`, sugerir criação de `RecurringRule`
4. Exibir tela **"Assinaturas"**: lista de recorrências, custo mensal total, custo anualizado
5. Alerta: *"Você paga R$ 274/mês em 7 assinaturas = R$ 3.288/ano"*
6. Detectar **assinatura zumbi**: recorrência ativa cuja categoria é Entretenimento/Serviços e que não teve nenhum lançamento relacionado registrado — sinalizar "Você usa isso?"
7. Detectar **aumento silencioso**: recorrência cujo valor subiu > 10% vs. a ocorrência anterior

### 7.2 Detector de gastos formiga

- Agrupar despesas com valor < R$ 30 (limiar configurável)
- Calcular total mensal e % do gasto total
- Insight: *"R$ 387 em 43 gastos pequenos — 12% do seu mês"*
- Drilldown por categoria dentro dos gastos formiga
- Comparação: *"Equivale a 1,3 assinaturas do seu plano de internet"*

### 7.3 Cards de insight acionáveis (Dashboard)

Gerar dinamicamente, ranqueados por relevância (impacto em R$ × novidade). Tipos obrigatórios:

| Tipo | Condição de disparo | Texto exemplo |
|---|---|---|
| `CATEGORY_SPIKE` | Categoria > 130% da média de 3 meses | "Alimentação está 38% acima da sua média. R$ 320 a mais que o normal." |
| `PACE_WARNING` | Gasto projetado > orçamento | "No ritmo atual você fecha agosto em R$ 4.100 — R$ 600 acima do orçado." |
| `SUBSCRIPTION_TOTAL` | Sempre (mensal) | "7 assinaturas ativas somam R$ 274/mês." |
| `ZOMBIE_SUB` | Recorrência sem uso aparente | "Você paga X há 8 meses. Ainda usa?" |
| `MICRO_SPEND` | Gastos formiga > 10% do total | "R$ 387 em compras pequenas este mês." |
| `EXPENSIVE_DAY` | Dia da semana com > 25% do gasto | "Sextas concentram 31% dos seus gastos." |
| `TOP_MERCHANT` | Estabelecimento repetido | "iFood: 14 pedidos, R$ 612 este mês." |
| `NO_SPEND_STREAK` | ≥ 3 dias sem gastos | "3 dias sem gastar. Melhor sequência do mês." |
| `PRICE_CREEP` | Item recorrente subiu de preço | "Sua conta de luz subiu R$ 47 vs. julho." |
| `UNCATEGORIZED` | > 5 lançamentos em "Outros" | "8 lançamentos sem categoria — R$ 340 invisíveis." |
| `SAVINGS_RATE` | Sempre | "Você guardou 18% do que entrou este mês." |
| `WEEKEND_RATIO` | Fim de semana > 40% | "Fins de semana representam 44% dos seus gastos." |

Cada card tem uma **ação**: "Ver lançamentos", "Criar orçamento", "Marcar como cancelada", "Categorizar agora".

### 7.4 Relatório de fechamento mensal

No dia 1, notificação: *"Seu agosto fechou. Ver resumo."* → tela dedicada com:
- Total entrou / saiu / sobrou
- Taxa de poupança
- Top 3 categorias
- **"Onde deu pra economizar"**: lista das 5 maiores oportunidades identificadas, com valor estimado recuperável
- Comparação com o mês anterior e com a média do ano
- Botão "Exportar CSV"

### 7.5 Consultas por voz (via AppFunctions `querySpending`)

Suportar perguntas naturais:
- "quanto gastei esse mês" / "qual meu saldo"
- "quanto gastei com comida" / "...em julho"
- "qual categoria eu mais gastei"
- "quanto entrou esse mês"
- "quanto gastei com uber esse ano"

Resposta curta, falável: *"Você gastou R$ 3.150 em agosto. A maior categoria foi Alimentação, com R$ 890."*

---

## 8. LEITURA DE NOTIFICAÇÕES BANCÁRIAS

### 8.1 Implementação

```kotlin
class BankNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in allowlist) return
        val text = sbn.notification.extras.getString(Notification.EXTRA_TEXT) ?: return
        // → BankMessageParser → sugestão pendente (NUNCA lançamento direto)
    }
}
```

```xml
<service android:name=".notifications.BankNotificationListener"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService"/>
    </intent-filter>
</service>
```

### 8.2 Allowlist configurável pelo usuário

Padrão sugerido (usuário marca quais ativar): Nubank, Inter, Itaú, Bradesco, Santander, BB, Caixa, C6, PicPay, Mercado Pago, PagBank, Neon, Will Bank.

### 8.3 Padrões de parsing

```regex
# Compra aprovada
compra\s+aprovada.*?R\$\s*([\d.,]+)\s*(?:em|no|na)\s+(.+?)(?:\.|$)

# PIX enviado/recebido
pix\s+(enviado|recebido).*?R\$\s*([\d.,]+)

# Débito
(débito|debitado).*?R\$\s*([\d.,]+)
```

Normalização de merchant: remover prefixos de adquirente (`PAG*`, `MP*`, `IFD*`, `PICPAY*`), números de pedido, e sufixos de cidade.

### 8.4 Regras de segurança inegociáveis

1. **Nunca lançar automaticamente.** Sempre criar uma *sugestão pendente*.
2. Sugestões aparecem numa **bandeja "Pendentes"** no Dashboard com botões `[Confirmar] [Editar] [Ignorar]`.
3. **Deduplicação:** se já existe lançamento com valor idêntico ±R$0,01 e `occurredAt` dentro de ±30 min, marcar como possível duplicata e não sugerir.
4. **Nada sai do dispositivo.** Nenhum texto de notificação vai para rede, log remoto ou crash report.
5. Feature 100% opt-in, com tela explicativa antes de pedir a permissão.
6. Se o app for publicado na Play Store, esta permissão exige declaração de uso — para uso pessoal via sideload, sem restrição.

---

## 9. PERMISSÕES — solicitação e justificativa

| Permissão | Quando pedir | Texto de justificativa (pt-BR) |
|---|---|---|
| `RECORD_AUDIO` | No tutorial de voz do onboarding | "Para você registrar gastos falando. O áudio é processado no seu aparelho e não é gravado." |
| `POST_NOTIFICATIONS` | Após o primeiro lançamento | "Para confirmar seus lançamentos e avisar quando o orçamento apertar." |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Só em Configurações, sob demanda | "Para sugerir lançamentos a partir das notificações do seu banco. Nada é enviado para fora do celular." |
| `SCHEDULE_EXACT_ALARM` | Ao criar primeiro lembrete | "Para lembrar você de lançar os gastos no horário escolhido." |
| `USE_BIOMETRIC` | Se ativar bloqueio | "Para proteger seus dados financeiros." |
| Assistente padrão | Card dispensável no Dashboard | "Segure o botão de energia e fale seu gasto, sem abrir o app." |
| Ignorar otimização de bateria | Se o listener falhar | "Para não perder notificações do banco." |

**Regras:**
- Nunca pedir mais de uma permissão por tela
- Sempre mostrar o *porquê* antes do diálogo do sistema
- Se negada, o app continua funcionando com degradação graciosa e oferece re-solicitar depois
- Tela **Configurações → Permissões** com o status de cada uma e botão de correção

---

## 10. DESIGN SYSTEM E REGRAS DE LAYOUT

### 10.1 Princípios

1. **Densidade informacional com respiro.** O número importante é grande; o resto é suporte.
2. **Cor carrega significado, não decoração.** Verde = entrada, vermelho = saída, âmbar = atenção, cinza = neutro. Categorias usam cor própria só em gráficos e ícones.
3. **Zero telas de carregamento.** Skeletons ou dados em cache. Nunca spinner de tela cheia.
4. **Toque mínimo.** Toda ação frequente acessível em ≤ 2 toques a partir da home.
5. **Polegar primeiro.** Ações primárias no terço inferior da tela.

### 10.2 Tokens

```kotlin
// Tipografia — Inter ou Roboto Flex, com FontFeatureSettings("tnum") em valores
displayLarge   57sp / w400   // saldo hero
headlineMedium 28sp / w600   // títulos de seção
titleLarge     22sp / w600   // valores em cards
bodyLarge      16sp / w400   // texto corrido
bodyMedium     14sp / w400   // descrições de lançamento
labelMedium    12sp / w500   // metadados, datas
labelSmall     11sp / w500   // chips

// Valores monetários: SEMPRE tabular figures
Modifier + TextStyle(fontFeatureSettings = "tnum")

// Espaçamento — escala 4pt
4, 8, 12, 16, 20, 24, 32, 40, 48

// Raios
Chip 8dp · Card 16dp · BottomSheet 28dp · FAB 20dp · Dialog 28dp

// Elevação — usar tonalElevation, não shadow
Card 1dp · FAB 3dp · BottomSheet 3dp

// Cores semânticas (extension no ColorScheme)
income      = #16A34A / dark #4ADE80
expense     = #DC2626 / dark #F87171
warning     = #D97706 / dark #FBBF24
neutral     = onSurfaceVariant
```

### 10.3 Regras visuais obrigatórias

- **Material You / dynamic color** ativo por padrão (Android 12+), com fallback para paleta própria
- **Dark mode nativo e verdadeiro** (superfícies `#0F1115` → `#1A1D23`, não cinza lavado)
- **Sinal do valor:** despesas com `−` e cor de despesa; receitas com `+`. Nunca só a cor (acessibilidade).
- **Formatação:** `NumberFormat.getCurrencyInstance(Locale("pt","BR"))`. Valores ≥ R$ 10.000 abreviam para `R$ 12,4 mil` em gráficos, nunca em listas.
- **Datas:** "Hoje", "Ontem", "Seg, 24 ago" — nunca `24/08/2026` na lista.
- **Estados vazios ilustrados** com CTA de voz: "Nenhum gasto ainda. Toque no microfone e diga seu primeiro gasto."
- **Motion:** transições de tela 250 ms `FastOutSlowIn`; valores animam com `spring(dampingRatio = 0.8f)`; gráficos animam de 0 ao valor em 600 ms na primeira composição apenas.
- **Haptics:** `HapticFeedbackType.LongPress` ao gravar, `ContextClick` ao trocar de mês, `Confirm` ao completar voz.
- **Acessibilidade:** todos os alvos ≥ 48dp, contraste ≥ 4.5:1, `contentDescription` em todos os ícones, suporte a fonte 200%, TalkBack lendo valores como "menos vinte reais em alimentação".

---

## 11. PERFORMANCE — requisitos não-negociáveis

1. **Nenhuma agregação em Kotlin.** Todos os somatórios em SQL:
   ```sql
   SELECT categoryId, SUM(amountCents) AS total, COUNT(*) AS qty
   FROM transactions
   WHERE yearMonth = :ym AND type = 'EXPENSE' AND excludeFromReports = 0
   GROUP BY categoryId ORDER BY total DESC
   ```
2. **Índices conforme §3.1.** Rodar `EXPLAIN QUERY PLAN` em todas as queries de relatório; nenhuma pode fazer `SCAN TABLE`.
3. **Flows com `distinctUntilChanged()` e `stateIn(SharingStarted.WhileSubscribed(5000))`.**
4. **Nunca coletar Flow dentro de item de lista.** Estado agregado no ViewModel.
5. **`derivedStateOf`** para qualquer cálculo derivado dentro de composable.
6. **Listas:** `key` estável obrigatório; itens não podem ter altura variável imprevisível.
7. **Baseline Profile** gerado e commitado; `ProfileInstaller` no release.
8. **R8 full mode** + `isShrinkResources = true`.
9. **Cold start:** nenhuma I/O na `Application.onCreate()`. Hilt lazy. Room criado com `Dispatchers.IO`.
10. **Cache de relatórios:** materializar agregados mensais numa tabela `monthly_summary`, invalidada por trigger ao inserir/editar/deletar. Meses fechados nunca são recalculados.
11. **Widget:** atualizar no máximo a cada 15 min ou por evento de escrita, nunca por polling.
12. **Memória:** nenhum `Bitmap` grande; ícones como vetores; sem imagens de rede.

---

## 12. SEGURANÇA E PRIVACIDADE

- **Zero rede** por padrão. Se o fallback LLM estiver desligado, o app não faz nenhuma requisição — declare isso na tela "Sobre".
- **Bloqueio biométrico opcional** (`BiometricPrompt`), com timeout configurável, e ocultação do conteúdo no app switcher (`FLAG_SECURE` quando ativado).
- **Backup:** `android:allowBackup="false"`. Exportação manual em JSON/CSV, com opção de criptografar com senha (AES-GCM via `EncryptedFile`).
- **Importação:** CSV com mapeamento de colunas, preview antes de confirmar, deduplicação.
- **Chave da API do LLM** (se usada) armazenada em `EncryptedSharedPreferences`.
- **Logs:** nenhum valor monetário, descrição ou texto de notificação em `Log.*` em build de release.

---

## 13. ARQUITETURA DE CÓDIGO

```
:app                    → DI, navegação, MainActivity, tema
:core:model             → data classes puras (sem Android)
:core:database          → Room, DAOs, migrations
:core:data              → repositories, mappers
:core:designsystem      → tokens, componentes reutilizáveis
:core:common            → utils, formatadores, Clock
:feature:dashboard
:feature:transactions
:feature:reports
:feature:categories
:feature:budgets
:feature:settings
:feature:voice          → captura, gateway, todas as 8 rotas
:feature:widget         → Glance
:nlu                    → parser PT-BR (módulo Kotlin puro, JVM)
:integration:appfunctions
:integration:notifications
```

**Padrão:** MVVM unidirecional. `UiState` como data class imutável exposta por `StateFlow`. Eventos como `Channel`/`SharedFlow`. Repositórios devolvem `Flow`. Nenhum `Context` acima da camada de dados.

---

## 14. CRITÉRIOS DE ACEITE (checklist de entrega)

**Voz**
- [ ] Dizer "gastei 20 reais de pastel" pelo widget grava despesa em Alimentação com data de hoje em < 800 ms
- [ ] Dizer "adicione 20 reais no dia 24 gasto com pastel" grava com `occurredAt` = dia 24
- [ ] Notificação de confirmação aparece com Desfazer funcional
- [ ] As 8 rotas de entrada funcionam e todas chamam `QuickEntryGateway.ingest`
- [ ] Parser passa em ≥ 92% do corpus de 200 frases
- [ ] Corrigir uma categoria manualmente cria regra e acerta na próxima vez

**Dados e histórico**
- [ ] Navegação para qualquer mês passado carrega em < 200 ms
- [ ] Meses fechados usam cache e não recalculam
- [ ] Exportar e reimportar CSV preserva 100% dos dados

**Inteligência**
- [ ] Assinaturas detectadas após 3 meses de dados sintéticos
- [ ] Todos os 12 tipos de insight disparam corretamente com dados de teste
- [ ] Aba "Comparar" identifica corretamente a categoria com maior aumento

**Qualidade**
- [ ] Zero janks em lista de 5.000 itens
- [ ] Cold start < 700 ms em dispositivo mid-range
- [ ] Funciona 100% em modo avião
- [ ] TalkBack navega todas as telas
- [ ] Dark mode sem nenhum contraste abaixo de 4.5:1

---

## 15. ROADMAP DE IMPLEMENTAÇÃO

| Fase | Entrega | Por que nesta ordem |
|---|---|---|
| **1** | Room + modelo de dados + CRUD manual + Dashboard básico | Base de tudo; permite testar sem voz |
| **2** | Módulo `:nlu` completo com testes | É o coração; precisa estar sólido antes das rotas |
| **3** | `QuickEntryGateway` + rotas 4, 5, 6, 7 (widget, tile, notificação, deep link) | Rotas que **não dependem do Google** — garantem o produto funcionando |
| **4** | Reconhecimento de voz próprio + rota 3 (assistente do sistema) | Hands-free real, independente de Gemini |
| **5** | Relatórios + `InsightEngine` | O valor do produto |
| **6** | Orçamentos + fechamento mensal | Fecha o ciclo de controle |
| **7** | Rotas 1 e 2 (AppFunctions + App Actions) + teste adb do §0.3 | APIs instáveis e possivelmente indisponíveis no S23. Timebox de meio dia. |
| **8** | Notification listener bancário | Feature mais delicada, requer base madura |
| **9** | Polimento: baseline profile, acessibilidade, animações | Otimização com o produto pronto |

**Entregue a Fase 3 e você já tem um app melhor que a maioria dos apps de finanças da Play Store**, porque registrar um gasto custa 2 toques em vez de 8.

---

## 16. PROMPT DE INICIALIZAÇÃO PARA A IA

> Implemente o app Android descrito neste documento. Comece pela Fase 1 do roadmap.
> Crie a estrutura completa de módulos Gradle, o schema Room com todas as migrations,
> e o Dashboard funcional com CRUD manual. Use Kotlin 2.x, Compose, Material 3, Hilt e Room.
> Não pule os índices do banco. Não use Double para dinheiro.
> Ao terminar cada fase, liste o que foi feito e peça confirmação antes de seguir.
> Ao implementar o módulo `:nlu`, escreva primeiro os testes da tabela §4.2 e só depois o parser.
