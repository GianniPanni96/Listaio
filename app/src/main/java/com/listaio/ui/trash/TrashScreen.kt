package com.listaio.ui.trash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.listaio.R
import com.listaio.domain.ShoppingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trash: List<ShoppingItem>,
    onBack: () -> Unit,
    onRestore: (Collection<String>) -> Unit,
    onDeleteForever: (Collection<String>) -> Unit,
) {
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Keep the selection in sync with what's actually still in the trash.
    LaunchedEffect(trash) {
        val ids = trash.mapTo(mutableSetOf()) { it.id }
        selected = selected.intersect(ids)
    }

    val allSelected = trash.isNotEmpty() && selected.size == trash.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (trash.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selected = if (allSelected) emptySet()
                                else trash.mapTo(mutableSetOf()) { it.id }
                            },
                        ) {
                            Text(
                                stringResource(
                                    if (allSelected) R.string.deselect_all else R.string.select_all
                                )
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                TrashActionBar(
                    count = selected.size,
                    onRestore = {
                        onRestore(selected)
                        selected = emptySet()
                    },
                    onDeleteForever = { confirmDelete = true },
                )
            }
        },
    ) { innerPadding ->
        if (trash.isEmpty()) {
            EmptyTrash(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(trash, key = { it.id }) { item ->
                    val isSelected = item.id in selected
                    TrashRow(
                        name = item.name,
                        selected = isSelected,
                        onToggle = {
                            selected = if (isSelected) selected - item.id
                            else selected + item.id
                        },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        val message = if (selected.size == 1) {
            val name = trash.firstOrNull { it.id in selected }?.name.orEmpty()
            stringResource(R.string.delete_forever_message_one, name)
        } else {
            stringResource(R.string.delete_forever_message_many, selected.size)
        }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_forever_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteForever(selected)
                        selected = emptySet()
                        confirmDelete = false
                    },
                ) {
                    Text(stringResource(R.string.delete_forever))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun TrashRow(
    name: String,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun TrashActionBar(
    count: Int,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(12.dp),
        ) {
            FilledTonalButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Restore, contentDescription = null)
                Text(
                    text = stringResource(R.string.restore_count, count),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Button(
                onClick = onDeleteForever,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null)
                Text(
                    text = stringResource(R.string.delete_forever_count, count),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyTrash(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.trash_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.trash_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
