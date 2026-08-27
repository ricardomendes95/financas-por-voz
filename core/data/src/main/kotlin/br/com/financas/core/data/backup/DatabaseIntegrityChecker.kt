package br.com.financas.core.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import br.com.financas.core.database.AppDatabase

/**
 * Roda `PRAGMA integrity_check` diretamente pelo caminho do arquivo — nunca
 * via Room/Hilt, porque um banco corrompido pode travar a abertura normal
 * (é usado justamente para decidir se é seguro deixar o Room abrir o banco).
 */
object DatabaseIntegrityChecker {

    /** `true` também quando o arquivo simplesmente não existe (banco novo, nada a validar). */
    fun isValid(context: Context): Boolean {
        val dbFile = context.getDatabasePath(AppDatabase.NAME)
        if (!dbFile.exists()) return true

        return runCatching {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
                }
            }
        }.getOrDefault(false)
    }

    /** Descarta o banco restaurado (e seus arquivos auxiliares de WAL) para começar do zero. */
    fun discard(context: Context) {
        val dbFile = context.getDatabasePath(AppDatabase.NAME)
        dbFile.delete()
        java.io.File(dbFile.path + "-wal").delete()
        java.io.File(dbFile.path + "-shm").delete()
    }
}
