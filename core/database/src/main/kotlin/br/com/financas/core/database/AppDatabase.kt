package br.com.financas.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import br.com.financas.core.database.dao.AccountDao
import br.com.financas.core.database.dao.BudgetDao
import br.com.financas.core.database.dao.CategoryDao
import br.com.financas.core.database.dao.CategoryRuleDao
import br.com.financas.core.database.dao.PendingSuggestionDao
import br.com.financas.core.database.dao.RecurringRuleDao
import br.com.financas.core.database.dao.TagDao
import br.com.financas.core.database.dao.TransactionDao
import br.com.financas.core.database.entity.AccountEntity
import br.com.financas.core.database.entity.BudgetEntity
import br.com.financas.core.database.entity.CategoryEntity
import br.com.financas.core.database.entity.CategoryRuleEntity
import br.com.financas.core.database.entity.PendingSuggestionEntity
import br.com.financas.core.database.entity.RecurringRuleEntity
import br.com.financas.core.database.entity.TagEntity
import br.com.financas.core.database.entity.TransactionEntity
import br.com.financas.core.database.entity.TransactionFts
import br.com.financas.core.database.entity.TransactionTagCrossRef

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CategoryRuleEntity::class,
        BudgetEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        AccountEntity::class,
        RecurringRuleEntity::class,
        TransactionFts::class,
        PendingSuggestionEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun tagDao(): TagDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun pendingSuggestionDao(): PendingSuggestionDao

    companion object {
        const val NAME = "financas.db"

        /** Adiciona `pending_suggestions` (§8) — leitor de notificações bancárias. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_suggestions` (
                        `id` TEXT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `merchantRaw` TEXT NOT NULL,
                        `merchantNormalized` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `detectedAt` INTEGER NOT NULL,
                        `sourcePackage` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_suggestions_status` ON `pending_suggestions` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_suggestions_detectedAt` ON `pending_suggestions` (`detectedAt`)")
            }
        }

        /** Adiciona `externalId` — chave de dedup da importação de extrato (CSV, §5.9/§12). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `externalId` TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_externalId` ON `transactions` (`externalId`)")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
