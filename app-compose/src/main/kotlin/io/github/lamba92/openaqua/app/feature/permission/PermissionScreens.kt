package io.github.lamba92.openaqua.app.feature.permission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.permission.DevicePermissionState
import io.github.lamba92.openaqua.app.permission.LINUX_UDEV_RULE_INSTALL_SCRIPT
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.CenteredScreenCard
import io.github.lamba92.openaqua.app.ui.components.OpenAquaButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaHorizontalScrollbar
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_warning
import open_aqua.app_compose.generated.resources.permission_dialog_linux_desc
import open_aqua.app_compose.generated.resources.permission_dialog_linux_install
import open_aqua.app_compose.generated.resources.permission_dialog_linux_title
import open_aqua.app_compose.generated.resources.permission_dialog_windows_desc
import open_aqua.app_compose.generated.resources.permission_dialog_windows_elevate
import open_aqua.app_compose.generated.resources.permission_dialog_windows_title
import open_aqua.app_compose.generated.resources.permission_hide_script
import open_aqua.app_compose.generated.resources.permission_installing
import open_aqua.app_compose.generated.resources.permission_loading_desc
import open_aqua.app_compose.generated.resources.permission_show_script
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel = rememberInstance(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    when (val permState = state.permissionState) {
        DevicePermissionState.Initializing -> {
            PermissionLoadingScreen(modifier = modifier)
        }

        is DevicePermissionState.Denied -> {
            PermissionDeniedScreen(
                state = permState,
                isRequesting = state.isRequesting,
                onRequest = { viewModel.onEvent(PermissionEvent.RequestPermissions) },
                modifier = modifier,
            )
        }

        DevicePermissionState.Granted -> {
            Box(modifier = modifier.fillMaxSize())
        }
    }
}

@Composable
fun PermissionLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = OpenAquaTheme.colors.primaryBright,
            )
            Text(
                text = stringResource(Res.string.permission_loading_desc),
                color = OpenAquaTheme.colors.textSecondary,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun PermissionDeniedScreen(
    state: DevicePermissionState.Denied,
    isRequesting: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val isWindows = state is DevicePermissionState.Denied.WindowsElevationRequired
    var isScriptExpanded by rememberSaveable { mutableStateOf(false) }

    val title =
        stringResource(
            if (isWindows) Res.string.permission_dialog_windows_title else Res.string.permission_dialog_linux_title,
        )
    val desc =
        stringResource(
            if (isWindows) Res.string.permission_dialog_windows_desc else Res.string.permission_dialog_linux_desc,
        )
    val buttonText =
        stringResource(
            if (isWindows) Res.string.permission_dialog_windows_elevate else Res.string.permission_dialog_linux_install,
        )

    CenteredScreenCard(
        modifier = modifier,
        cardModifier = Modifier.widthIn(max = if (isScriptExpanded) 600.dp else 480.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_warning),
            contentDescription = null,
            tint = colors.statusWarning,
            modifier = Modifier.size(40.dp),
        )

        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = desc,
            color = colors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        if (!isWindows) {
            val arrowRotation by animateFloatAsState(
                targetValue = if (isScriptExpanded) 90f else 0f,
                label = "arrowRotation",
            )
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(metrics.radiusSmall))
                        .clickable { isScriptExpanded = !isScriptExpanded }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.primaryBright,
                    modifier = Modifier.size(18.dp).rotate(arrowRotation),
                )
                Text(
                    text =
                        stringResource(
                            if (isScriptExpanded) Res.string.permission_hide_script else Res.string.permission_show_script,
                        ),
                    color = colors.primaryBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            AnimatedVisibility(
                visible = isScriptExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val vScroll = rememberScrollState()
                val hScroll = rememberScrollState()
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(metrics.radiusMedium))
                            .background(colors.surfaceLowest)
                            .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusMedium)),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(vScroll)
                                .horizontalScroll(hScroll)
                                .padding(start = 12.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
                    ) {
                        SelectionContainer {
                            Text(
                                text = LINUX_UDEV_RULE_INSTALL_SCRIPT,
                                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = colors.textPrimary,
                                softWrap = false,
                            )
                        }
                    }
                    OpenAquaVerticalScrollbar(
                        scrollState = vScroll,
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                    )
                    OpenAquaHorizontalScrollbar(
                        scrollState = hScroll,
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OpenAquaButton(
            text = buttonText,
            onClick = onRequest,
            isLoading = isRequesting,
            loadingText = stringResource(Res.string.permission_installing),
            containerColor = colors.primaryBright,
            contentColor = colors.background,
        )
    }
}
