package br.com.financas.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.designsystem.component.CategoryIcons
import br.com.financas.core.designsystem.theme.FinanceTheme
import br.com.financas.feature.reports.component.DonutChart
import br.com.financas.feature.reports.component.DualLineChart
import br.com.financas.feature.reports.component.SimpleBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(tabLabel(uiState.selectedTab)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.reports_menu))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        ReportTab.entries.forEach { tab ->
                            DropdownMenuItem(
                                text = { Text(tabLabel(tab)) },
                                onClick = {
                                    viewModel.onTabSelected(tab)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.selectedTab != ReportTab.YEARLY) {
                MonthSelector(uiState.monthLabel, viewModel::onPreviousMonth, viewModel::onNextMonth)
            }
            when (uiState.selectedTab) {
                ReportTab.OVERVIEW -> OverviewTab(uiState)
                ReportTab.CATEGORIES -> CategoriesTab(uiState)
                ReportTab.EVOLUTION -> EvolutionTab(uiState)
                ReportTab.COMPARE -> CompareTab(uiState)
                ReportTab.YEARLY -> YearlyTab(uiState, viewModel::onPreviousYear, viewModel::onNextYear)
            }
        }
    }
}

@Composable
private fun MonthSelector(monthLabel: String, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.reports_previous_month))
        }
        Text(monthLabel, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.reports_next_month))
        }
    }
}

@Composable
private fun tabLabel(tab: ReportTab): String = when (tab) {
    ReportTab.OVERVIEW -> stringResource(R.string.reports_tab_overview)
    ReportTab.CATEGORIES -> stringResource(R.string.reports_tab_categories)
    ReportTab.EVOLUTION -> stringResource(R.string.reports_tab_evolution)
    ReportTab.COMPARE -> stringResource(R.string.reports_tab_compare)
    ReportTab.YEARLY -> stringResource(R.string.reports_tab_yearly)
}

@Composable
private fun OverviewTab(state: ReportsUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            val slices = state.categoryReport.take(6).map { it.percentOfTotal to Color(it.colorArgb) }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    DonutChart(slices = slices, modifier = Modifier.size(180.dp))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(R.string.reports_max_expense), MoneyFormatter.format(state.overview.maxExpenseCents), Modifier.weight(1f))
                StatCard(stringResource(R.string.reports_average_daily), MoneyFormatter.format(state.overview.averageDailyCents), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(R.string.reports_no_spend_days), "${state.overview.noSpendDays}", Modifier.weight(1f))
                StatCard(stringResource(R.string.reports_projected), MoneyFormatter.format(state.overview.projectedMonthEndCents), Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.reports_weekday_distribution), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    val values = (1..7).map { day -> state.weekdayBreakdown.firstOrNull { it.dayOfWeek == day }?.totalCents?.toFloat() ?: 0f }
                    SimpleBarChart(values = values, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.reports_payment_distribution), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    state.paymentBreakdown.forEach { breakdown ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(breakdown.method?.name ?: stringResource(R.string.reports_payment_unknown))
                            Text(MoneyFormatter.format(breakdown.totalCents))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CategoriesTab(state: ReportsUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.categoryReport, key = { it.categoryId }) { row ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(CategoryIcons.resolve(row.icon), contentDescription = null, tint = Color(row.colorArgb))
                            Text(row.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(MoneyFormatter.format(row.totalCents), style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${row.count} lançamentos · ${(row.percentOfTotal * 100).toInt()}%" +
                            (row.deltaVsAveragePercent?.let { " · ${if (it >= 0) "+" else ""}$it% vs. média" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EvolutionTab(state: ReportsUiState) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(FinanceTheme.colors.income, stringResource(R.string.reports_income))
                    LegendDot(FinanceTheme.colors.expense, stringResource(R.string.reports_expense))
                }
                Spacer(Modifier.height(12.dp))
                DualLineChart(
                    seriesA = state.trend.map { it.incomeCents.toFloat() },
                    seriesB = state.trend.map { it.expenseCents.toFloat() },
                    colorA = FinanceTheme.colors.income,
                    colorB = FinanceTheme.colors.expense,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun YearlyTab(state: ReportsUiState, onPreviousYear: () -> Unit, onNextYear: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousYear) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.reports_previous_year))
                }
                Text(state.selectedYear.toString(), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onNextYear) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.reports_next_year))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(R.string.reports_income), MoneyFormatter.format(state.yearlyIncomeCents), Modifier.weight(1f))
                StatCard(stringResource(R.string.reports_expense), MoneyFormatter.format(state.yearlyExpenseCents), Modifier.weight(1f))
            }
        }
        item {
            Text(stringResource(R.string.reports_yearly_categories), style = MaterialTheme.typography.titleMedium)
        }
        items(state.yearlyCategoryReport, key = { "cat_${it.categoryId}" }) { row ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(CategoryIcons.resolve(row.icon), contentDescription = null, tint = Color(row.colorArgb))
                        Text(row.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(MoneyFormatter.format(row.totalCents), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            Text(
                stringResource(R.string.reports_yearly_compare, state.yearALabel),
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(state.yearComparison, key = { "cmp_${it.categoryName}" }) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.categoryName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(MoneyFormatter.format(row.yearACents), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    MoneyFormatter.format(row.yearBCents),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.deltaCents > 0) FinanceTheme.colors.expense else FinanceTheme.colors.income
                )
            }
        }
    }
}

@Composable
private fun CompareTab(state: ReportsUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.reports_compare_category), style = MaterialTheme.typography.labelMedium)
                Text(state.monthALabel, style = MaterialTheme.typography.labelMedium)
                Text(state.monthBLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
        items(state.comparison, key = { it.categoryName }) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.categoryName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(MoneyFormatter.format(row.monthACents), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    MoneyFormatter.format(row.monthBCents),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.deltaCents > 0) FinanceTheme.colors.expense else FinanceTheme.colors.income
                )
            }
        }
    }
}
