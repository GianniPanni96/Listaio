package com.listaio.domain

import kotlinx.coroutines.flow.Flow

/**
 * The single seam between the UI and the data source.
 *
 * The UI and ViewModel depend ONLY on this interface. Phase 1 ships a Room-backed
 * implementation ([com.listaio.data.local.LocalListRepository]). Phase 2 adds a
 * Firestore-backed implementation for real-time multi-device sync — nothing else in the
 * app changes. See FIRESTORE.md.
 *
 * Removal is a soft-delete: items move to the trash and can be restored or deleted
 * permanently.
 */
interface ListRepository {

    /** Emits the active items (not trashed), ordered by [ShoppingItem.order]. */
    fun observeItems(): Flow<List<ShoppingItem>>

    /** Emits the trashed items, most-recently trashed first. */
    fun observeTrash(): Flow<List<ShoppingItem>>

    /** Adds a new item at the end of the active list. No-op for blank names. */
    suspend fun addItem(name: String)

    /** Checks/unchecks an item. */
    suspend fun setChecked(id: String, checked: Boolean)

    /** Renames an item. No-op for blank names. */
    suspend fun rename(id: String, name: String)

    /** Moves an item to a new fractional [newOrder] (typically the midpoint of two items). */
    suspend fun move(id: String, newOrder: Double)

    /** Soft-deletes the given items: moves them to the trash. */
    suspend fun moveToTrash(ids: List<String>)

    /** Moves all currently checked active items to the trash. */
    suspend fun moveCheckedToTrash()

    /** Restores the given items from the trash back into the active list. */
    suspend fun restore(ids: List<String>)

    /** Permanently removes the given items (irreversible). */
    suspend fun deleteForever(ids: List<String>)
}
