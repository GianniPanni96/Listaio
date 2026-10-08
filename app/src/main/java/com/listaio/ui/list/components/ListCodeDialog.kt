package com.listaio.ui.list.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listaio.R

/**
 * Mostra il codice della lista condivisa, che dopo l'onboarding non era più visibile.
 * Serve per unire altri dispositivi e per collegare l'assistente vocale VoiceHome.
 */
@Composable
fun ListCodeDialog(code: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.list_code_copied)
    val shareText = stringResource(R.string.list_code_share_text, code)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.list_code_title)) },
        text = {
            Column {
                Text(
                    text = code,
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.list_code_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("Codice lista", code))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.list_code_copy)) }
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                    context.startActivity(Intent.createChooser(send, null))
                }) { Text(stringResource(R.string.list_code_share)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
