package br.com.financas.feature.budgets

data class BudgetRowUi(
    val categoryId: String,
    val name: String,
    val icon: String,
    val colorArgb: Int,
    val limitText: String,
    val spentCents: Long,
    val suggestedCents: Long?
)

data class BudgetsUiState(
    val yearMonth: Int = 0,
    val generalLimitText: String = "",
    val generalSpentCents: Long = 0,
    val rollover: Boolean = false,
    val rows: List<BudgetRowUi> = emptyList(),
    val isLoading: Boolean = true,
    val saved: Boolean = false
)
