package br.com.financas.core.data

import br.com.financas.core.data.repository.AccountRepository
import br.com.financas.core.data.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Popula categorias/regras/conta padrão no primeiro boot. Disparado pelo
 * `:app` numa coroutine em `Dispatchers.IO`, nunca de forma bloqueante no
 * `Application.onCreate` (regra §6).
 */
@Singleton
class AppInitializer @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {
    suspend fun run() {
        accountRepository.seedIfEmpty()
        categoryRepository.seedIfEmpty()
    }
}
