package com.example.focusfortvs.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.focusfortvs.data.local.entity.BlockedAppEntity

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps WHERE lower(mode) = lower(:mode)")
    fun observeForMode(mode: String): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE lower(mode) = lower(:mode)")
    suspend fun getForMode(mode: String): List<BlockedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BlockedAppEntity)

    @Delete
    suspend fun delete(entity: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE mode = :mode")
    suspend fun clearForMode(mode: String)
}
