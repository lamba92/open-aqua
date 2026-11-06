package io.github.lamba92.openaqua.app.feature.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.CenteredScreenCard
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.select_a_device_subtitle
import open_aqua.app_compose.generated.resources.select_a_device_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SelectADeviceScreen(modifier: Modifier = Modifier.Companion) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    CenteredScreenCard(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
    ) {
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            tint = colors.primaryBright,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.select_a_device_title),
            color = colors.textPrimary,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.select_a_device_subtitle),
            color = colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}
