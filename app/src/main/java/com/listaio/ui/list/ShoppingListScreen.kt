package com.listaio.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.listaio.R
import kotlinx.coroutines.launch
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
    var longPressMenu by remember { mutableStateOf<ShoppingItem?>(null) }
    var renameItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var confirmClearChecked by remember { mutableStateOf(false) }
    var moveSourceId by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()

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
                        if (trash.isNotEmpty()) Badge { Text(trash.size.toString()) }
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
            val moveMode = moveSourceId != null
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(if (moveMode) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
            ) {
                if (moveMode) {
                    val source = items.firstOrNull { it.id == moveSourceId }
                    MoveModeBanner(
                        itemName = source?.name.orEmpty(),
                        onCancel = { moveSourceId = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    ItemList(
                        items = items,
                        lazyListState = lazyListState,
                        onToggle = { it -> viewModel.toggle(it) },
                        onDelete = { it -> viewModel.moveToTrash(it.id) },
                        onLongPress = { it ->
                            if (moveMode) {
                                targetTapped(it.id, moveSourceId!!, viewModel)
                                moveSourceId = null
                            } else {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                longPressMenu = it
                            }
                        },
                        onMove = { from, to -> viewModel.moveItem(from, to) },
                        moveSourceId = moveSourceId,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ListScrollbar(
                        lazyState = lazyListState,
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }

    longPressMenu?.let { item ->
        LongPressMenu(
            itemName = item.name,
            onMove = {
                moveSourceId = item.id
                longPressMenu = null
            },
            onRename = {
                longPressMenu = null
                renameItem = item
            },
            onDelete = {
                viewModel.moveToTrash(item.id)
                longPressMenu = null
            },
            onDismiss = { longPressMenu = null },
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

    renameItem?.let { item ->
        RenameDialog(
            initialName = item.name,
            onDismiss = { renameItem = null },
            onConfirm = { newName ->
                viewModel.rename(item.id, newName)
                renameItem = null
            },
        )
    }
}

private fun targetTapped(
    targetId: String,
    sourceId: String,
    viewModel: ShoppingListViewModel,
) {
    if (targetId == sourceId) return
    viewModel.moveItemBelow(sourceId, targetId)
}

@Composable
private fun MoveModeBanner(
    itemName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.MoveToInbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.move_mode_hint, itemName),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.move_mode_cancel))
            }
        }
    }
}

@Composable
private fun LongPressMenu(
    itemName: String,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onDismiss),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .width(262.dp),
        ) {
            Column {
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                )
                Divider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)
                MenuRow(
                    icon = Icons.Filled.MoveToInbox,
                    label = stringResource(R.string.menu_move_item),
                    onClick = onMove,
                )
                MenuRow(
                    icon = Icons.Filled.Edit,
                    label = stringResource(R.string.menu_rename),
                    onClick = onRename,
                )
                MenuRow(
                    icon = Icons.Filled.Delete,
                    label = stringResource(R.string.move_to_trash),
                    danger = true,
                    onClick = onDelete,
                )
                Divider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)
                MenuRow(icon = Icons.Filled.Close, label = stringResource(R.string.cancel), onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemList(
    items: List<ShoppingItem>,
    lazyListState: LazyListState,
    onToggle: (ShoppingItem) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    onLongPress: (ShoppingItem) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    moveSourceId: String?,
    modifier: Modifier = Modifier,
) {
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMove(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp),
        modifier = modifier,
    ) {
        items(items, key = { it.id }) { item ->
            ReorderableItem(reorderableState, key = item.id) { isDragging ->
                val dismiss = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            onDelete(item)
                        }
                        false
                    },
                    // Make the swipe deliberately less reactive: the box only dismisses
                    // once the finger has travelled at least 50% of the full width.
                    positionalThreshold = { it * 0.5f },
                )
                SwipeToDismissBox(
                    state = dismiss,
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
                        moveModeActive = moveSourceId != null,
                        isMoveSource = moveSourceId == item.id,
                        onMoveTarget = { onLongPress(item) },
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
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterStart,
    ) {
        Icon(
            imageVector = Icons.Filled.DeleteSweep,
            contentDescription = stringResource(R.string.delete),
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(start = 20.dp),
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
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(initialName)) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListScrollbar(
    lazyState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
    ) {
        val trackPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(lazyState, scope, trackPx) {
                    var lastTarget = -1
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, _ ->
                            val fraction = (change.position.y / trackPx).coerceIn(0f, 1f)
                            val total = lazyState.layoutInfo.totalItemsCount.coerceAtLeast(1)
                            val target = (fraction * (total - 1)).toInt().coerceIn(0, total - 1)
                            if (target != lastTarget) {
                                lastTarget = target
                                scope.launch { lazyState.scrollToItem(target) }
                            }
                            change.consume()
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.28f)
                    .width(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            )
        }
    }
}
