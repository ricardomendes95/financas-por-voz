package br.com.financas.app.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.database.sqlite.SQLiteDatabase
import android.os.ParcelFileDescriptor
import br.com.financas.core.data.backup.AutoBackupPreferences
import br.com.financas.core.database.AppDatabase

/**
 * Intercepta o Android Auto Backup (só o full backup do arquivo do banco —
 * key/value não é usado). Sem isso, o sistema copiaria `financas.db` "ao
 * vivo": o Room usa WAL, e sem um checkpoint antes, o backup pode sair
 * inconsistente. Também é o único jeito de deixar o usuário desativar o
 * backup automático sem a permissão de sistema que `BackupManager.setBackupEnabled`
 * exige — se a preferência estiver desligada, simplesmente não inclui nada.
 *
 * Roda fora do ciclo de vida normal do app (sem Hilt), então lê a
 * preferência de forma síncrona em vez de injetada.
 */
class AutoBackupAgent : BackupAgent() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        if (!AutoBackupPreferences.isEnabledBlocking(applicationContext)) return

        checkpointWal()
        super.onFullBackup(data)
    }

    /** Força os dados do WAL para o arquivo principal antes do sistema copiar. */
    private fun checkpointWal() {
        val dbFile = getDatabasePath(AppDatabase.NAME)
        if (!dbFile.exists()) return
        runCatching {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
            }
        }
    }

    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) {
        // Key/value backup não é usado — só o full backup acima.
    }

    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) {
        // Key/value restore não é usado.
    }
}
