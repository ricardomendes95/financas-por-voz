package br.com.financas.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Migrations versionadas desde a v1 — `fallbackToDestructiveMigration` é proibido em release. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun `migracao 1 para 2 cria a tabela pending_suggestions`() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='pending_suggestions'")
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()
        db.close()
    }

    @Test
    fun `migracao 2 para 3 adiciona a coluna externalId em transactions`() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 3, true, AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3
        )

        val cursor = db.query("PRAGMA table_info(transactions)")
        val columnNames = generateSequence { if (cursor.moveToNext()) cursor else null }
            .map { it.getString(it.getColumnIndexOrThrow("name")) }
            .toList()
        cursor.close()
        assertThat(columnNames).contains("externalId")
        db.close()
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
