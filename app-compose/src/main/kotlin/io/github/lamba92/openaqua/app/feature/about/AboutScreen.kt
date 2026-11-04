package io.github.lamba92.openaqua.app.feature.about

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.build.OpenAquaBuildInfo
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.app.ui.components.ScreenHeader
import io.github.lamba92.openaqua.app.ui.components.SectionHeader
import io.github.lamba92.openaqua.app.ui.components.scrollFadingEdge
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.about_author_desc
import open_aqua.app_compose.generated.resources.about_author_handle
import open_aqua.app_compose.generated.resources.about_author_name
import open_aqua.app_compose.generated.resources.about_author_title
import open_aqua.app_compose.generated.resources.about_github_button
import open_aqua.app_compose.generated.resources.about_github_desc
import open_aqua.app_compose.generated.resources.about_github_title
import open_aqua.app_compose.generated.resources.about_project_desc
import open_aqua.app_compose.generated.resources.about_project_title
import open_aqua.app_compose.generated.resources.about_screen_subtitle
import open_aqua.app_compose.generated.resources.about_screen_title
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.ic_openaqua
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val GITHUB_REPO_URL = "https://github.com/lamba92/open-aqua"

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val uriHandler = LocalUriHandler.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.spacingXxl)
                            .padding(top = metrics.spacingXxl, bottom = metrics.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                ) {
                    ScreenHeader(
                        title = stringResource(Res.string.about_screen_title),
                        subtitle = stringResource(Res.string.about_screen_subtitle),
                    )
                }

                HorizontalDivider(
                    color = colors.borderOutline,
                    modifier = Modifier.padding(horizontal = metrics.spacingXxl),
                )
            }

            val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .scrollFadingEdge(
                            color = colors.background,
                            scrollState = scrollState,
                            height = 36.dp,
                        ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = metrics.spacingXxl)
                            .padding(top = metrics.spacingLg, bottom = metrics.spacingXxl),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingXl),
                ) {
                    // Project Card
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(metrics.radiusLarge))
                                .background(colors.surfaceLow)
                                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                                .padding(metrics.spacingXl),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_openaqua),
                                    contentDescription = null,
                                    tint = colors.primaryBright,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "OpenAqua",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        text = "v${OpenAquaBuildInfo.VERSION}",
                                        fontSize = 12.sp,
                                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                        color = colors.primaryBright,
                                    )
                                }
                                Text(
                                    text = stringResource(Res.string.about_project_title),
                                    fontSize = 12.sp,
                                    color = colors.textMuted,
                                )
                            }
                        }

                        Text(
                            text = stringResource(Res.string.about_project_desc),
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        )
                    }

                    // Build Info Card
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(metrics.radiusLarge))
                                .background(colors.surfaceLow)
                                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                                .padding(metrics.spacingXl),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                    ) {
                        SectionHeader(
                            icon = painterResource(Res.drawable.ic_info),
                            title = "Build Information",
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingSm)) {
                            BuildInfoRow(label = "Version", value = OpenAquaBuildInfo.VERSION)
                            BuildInfoRow(label = "Build Date", value = OpenAquaBuildInfo.BUILD_TIMESTAMP)
                            BuildInfoRow(label = "Gradle Version", value = OpenAquaBuildInfo.GRADLE_VERSION)
                            BuildInfoRow(label = "Kotlin Version", value = OpenAquaBuildInfo.KOTLIN_VERSION)
                            BuildInfoRow(
                                label = "JVM",
                                value = "${OpenAquaBuildInfo.JVM_VERSION} (${OpenAquaBuildInfo.JVM_VENDOR})",
                            )
                            BuildInfoRow(
                                label = "Target OS",
                                value = "${OpenAquaBuildInfo.OS_NAME} (${OpenAquaBuildInfo.OS_ARCH})",
                            )
                        }
                    }

                    // Author Card
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(metrics.radiusLarge))
                                .background(colors.surfaceLow)
                                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                                .padding(metrics.spacingXl),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                    ) {
                        SectionHeader(
                            icon = painterResource(Res.drawable.ic_info),
                            title = stringResource(Res.string.about_author_title),
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(Res.string.about_author_name),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = stringResource(Res.string.about_author_handle),
                                fontSize = 13.sp,
                                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                color = colors.primaryBright,
                            )
                        }

                        Text(
                            text = stringResource(Res.string.about_author_desc),
                            color = colors.textMuted,
                            fontSize = 13.sp,
                        )
                    }

                    // GitHub Card
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(metrics.radiusLarge))
                                .background(colors.surfaceLow)
                                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                                .padding(metrics.spacingXxl),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                    ) {
                        SectionHeader(
                            icon = painterResource(Res.drawable.ic_info),
                            title = stringResource(Res.string.about_github_title),
                        )

                        Text(
                            text = stringResource(Res.string.about_github_desc),
                            color = colors.textMuted,
                            fontSize = 13.sp,
                        )

                        Text(
                            text = GITHUB_REPO_URL,
                            color = colors.primaryBright,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                            fontSize = 13.sp,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OpenAquaButton(
                            text = stringResource(Res.string.about_github_button),
                            onClick = {
                                runCatching { uriHandler.openUri(GITHUB_REPO_URL) }
                            },
                        )
                    }
                }

                OpenAquaVerticalScrollbar(
                    scrollState = scrollState,
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun BuildInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = colors.textMuted,
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            color = colors.textPrimary,
        )
    }
}
