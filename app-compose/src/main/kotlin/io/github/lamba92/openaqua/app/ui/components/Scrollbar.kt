package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme

@Composable
fun openAquaScrollbarStyle(): ScrollbarStyle {
    val colors = OpenAquaTheme.colors
    return ScrollbarStyle(
        minimalHeight = 28.dp,
        thickness = 8.dp,
        shape = RoundedCornerShape(4.dp),
        hoverDurationMillis = 200,
        unhoverColor = colors.borderOutline.copy(alpha = 0.5f),
        hoverColor = colors.primaryBright.copy(alpha = 0.85f),
    )
}

@Composable
fun OpenAquaVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    style: ScrollbarStyle = openAquaScrollbarStyle(),
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        style = style,
    )
}

@Composable
fun OpenAquaVerticalScrollbar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    style: ScrollbarStyle = openAquaScrollbarStyle(),
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(lazyListState),
        modifier = modifier,
        style = style,
    )
}

@Composable
fun OpenAquaHorizontalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    style: ScrollbarStyle = openAquaScrollbarStyle(),
) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        style = style,
    )
}
