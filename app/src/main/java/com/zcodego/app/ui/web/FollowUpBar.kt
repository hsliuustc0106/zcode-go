package com.zcodego.app.ui.web

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.zcodego.app.R
import com.zcodego.app.remote.InsertOutcome

@Composable
internal fun FollowUpBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onInsert: (String, (InsertOutcome) -> Unit) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var draft by rememberSaveable { mutableStateOf("") }
    var inserting by rememberSaveable { mutableStateOf(false) }
    val inserted = stringResource(R.string.followup_inserted)
    val missing = stringResource(R.string.followup_no_target)
    val copied = stringResource(R.string.followup_copied)
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onExpandedChange(!expanded) }) {
                    Text(stringResource(if (expanded) R.string.followup_collapse else R.string.followup_open))
                }
            }
            if (expanded) {
                Text(
                    text = stringResource(R.string.followup_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text(stringResource(R.string.followup_placeholder)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default,
                    ),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            if (draft.isBlank()) return@TextButton
                            copyPlainText(context, draft)
                            onMessage(copied)
                        },
                        enabled = draft.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.followup_copy))
                    }
                    TextButton(
                        onClick = {
                            if (draft.isBlank() || inserting) return@TextButton
                            inserting = true
                            val text = draft
                            onInsert(text) { outcome ->
                                inserting = false
                                if (outcome == InsertOutcome.INSERTED) {
                                    draft = ""
                                    onMessage(inserted)
                                } else {
                                    onMessage(missing)
                                }
                            }
                        },
                        enabled = draft.isNotBlank() && !inserting,
                    ) {
                        Text(stringResource(R.string.followup_insert))
                    }
                }
            }
        }
    }
}

private fun copyPlainText(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("zcode-go", text))
}
