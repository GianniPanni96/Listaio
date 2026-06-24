package com.listaio.data.local

import com.listaio.domain.ListRepository
import com.listaio.domain.ShoppingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Room-backed [ListRepository] for phase 1 (single device, offline).
 *
 * Reactive: the observe* flows are driven by Room, so the UI updates automatically
 * whenever the database changes — the local mirror of Firestore's snapshot listeners.
 */
class LocalListRepository(
    private val dao: ItemDao,
) : ListRepository {

    override fun observeItems(): Flow<List<ShoppingItem>> =
        dao.observeActive().map { entities -> entities.map { it.toDomain() } }

    override fun observeTrash(): Flow<List<ShoppingItem>> =
        dao.observeTrash().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val now = System.currentTimeMillis()
        val nextOrder = (dao.maxActiveOrder() ?: 0.0) + ORDER_GAP
        dao.upsert(
            ItemEntity(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                checked = false,
                order = nextOrder,
                updatedAt = now,
            )
        )
    }

    override suspend fun setChecked(id: String, checked: Boolean) {
        dao.setChecked(id, checked, System.currentTimeMillis())
    }

    override suspend fun rename(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dao.rename(id, trimmed, System.currentTimeMillis())
    }

    override suspend fun move(id: String, newOrder: Double) {
        dao.setOrder(id, newOrder, System.currentTimeMillis())
    }

    override suspend fun moveToTrash(ids: List<String>) {
        if (ids.isEmpty()) return
        dao.moveToTrash(ids, System.currentTimeMillis())
    }

    override suspend fun moveCheckedToTrash() {
        dao.moveCheckedToTrash(System.currentTimeMillis())
    }

    override suspend fun restore(ids: List<String>) {
        if (ids.isEmpty()) return
        dao.restore(ids, System.currentTimeMillis())
    }

    override suspend fun deleteForever(ids: List<String>) {
        if (ids.isEmpty()) return
        dao.deleteForever(ids)
    }

    companion object {
        /** Spacing between consecutive items; leaves room to insert via midpoints. */
        const val ORDER_GAP = 1024.0
    }
}
