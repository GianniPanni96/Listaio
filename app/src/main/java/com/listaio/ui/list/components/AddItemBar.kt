package com.listaio.ui.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.listaio.R
import com.listaio.util.normalizeName

private const val MAX_PREVIEW = 5

@Composable
fun AddItemBar(
    existingNames: List<String>,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val query = text.trim()
    val normalizedQuery = normalizeName(query)

    // Pair each existing name with its normalized form once per list change.
    val normalizedExisting = remember(existingNames) {
        existingNames.map { it to normalizeName(it) }
    }

    val matches = if (normalizedQuery.isEmpty()) {
        emptyList()
    } else {
        normalizedExisting.filter { it.second.contains(normalizedQuery) }.map { it.first }
    }
    val isDuplicate = normalizedQuery.isNotEmpty() &&
        normalizedExisting.any { it.second == normalizedQuery }
    val canAdd = query.isNotEmpty() && !isDuplicate

    fun submit() {
        if (canAdd) {
            onAdd(query)
            text = ""
        }
    }

    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        Column(
            // Lift the content above whichever is taller: the Android navigation bar
            // (keyboard closed) or the keyboard (open). With adjustResize + edge-to-edge
            // the window does NOT physically resize, so this single inset padding is what
            // moves the bar up by exactly the keyboard height — no doubling, no gap.
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(12.dp),
        ) {
            if (matches.isNotEmpty()) {
                MatchPreview(
                    matches = matches,
                    normalizedQuery = normalizedQuery,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.add_item_hint)) },
                    singleLine = true,
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text(stringResource(R.string.already_in_list, query)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(onClick = { submit() }, enabled = canAdd) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_item),
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchPreview(
    matches: List<String>,
    normalizedQuery: String,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.existing_matches_header),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        matches.take(MAX_PREVIEW).forEach { name ->
            val exact = normalizeName(name) == normalizedQuery
            Text(
                text = "•  $name",
                style = MaterialTheme.typography.bodyMedium,
                color = if (exact) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}
