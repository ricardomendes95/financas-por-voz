package br.com.financas.core.data.tour

/**
 * Um passo do tour guiado: qual tela (`screenId`) precisa estar visível e qual elemento
 * dentro dela (`targetId`) deve ser destacado. Mensagens de texto e navegação real (rotas)
 * ficam em `:app` — este módulo não sabe nada sobre UI ou `Destinations`.
 */
enum class TourStep(val screenId: String, val targetId: String) {
    DASHBOARD_ADD(screenId = "dashboard", targetId = "dashboard_add_fab"),
    SETTINGS_BANK_NOTIFICATIONS(screenId = "settings", targetId = "settings_bank_card"),
    SETTINGS_CATEGORIES(screenId = "settings", targetId = "settings_categories_card"),
    SETTINGS_IMPORT_STATEMENT(screenId = "settings", targetId = "settings_import_card"),
    REPORTS_TAB_MENU(screenId = "reports", targetId = "reports_tab_menu")
}
