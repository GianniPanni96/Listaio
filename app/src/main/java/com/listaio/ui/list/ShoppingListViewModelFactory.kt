package com.listaio.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.listaio.domain.ListRepository

class ShoppingListViewModelFactory(
    private val repository: ListRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ShoppingListViewModel(repository) as T
    }
}
