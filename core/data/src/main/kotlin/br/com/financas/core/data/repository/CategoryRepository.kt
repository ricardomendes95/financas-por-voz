package br.com.financas.core.data.repository

import br.com.financas.core.data.mapper.toDomain
import br.com.financas.core.data.seed.CategorySeeder
import br.com.financas.core.database.dao.CategoryDao
import br.com.financas.core.database.dao.CategoryRuleDao
import br.com.financas.core.database.entity.CategoryEntity
import br.com.financas.core.model.Category
import br.com.financas.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao
) {

    fun observeActive(): Flow<List<Category>> =
        categoryDao.observeActive().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    suspend fun getById(id: String): Category? = categoryDao.getById(id)?.toDomain()

    /** Cria uma categoria de despesa definida pelo usuário (§5.2, `createCategory`). */
    suspend fun createUserCategory(id: String, name: String) {
        categoryDao.insertAll(
            listOf(
                CategoryEntity(
                    id = id,
                    name = name,
                    icon = "more_horiz",
                    colorArgb = 0xFF94A3B8.toInt(),
                    type = TransactionType.EXPENSE,
                    parentId = null,
                    isSystem = false,
                    sortOrder = 100,
                    archivedAt = null
                )
            )
        )
    }

    /** Roda uma vez no primeiro boot — fora do `Application.onCreate` (regra §6). */
    suspend fun seedIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(CategorySeeder.categoryEntities())
        }
        if (categoryRuleDao.count() == 0) {
            categoryRuleDao.insertAll(CategorySeeder.categoryRuleEntities())
        }
    }
}
