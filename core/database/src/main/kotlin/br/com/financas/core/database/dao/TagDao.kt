package br.com.financas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.financas.core.database.entity.TagEntity
import br.com.financas.core.database.entity.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

/** Sem UI própria ainda — schema reservado desde a v1; tags entram junto com a tela de Categorias. */
@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(crossRef: TransactionTagCrossRef)

    @Query("SELECT * FROM tags")
    fun observeAll(): Flow<List<TagEntity>>
}
