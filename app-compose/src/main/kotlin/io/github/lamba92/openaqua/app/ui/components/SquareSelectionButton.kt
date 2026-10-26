package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Deprecated(
    message = "Use ChannelSelectionCard instead",
    replaceWith = ReplaceWith("ChannelSelectionCard(name, valueText ?: \"\", selected, onClick, modifier)"),
)
@Composable
fun SquareSelectionButton(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDoubleClick: (() -> Unit)? = null,
    badgeText: String? = null,
    valueText: String? = null,
    icon: @Composable () -> Unit,
) {
    ChannelSelectionCard(
        name = name,
        valueText = valueText ?: "",
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        badgeText = badgeText,
        onDoubleClick = onDoubleClick,
        icon = icon,
    )
}
