package br.com.financas.app.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.financas.core.common.DeepLinks
import br.com.financas.core.data.tour.TourController
import br.com.financas.core.data.tour.TourStep
import br.com.financas.feature.budgets.BudgetsScreen
import br.com.financas.feature.dashboard.DashboardScreen
import br.com.financas.feature.recurring.RecurringScreen
import br.com.financas.feature.reports.ReportsScreen
import br.com.financas.feature.reports.closing.MonthClosingScreen
import br.com.financas.feature.settings.SettingsScreen
import br.com.financas.feature.settings.category.CategoriesScreen
import br.com.financas.feature.settings.statement.ImportStatementScreen
import br.com.financas.feature.transactions.AddEditTransactionScreen
import br.com.financas.feature.transactions.TransactionsScreen
import br.com.financas.integration.notifications.BankAllowlistScreen

@Composable
fun FinanceNavHost(
    tourController: TourController,
    deepLinkUri: Uri? = null,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    LaunchedEffect(deepLinkUri) {
        val route = deepLinkUri?.toDestination() ?: return@LaunchedEffect
        navController.navigate(route)
    }

    // Dirige a navegação durante o tour guiado — o usuário nunca precisa tocar em nada
    // para ir de uma tela a outra enquanto os passos avançam.
    val tourStep by tourController.currentStep.collectAsState()
    LaunchedEffect(tourStep) {
        val step = tourStep ?: return@LaunchedEffect
        val current = navController.currentBackStackEntry?.destination
        val alreadyThere = when (step.screenId) {
            "dashboard" -> current?.hasRoute<Dashboard>() == true
            "settings" -> current?.hasRoute<Settings>() == true
            "reports" -> current?.hasRoute<Reports>() == true
            else -> true
        }
        if (!alreadyThere) {
            when (step.screenId) {
                "dashboard" -> navController.navigate(Dashboard)
                "settings" -> navController.navigate(Settings)
                "reports" -> navController.navigate(Reports)
            }
        }
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
                onSeeAllTransactions = { navController.navigate(Transactions()) },
                onOpenInsightCategory = { categoryId -> navController.navigate(Transactions(categoryId)) },
                onOpenSettings = { navController.navigate(Settings) },
                onOpenReports = { navController.navigate(Reports) },
                onOpenBudgets = { navController.navigate(Budgets) },
                onOpenRecurring = { navController.navigate(Recurring) }
            )
        }
        composable<Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenMonthClosing = { navController.navigate(MonthClosing) },
                onOpenBankAllowlist = { navController.navigate(BankAllowlist) },
                onOpenImportStatement = { navController.navigate(ImportStatement) },
                onOpenCategories = { navController.navigate(Categories) },
                onReplayTour = tourController::start,
                tourStep = tourStep
            )
        }
        composable<BankAllowlist> {
            BankAllowlistScreen(onBack = { navController.popBackStack() })
        }
        composable<ImportStatement> {
            ImportStatementScreen(onBack = { navController.popBackStack() })
        }
        composable<Categories> {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable<Reports> {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable<Budgets> {
            BudgetsScreen(onBack = { navController.popBackStack() })
        }
        composable<Recurring> {
            RecurringScreen(
                onBack = { navController.popBackStack() },
                onEditPaidTransaction = { id -> navController.navigate(EditTransaction(id)) }
            )
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
