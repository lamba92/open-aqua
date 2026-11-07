package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLowest
import io.github.lamba92.openaqua.model.device.DeviceType
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.device_d5_next
import open_aqua.app_compose.generated.resources.device_octo
import open_aqua.app_compose.generated.resources.device_quadro
import open_aqua.app_compose.generated.resources.ic_device
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val DeviceType.drawable: DrawableResource
    get() =
        when (this) {
            DeviceType.OCTO -> Res.drawable.device_octo
            DeviceType.QUADRO -> Res.drawable.device_quadro
            DeviceType.D5_NEXT -> Res.drawable.device_d5_next
            else -> Res.drawable.ic_device
        }

@Composable
fun DeviceIllustration(
    type: DeviceType,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceContainerHigh, RoundedCornerShape(16.dp))
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(type.drawable),
            contentDescription = type.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        // Overlay label in bottom corner
        Text(
            text = type.name,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            color = CyanBright.copy(alpha = 0.6f),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
        )
    }
}
