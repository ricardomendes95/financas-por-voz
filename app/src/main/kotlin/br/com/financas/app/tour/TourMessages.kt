package br.com.financas.app.tour

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.financas.app.R
import br.com.financas.core.data.tour.TourStep

/** Único lugar que traduz cada [TourStep] (que não sabe nada de UI) para o texto exibido. */
@Composable
fun tourMessage(step: TourStep): String = stringResource(
    when (step) {
        TourStep.DASHBOARD_ADD -> R.string.tour_dashboard_add
        TourStep.SETTINGS_BANK_NOTIFICATIONS -> R.string.tour_settings_bank
        TourStep.SETTINGS_CATEGORIES -> R.string.tour_settings_categories
        TourStep.SETTINGS_IMPORT_STATEMENT -> R.string.tour_settings_import
        TourStep.REPORTS_TAB_MENU -> R.string.tour_reports_menu
    }
)
