package br.com.financas.feature.settings.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = categoryRepository.observeActive()
        .map { categories -> CategoriesUiState(categories = categories, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onCreateCategory(name: String, type: TransactionType?, icon: String, colorArgb: Int) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            categoryRepository.createCategory(name = trimmed, type = type, icon = icon, colorArgb = colorArgb)
        }
    }

    fun onArchive(id: String) {
        viewModelScope.launch { categoryRepository.archiveCategory(id) }
    }
}
