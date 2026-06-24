package com.listaio.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listaio.data.local.LocalListRepository.Companion.ORDER_GAP
import com.listaio.domain.ListRepository
import com.listaio.domain.ShoppingItem
import com.listaio.util.normalizeName
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val repository: ListRepository,
) : ViewModel() {

    val items: StateFlow<List<ShoppingItem>> = repository.observeItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val trash: StateFlow<List<ShoppingItem>> = repository.observeTrash()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Adds an item, but refuses duplicates: if an active item already has the same
     * normalized name (ignoring case/accents/spacing), it is a no-op. This mirrors the
     * UI's disabled add-button so the rule holds regardless of entry point.
     */
    fun addItem(name: String) = viewModelScope.launch {
        val normalized = normalizeName(name)
        if (normalized.isEmpty()) return@launch
        if (items.value.any { normalizeName(it.name) == normalized }) return@launch
        repository.addItem(name)
    }

    fun toggle(item: ShoppingItem) = viewModelScope.launch {
        repository.setChecked(item.id, !item.checked)
    }

    fun rename(id: String, name: String) = viewModelScope.launch {
        repository.rename(id, name)
    }

    fun moveToTrash(id: String) = viewModelScope.launch {
        repository.moveToTrash(listOf(id))
    }

    fun moveCheckedToTrash() = viewModelScope.launch {
        repository.moveCheckedToTrash()
    }

    fun restore(ids: Collection<String>) = viewModelScope.launch {
        repository.restore(ids.toList())
    }

    fun deleteForever(ids: Collection<String>) = viewModelScope.launch {
        repository.deleteForever(ids.toList())
    }

    /**
     * Reorders within the currently displayed list and persists only the moved item by
     * giving it a fractional order between its new neighbours.
     */
    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val current = items.value
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val reordered = current.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        val moved = reordered[toIndex]
        val prev = reordered.getOrNull(toIndex - 1)
        val next = reordered.getOrNull(toIndex + 1)

        val newOrder = when {
            prev == null && next == null -> moved.order
            prev == null -> next!!.order - ORDER_GAP
            next == null -> prev.order + ORDER_GAP
            else -> (prev.order + next.order) / 2.0
        }

        viewModelScope.launch { repository.move(moved.id, newOrder) }
    }
}
