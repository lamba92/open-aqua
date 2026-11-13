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
import open_aqua.app_compose.generated.resources.device_aquaero
import open_aqua.app_compose.generated.resources.device_aquastream_ultimate
import open_aqua.app_compose.generated.resources.device_aquastream_xt
import open_aqua.app_compose.generated.resources.device_d5_next
import open_aqua.app_compose.generated.resources.device_farbwerk
import open_aqua.app_compose.generated.resources.device_farbwerk360
import open_aqua.app_compose.generated.resources.device_high_flow_next
import open_aqua.app_compose.generated.resources.device_high_flow_usb
import open_aqua.app_compose.generated.resources.device_leakshield
import open_aqua.app_compose.generated.resources.device_octo
import open_aqua.app_compose.generated.resources.device_poweradjust3
import open_aqua.app_compose.generated.resources.device_quadro
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val DeviceType.drawable: DrawableResource
    get() =
        when (this) {
            DeviceType.OCTO -> Res.drawable.device_octo
            DeviceType.QUADRO -> Res.drawable.device_quadro
            DeviceType.D5_NEXT -> Res.drawable.device_d5_next
            DeviceType.FARBWERK_360 -> Res.drawable.device_farbwerk360
            DeviceType.FARBWERK -> Res.drawable.device_farbwerk
            DeviceType.HIGH_FLOW_NEXT -> Res.drawable.device_high_flow_next
            DeviceType.LEAKSHIELD -> Res.drawable.device_leakshield
            DeviceType.AQUASTREAM_ULTIMATE -> Res.drawable.device_aquastream_ultimate
            DeviceType.AQUAERO -> Res.drawable.device_aquaero
            DeviceType.HIGH_FLOW_USB -> Res.drawable.device_high_flow_usb
            DeviceType.POWERADJUST3 -> Res.drawable.device_poweradjust3
            DeviceType.AQUASTREAM_XT -> Res.drawable.device_aquastream_xt
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
