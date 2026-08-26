package br.com.financas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.financas.core.database.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<CategoryRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRuleEntity)

    @Query("SELECT * FROM category_rules ORDER BY weight DESC")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT COUNT(*) FROM category_rules")
    suspend fun count(): Int

    @Query("UPDATE category_rules SET hitCount = hitCount + 1, lastUsedAt = :now WHERE id = :id")
    suspend fun registerHit(id: String, now: Long)
}
