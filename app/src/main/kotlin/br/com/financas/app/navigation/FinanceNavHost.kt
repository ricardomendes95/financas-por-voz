package br.com.financas.app.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.financas.core.common.DeepLinks
import br.com.financas.feature.budgets.BudgetsScreen
import br.com.financas.feature.dashboard.DashboardScreen
import br.com.financas.feature.reports.ReportsScreen
import br.com.financas.feature.reports.closing.MonthClosingScreen
import br.com.financas.feature.settings.SettingsScreen
import br.com.financas.feature.settings.statement.ImportStatementScreen
import br.com.financas.feature.transactions.AddEditTransactionScreen
import br.com.financas.feature.transactions.TransactionsScreen
import br.com.financas.integration.notifications.BankAllowlistScreen

@Composable
fun FinanceNavHost(
    deepLinkUri: Uri? = null,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    LaunchedEffect(deepLinkUri) {
        val route = deepLinkUri?.toDestination() ?: return@LaunchedEffect
        navController.navigate(route)
    }

    NavHost(navController = navController, startDestination = Dashboard) {
        composable<Dashboard> {
            DashboardScreen(
                onAddTransaction = { navController.navigate(AddTransaction()) },
                onLaunchVoiceCapture = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.capture()))
                        .setPackage(context.packageName)
                    context.startActivity(intent)
                },
                onEditTransaction = { id -> navController.navigate(EditTransaction(id)) },
                onSeeAllTransactions = { navController.navigate(Transactions) },
                onOpenSettings = { navController.navigate(Settings) },
                onOpenReports = { navController.navigate(Reports) },
                onOpenBudgets = { navController.navigate(Budgets) }
            )
        }
        composable<Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenMonthClosing = { navController.navigate(MonthClosing) },
                onOpenBankAllowlist = { navController.navigate(BankAllowlist) },
                onOpenImportStatement = { navController.navigate(ImportStatement) }
            )
        }
        composable<BankAllowlist> {
            BankAllowlistScreen(onBack = { navController.popBackStack() })
        }
        composable<ImportStatement> {
            ImportStatementScreen(onBack = { navController.popBackStack() })
        }
        composable<Reports> {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable<Budgets> {
            BudgetsScreen(onBack = { navController.popBackStack() })
        }
        composable<MonthClosing> {
            MonthClosingScreen(onBack = { navController.popBackStack() })
        }
        composable<Transactions> {
            TransactionsScreen(
                onAddTransaction = { navController.navigate(AddTransaction()) },
                onEditTransaction = { id -> navController.navigate(EditTransaction(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<AddTransaction> {
            AddEditTransactionScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable<EditTransaction> {
            AddEditTransactionScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun Uri.toDestination(): Any? = when (host) {
    DeepLinks.HOST_ADD -> AddTransaction(type = getQueryParameter(DeepLinks.PARAM_TYPE))
    DeepLinks.HOST_EDIT -> getQueryParameter(DeepLinks.PARAM_ID)?.let { EditTransaction(it) }
    else -> null
}
