package br.com.financas.core.data.di

import android.content.Context
import androidx.room.Room
import br.com.financas.core.database.AppDatabase
import br.com.financas.core.database.dao.AccountDao
import br.com.financas.core.database.dao.BudgetDao
import br.com.financas.core.database.dao.CategoryDao
import br.com.financas.core.database.dao.CategoryRuleDao
import br.com.financas.core.database.dao.PendingSuggestionDao
import br.com.financas.core.database.dao.RecurringRuleDao
import br.com.financas.core.database.dao.TagDao
import br.com.financas.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `Room.databaseBuilder(...).build()` não toca em disco — só na primeira
 * query — então não viola a regra §6 (nenhuma I/O em `Application.onCreate`)
 * mesmo sendo criado aqui de forma lazy pelo Hilt.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideCategoryRuleDao(db: AppDatabase): CategoryRuleDao = db.categoryRuleDao()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    fun provideRecurringRuleDao(db: AppDatabase): RecurringRuleDao = db.recurringRuleDao()

    @Provides
    fun providePendingSuggestionDao(db: AppDatabase): PendingSuggestionDao = db.pendingSuggestionDao()
}
