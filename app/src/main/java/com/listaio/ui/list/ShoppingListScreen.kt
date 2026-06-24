package com.listaio.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.listaio.R
import com.listaio.domain.ShoppingItem
import com.listaio.ui.list.components.AddItemBar
import com.listaio.ui.list.components.ItemRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel,
    onOpenTrash: () -> Unit,
) {
    val items by viewModel.items.collectAsState()
    val trash by viewModel.trash.collectAsState()
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }
    var confirmClearChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (items.any { it.checked }) {
                        IconButton(onClick = { confirmClearChecked = true }) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_checked),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onOpenTrash,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                BadgedBox(
                    badge = {
                        if (trash.isNotEmpty()) {
                            Badge { Text(trash.size.toString()) }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.open_trash),
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            AddItemBar(
                existingNames = items.map { it.name },
                onAdd = { viewModel.addItem(it) },
            )
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            ItemList(
                items = items,
                contentPadding = innerPadding,
                onToggle = viewModel::toggle,
                onDelete = { viewModel.moveToTrash(it.id) },
                onLongPress = { editing = it },
                onMove = viewModel::moveItem,
            )
        }
    }

    editing?.let { item ->
        RenameDialog(
            initialName = item.name,
            onDismiss = { editing = null },
            onConfirm = { newName ->
                viewModel.rename(item.id, newName)
                editing = null
            },
        )
    }

    if (confirmClearChecked) {
        ConfirmDialog(
            title = stringResource(R.string.clear_checked_title),
            message = stringResource(R.string.clear_checked_message),
            confirmLabel = stringResource(R.string.move_to_trash),
            onConfirm = {
                viewModel.moveCheckedToTrash()
                confirmClearChecked = false
            },
            onDismiss = { confirmClearChecked = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemList(
    items: List<ShoppingItem>,
    contentPadding: PaddingValues,
    onToggle: (ShoppingItem) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    onLongPress: (ShoppingItem) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMove(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item ->
            ReorderableItem(reorderableState, key = item.id) { isDragging ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            onDelete(item)
                        }
                        false
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { DismissBackground() },
                ) {
                    ItemRow(
                        name = item.name,
                        checked = item.checked,
                        isDragging = isDragging,
                        onToggle = { onToggle(item) },
                        onLongPress = { onLongPress(item) },
                        dragHandle = {
                            Icon(
                                imageVector = Icons.Filled.DragHandle,
                                contentDescription = stringResource(R.string.drag_handle),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.draggableHandle(),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DismissBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Icon(
            imageVector = Icons.Filled.DeleteSweep,
            contentDescription = stringResource(R.string.delete),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember {
        mutableStateOf(TextFieldValue(initialName, selection = androidx.compose.ui.text.TextRange(initialName.length)))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
