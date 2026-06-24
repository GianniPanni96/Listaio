package com.listaio.domain

/**
 * A single shopping-list entry. This is the domain model the UI works with — it is
 * deliberately storage-agnostic so the same model can be backed by Room today and by
 * Firestore later (see FIRESTORE.md).
 *
 * @param order fractional index used for ordering. Inserting/moving an item only needs
 *   a single new value between its neighbours (no rewrite of the whole list).
 */
data class ShoppingItem(
    val id: String,
    val name: String,
    val checked: Boolean,
    val order: Double,
    val updatedAt: Long,
)
