package com.listaio.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.listaio.domain.ShoppingItem

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "checked") val checked: Boolean,
    // "order" is a reserved SQL keyword, so the column is named "sort_order".
    @ColumnInfo(name = "sort_order") val order: Double,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    // Soft-delete flag: true = the item lives in the trash, not in the active list.
    @ColumnInfo(name = "deleted") val deleted: Boolean = false,
)

fun ItemEntity.toDomain() = ShoppingItem(
    id = id,
    name = name,
    checked = checked,
    order = order,
    updatedAt = updatedAt,
)
