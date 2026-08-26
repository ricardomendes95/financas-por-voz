package br.com.financas.integration.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BankRowUi(val bank: BankApp, val enabled: Boolean)

@HiltViewModel
class BankAllowlistViewModel @Inject constructor(
    private val preferences: BankAllowlistPreferences
) : ViewModel() {

    val rows: StateFlow<List<BankRowUi>> = preferences.observeEnabledPackages()
        .map { enabled -> BankAllowlist.KNOWN_BANKS.map { BankRowUi(it, it.packageName in enabled) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onToggle(bank: BankApp, enabled: Boolean) {
        viewModelScope.launch { preferences.setEnabled(bank.packageName, enabled) }
    }
}
