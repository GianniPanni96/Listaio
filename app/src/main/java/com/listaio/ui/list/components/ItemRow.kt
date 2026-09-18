package com.listaio.ui.list.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * A single list row: checkbox + name + drag handle.
 *
 * Behavior depends on [moveModeActive]:
 * - normal mode: tap toggles the checkbox, long-press triggers the context menu.
 * - move mode: the row is a tap target. If this row is the *source*
 *   ([isMoveSource]) the checkbox is disabled; otherwise tapping confirms the
 *   move of the source item below this one.
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
    moveModeActive: Boolean = false,
    isMoveSource: Boolean = false,
    onMoveTarget: () -> Unit = {},
) {
    val boxColor = when {
        isDragging -> MaterialTheme.colorScheme.surface
        isMoveSource -> MaterialTheme.colorScheme.primaryContainer
        moveModeActive -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val onBox = when {
        isMoveSource -> MaterialTheme.colorScheme.onPrimaryContainer
        moveModeActive -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border = if (isMoveSource) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else if (moveModeActive) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        BorderStroke(0.dp, Color.Transparent)
    }
    val elevation = if (isDragging) 8.dp else if (isMoveSource) 2.dp else 0.dp
    Surface(
        tonalElevation = elevation,
        shadowElevation = elevation,
        color = boxColor,
        border = border,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .combinedClickable(
                    enabled = !isMoveSource,
                    onClick = { if (moveModeActive) onMoveTarget() else onToggle() },
                    onLongClick = { if (!moveModeActive) onLongPress() },
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Checkbox(
                checked = checked,
                enabled = !moveModeActive,
                onCheckedChange = { onToggle() },
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else onBox,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            Row(
                modifier = Modifier
                    .size(48.dp)
                    .alpha(if (moveModeActive) 0.3f else 1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dragHandle()
            }
        }
    }
}
