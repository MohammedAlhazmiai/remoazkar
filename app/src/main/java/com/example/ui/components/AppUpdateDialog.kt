package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.AppUpdateInfo
import com.example.data.tr

sealed class UpdateDialogState {
    data class Available(val update: AppUpdateInfo) : UpdateDialogState()
    object Checking : UpdateDialogState()
    object UpToDate : UpdateDialogState()
    object NoPublishedRelease : UpdateDialogState()
    object Failed : UpdateDialogState()
}

@Composable
fun AppUpdateDialog(
    state: UpdateDialogState,
    onDismiss: () -> Unit,
    onDownload: (AppUpdateInfo) -> Unit
) {
    val update = (state as? UpdateDialogState.Available)?.update
    val title = when (state) {
        is UpdateDialogState.Available -> tr("update_available")
        UpdateDialogState.Checking -> tr("checking_updates")
        UpdateDialogState.UpToDate -> tr("app_is_current")
        UpdateDialogState.NoPublishedRelease -> tr("no_published_release")
        UpdateDialogState.Failed -> tr("update_check_failed")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            when (state) {
                is UpdateDialogState.Available -> Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tr("update_version").replace("{version}", state.update.version))
                    if (state.update.releaseNotes.isNotBlank()) {
                        Text(state.update.releaseNotes, style = MaterialTheme.typography.bodySmall)
                    }
                }
                UpdateDialogState.Checking -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(tr("checking_updates"))
                }
                UpdateDialogState.UpToDate -> Text(tr("app_is_current_message"))
                UpdateDialogState.NoPublishedRelease -> Text(tr("no_published_release_message"))
                UpdateDialogState.Failed -> Text(tr("update_check_failed_message"))
            }
        },
        confirmButton = {
            if (update != null) {
                TextButton(onClick = { onDownload(update) }) { Text(tr("download_update")) }
            } else {
                TextButton(onClick = onDismiss) { Text(tr("close")) }
            }
        },
        dismissButton = if (update != null) {
            { TextButton(onClick = onDismiss) { Text(tr("later")) } }
        } else {
            null
        }
    )
}
