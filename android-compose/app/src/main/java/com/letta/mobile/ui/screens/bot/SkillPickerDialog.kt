package com.letta.mobile.ui.screens.bot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.letta.mobile.R
import com.letta.mobile.bot.skills.BotSkill
import com.letta.mobile.bot.skills.BotSkillActivationRule
import com.letta.mobile.ui.components.ConfirmDialog
import java.time.LocalDate

@Composable
fun SkillPickerDialog(
    availableSkills: List<BotSkill>,
    selectedSkillIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    var selection by remember(availableSkills, selectedSkillIds) { mutableStateOf(selectedSkillIds.toSet()) }

    ConfirmDialog(
        show = true,
        title = "Choose Skills",
        confirmText = stringResource(R.string.action_save),
        dismissText = stringResource(R.string.action_cancel),
        onConfirm = { onConfirm(selection.toList()) },
        onDismiss = onDismiss,
    ) {
        if (availableSkills.isEmpty()) {
            Text(
                text = "No bundled skills are available.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(availableSkills, key = { it.id }) { skill ->
                    TextButton(
                        onClick = {
                            selection = if (skill.id in selection) {
                                selection - skill.id
                            } else {
                                selection + skill.id
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = skill.id in selection,
                            onCheckedChange = null,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = skill.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = skill.id,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            skill.description.takeIf { it.isNotBlank() }?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = activationLabel(skill),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (skill.activationRule.isActive(LocalDate.now())) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun activationLabel(skill: BotSkill): String = when (skill.activationRule) {
    BotSkillActivationRule.Always -> "Always active"
    is BotSkillActivationRule.WeekdayOnly -> if (skill.activationRule.isActive(LocalDate.now())) {
        "Weekday skill • active today"
    } else {
        "Weekday skill • inactive today"
    }
}
