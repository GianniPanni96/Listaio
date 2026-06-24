package com.listaio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    /** Active list: everything not in the trash, ordered by fractional index. */
    @Query("SELECT * FROM items WHERE deleted = 0 ORDER BY sort_order ASC")
    fun observeActive(): Flow<List<ItemEntity>>

    /** Trash: soft-deleted items, most-recently trashed first. */
    @Query("SELECT * FROM items WHERE deleted = 1 ORDER BY updated_at DESC")
    fun observeTrash(): Flow<List<ItemEntity>>

    /** Highest order among ACTIVE items (new items go after it). */
    @Query("SELECT MAX(sort_order) FROM items WHERE deleted = 0")
    suspend fun maxActiveOrder(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    @Query("UPDATE items SET checked = :checked, updated_at = :updatedAt WHERE id = :id")
    suspend fun setChecked(id: String, checked: Boolean, updatedAt: Long)

    @Query("UPDATE items SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, name: String, updatedAt: Long)

    @Query("UPDATE items SET sort_order = :order, updated_at = :updatedAt WHERE id = :id")
    suspend fun setOrder(id: String, order: Double, updatedAt: Long)

    @Query("UPDATE items SET deleted = 1, updated_at = :updatedAt WHERE id IN (:ids)")
    suspend fun moveToTrash(ids: List<String>, updatedAt: Long)

    @Query("UPDATE items SET deleted = 1, updated_at = :updatedAt WHERE checked = 1 AND deleted = 0")
    suspend fun moveCheckedToTrash(updatedAt: Long)

    /** Restore from trash and reset the check so it reappears as a to-buy item. */
    @Query("UPDATE items SET deleted = 0, checked = 0, updated_at = :updatedAt WHERE id IN (:ids)")
    suspend fun restore(ids: List<String>, updatedAt: Long)

    @Query("DELETE FROM items WHERE id IN (:ids)")
    suspend fun deleteForever(ids: List<String>)
}
