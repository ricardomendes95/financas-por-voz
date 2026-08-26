package br.com.financas.core.data.repository

import br.com.financas.core.data.mapper.toDomain
import br.com.financas.core.database.dao.AccountDao
import br.com.financas.core.database.entity.AccountEntity
import br.com.financas.core.model.Account
import br.com.financas.core.model.AccountKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val dao: AccountDao
) {

    fun observeAll(): Flow<List<Account>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    /** Roda uma vez no primeiro boot — cria a conta "Carteira" usada como padrão. */
    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.insert(
                AccountEntity(
                    id = Account.DEFAULT_ID,
                    name = "Carteira",
                    kind = AccountKind.CASH,
                    openingBalanceCents = 0,
                    closingDay = null,
                    dueDay = null,
                    colorArgb = 0xFF64748B.toInt()
                )
            )
        }
    }
}
