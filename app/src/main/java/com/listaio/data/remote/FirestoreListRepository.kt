package com.listaio.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.listaio.domain.ListRepository
import com.listaio.domain.ShoppingItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreListRepository(
    private val listId: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ListRepository {

    companion object {
        private const val ORDER_GAP = 1024.0
    }

    private fun col() = db.collection("lists").document(listId).collection("items")

    private fun DocumentSnapshot.toShoppingItem(): ShoppingItem? {
        val name = getString("name") ?: return null
        return ShoppingItem(
            id = id,
            name = name,
            checked = getBoolean("checked") ?: false,
            order = getDouble("order") ?: 0.0,
            updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
        )
    }

    override fun observeItems(): Flow<List<ShoppingItem>> = callbackFlow {
        val reg = col().whereEqualTo("deleted", false)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val items = snap?.documents
                    ?.mapNotNull { it.toShoppingItem() }
                    ?.sortedBy { it.order }
                    ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    override fun observeTrash(): Flow<List<ShoppingItem>> = callbackFlow {
        val reg = col().whereEqualTo("deleted", true)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val items = snap?.documents
                    ?.mapNotNull { it.toShoppingItem() }
                    ?.sortedByDescending { it.updatedAt }
                    ?: emptyList()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val snap = col().whereEqualTo("deleted", false).get().await()
        val maxOrder = snap.documents.mapNotNull { it.getDouble("order") }.maxOrNull() ?: 0.0
        col().add(
            mapOf(
                "name" to trimmed,
                "checked" to false,
                "deleted" to false,
                "order" to maxOrder + ORDER_GAP,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    override suspend fun setChecked(id: String, checked: Boolean) {
        col().document(id).update(
            mapOf("checked" to checked, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    override suspend fun rename(id: String, name: String) {
        col().document(id).update(
            mapOf("name" to name.trim(), "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    override suspend fun move(id: String, newOrder: Double) {
        col().document(id).update(
            mapOf("order" to newOrder, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    override suspend fun moveToTrash(ids: List<String>) {
        if (ids.isEmpty()) return
        val batch = db.batch()
        ids.forEach { id ->
            batch.update(col().document(id), mapOf("deleted" to true, "updatedAt" to FieldValue.serverTimestamp()))
        }
        batch.commit().await()
    }

    override suspend fun moveCheckedToTrash() {
        val snap = col().whereEqualTo("deleted", false).get().await()
        val checkedIds = snap.documents
            .filter { it.getBoolean("checked") == true }
            .map { it.id }
        moveToTrash(checkedIds)
    }

    override suspend fun restore(ids: List<String>) {
        if (ids.isEmpty()) return
        val batch = db.batch()
        ids.forEach { id ->
            batch.update(
                col().document(id),
                mapOf("deleted" to false, "checked" to false, "updatedAt" to FieldValue.serverTimestamp()),
            )
        }
        batch.commit().await()
    }

    override suspend fun deleteForever(ids: List<String>) {
        if (ids.isEmpty()) return
        val batch = db.batch()
        ids.forEach { id -> batch.delete(col().document(id)) }
        batch.commit().await()
    }
}
