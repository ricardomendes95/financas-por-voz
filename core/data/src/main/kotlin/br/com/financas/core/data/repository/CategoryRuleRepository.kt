package br.com.financas.core.data.repository

import br.com.financas.core.data.mapper.toDomain
import br.com.financas.core.database.dao.CategoryRuleDao
import br.com.financas.core.database.entity.CategoryRuleEntity
import br.com.financas.core.model.CategoryRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRuleRepository @Inject constructor(
    private val dao: CategoryRuleDao,
    private val clock: Clock
) {

    fun observeAll(): Flow<List<CategoryRule>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    /**
     * Cria (ou substitui) uma regra de usuário para a palavra-chave — o app
     * "aprende sozinho" quando o usuário corrige uma categoria à mão (§4.1
     * etapa 7). Regras de usuário nascem com peso 100, vencendo qualquer
     * regra de fábrica.
     */
    suspend fun learn(keyword: String, categoryId: String) {
        dao.insert(
            CategoryRuleEntity(
                id = "user_${UUID.randomUUID()}",
                keyword = keyword,
                categoryId = categoryId,
                weight = 100,
                isUserDefined = true,
                hitCount = 1,
                lastUsedAt = clock.millis()
            )
        )
    }

    suspend fun registerHit(ruleId: String) = dao.registerHit(ruleId, clock.millis())
}
