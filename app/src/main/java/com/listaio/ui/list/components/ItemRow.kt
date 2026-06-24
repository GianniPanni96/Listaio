package com.listaio.ui.list.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * A single list row: checkbox + name + drag handle. Tap toggles the check, long-press
 * triggers rename. The [dragHandle] slot is supplied by the caller so it can carry the
 * reorderable drag modifier from the enclosing item scope.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    name: String,
    checked: Boolean,
    isDragging: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    dragHandle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tonalElevation = if (isDragging) 8.dp else 0.dp
    Surface(
        tonalElevation = tonalElevation,
        shadowElevation = tonalElevation,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .combinedClickable(
                    onClick = onToggle,
                    onLongClick = onLongPress,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (checked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            Row(modifier = Modifier.size(48.dp), verticalAlignment = Alignment.CenterVertically) {
                dragHandle()
            }
        }
    }
}
