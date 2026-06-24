package com.listaio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.listaio.ui.list.ShoppingListScreen
import com.listaio.ui.list.ShoppingListViewModel
import com.listaio.ui.trash.TrashScreen

/**
 * Top-level navigation host. Two screens (list ↔ trash) switched by a single flag —
 * deliberately dependency-free (no navigation library) to keep things simple.
 */
@Composable
fun ListaioApp(viewModel: ShoppingListViewModel) {
    var showTrash by rememberSaveable { mutableStateOf(false) }

    if (showTrash) {
        val trash by viewModel.trash.collectAsState()
        BackHandler { showTrash = false }
        TrashScreen(
            trash = trash,
            onBack = { showTrash = false },
            onRestore = viewModel::restore,
            onDeleteForever = viewModel::deleteForever,
        )
    } else {
        ShoppingListScreen(
            viewModel = viewModel,
            onOpenTrash = { showTrash = true },
        )
    }
}
