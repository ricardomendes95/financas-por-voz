package br.com.financas.core.data.repository

import android.content.Context
import br.com.financas.core.database.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backup/restauração por cópia direta do arquivo SQLite — garante fidelidade
 * total (todas as tabelas, sem precisar mapear entidade por entidade) já que
 * a exportação e a importação sempre falam com a mesma versão de schema.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) {

    /** Força os dados do WAL para o arquivo principal antes de copiar. */
    fun exportTo(destination: OutputStream): Boolean = runCatching {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
        context.getDatabasePath(AppDatabase.NAME).inputStream().use { input ->
            input.copyTo(destination)
        }
    }.isSuccess

    /**
     * Fecha a conexão Room e substitui o arquivo do banco pelo importado.
     * O processo do app precisa reiniciar depois — outros componentes já
     * injetados mantêm referências à conexão fechada.
     */
    fun importFrom(source: InputStream): Boolean = runCatching {
        database.close()
        val dbFile = context.getDatabasePath(AppDatabase.NAME)
        dbFile.outputStream().use { output -> source.copyTo(output) }
        // Arquivos auxiliares do WAL do banco antigo não correspondem mais
        // ao arquivo principal recém-substituído — descartar evita abrir o
        // banco importado com sobras de outra sessão.
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
    }.isSuccess
}
