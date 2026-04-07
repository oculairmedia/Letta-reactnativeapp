package com.letta.mobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class MessageAction(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun MessageContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    onRegenerate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text("Copy") },
            onClick = { onCopy(); onDismiss() },
            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") },
        )
        if (onRegenerate != null) {
            DropdownMenuItem(
                text = { Text("Regenerate") },
                onClick = { onRegenerate(); onDismiss() },
                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = "Regenerate") },
            )
        }
        if (onDelete != null) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { onDelete(); onDismiss() },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
            )
        }
    }
}
