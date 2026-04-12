package com.letta.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

val Typography.listItemHeadline: TextStyle
    get() = titleMedium

val Typography.listItemSupporting: TextStyle
    get() = bodySmall

val Typography.listItemMetadata: TextStyle
    get() = labelMedium

val Typography.listItemMetadataMonospace: TextStyle
    get() = labelMedium.copy(fontFamily = FontFamily.Monospace)

val Typography.dialogSectionHeading: TextStyle
    get() = labelLarge

val Typography.sectionTitle: TextStyle
    get() = titleSmallEmphasized

val Typography.chatBubbleSender: TextStyle
    get() = labelMediumEmphasized

val Typography.statValue: TextStyle
    get() = headlineMedium
