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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.build.OpenAquaBuildInfo
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.app.ui.components.ScreenHeaderSection
import io.github.lamba92.openaqua.app.ui.components.SectionHeader
import io.github.lamba92.openaqua.app.ui.components.scrollFadingEdge
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.about_author_desc
import open_aqua.app_compose.generated.resources.about_author_handle
import open_aqua.app_compose.generated.resources.about_author_name
import open_aqua.app_compose.generated.resources.about_author_title
import open_aqua.app_compose.generated.resources.about_build_info_title
import open_aqua.app_compose.generated.resources.about_dependencies_title
import open_aqua.app_compose.generated.resources.about_github_button
import open_aqua.app_compose.generated.resources.about_github_desc
import open_aqua.app_compose.generated.resources.about_github_title
import open_aqua.app_compose.generated.resources.about_project_desc
import open_aqua.app_compose.generated.resources.about_project_title
import open_aqua.app_compose.generated.resources.about_screen_subtitle
import open_aqua.app_compose.generated.resources.about_screen_title
import open_aqua.app_compose.generated.resources.about_tab_open_aqua
import open_aqua.app_compose.generated.resources.about_tab_subtitle
import open_aqua.app_compose.generated.resources.about_tab_supported_devices
import open_aqua.app_compose.generated.resources.ic_device
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.ic_openaqua
import open_aqua.app_compose.generated.resources.nav_about
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val GITHUB_REPO_URL = "https://github.com/lamba92/open-aqua"

@Composable
fun AboutScreen(
    selectedTab: AboutTab = AboutTab.OPEN_AQUA,
    onSelectTab: (AboutTab) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val openAquaScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val supportedDevicesScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    AboutDrawerScaffold(
        selectedTab = selectedTab,
        onSelectTab = onSelectTab,
        modifier = modifier,
    ) {
        saveableStateHolder.SaveableStateProvider(selectedTab) {
            when (selectedTab) {
                AboutTab.OPEN_AQUA -> {
                    AboutOpenAquaContent(scrollState = openAquaScrollState)
                }

                AboutTab.SUPPORTED_DEVICES -> {
                    SupportedDevicesContent(scrollState = supportedDevicesScrollState)
                }
            }
        }
    }
}

@Composable
private fun AboutDrawerScaffold(
    selectedTab: AboutTab,
    onSelectTab: (AboutTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = OpenAquaTheme.colors
    val drawerMetrics = OpenAquaTheme.metrics.navigationDrawer
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier =
                    Modifier
                        .width(drawerMetrics.width)
                        .fillMaxHeight(),
                drawerContainerColor = colors.surfaceLow,
                drawerContentColor = colors.textPrimary,
                drawerShape = RoundedCornerShape(0.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .padding(
                                horizontal = drawerMetrics.headerPaddingHorizontal,
                                vertical = drawerMetrics.headerPaddingVertical,
                            ),
                    verticalArrangement = Arrangement.spacedBy(drawerMetrics.itemSpacing),
                ) {
                    Text(
                        text = stringResource(Res.string.nav_about),
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Text(
                        text = stringResource(Res.string.about_tab_subtitle),
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.borderOutline)
                    Spacer(modifier = Modifier.height(8.dp))

                    val drawerColors =
                        NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = colors.surfaceHigh,
                            unselectedContainerColor = Color.Transparent,
                            selectedIconColor = colors.primaryBright,
                            unselectedIconColor = colors.textSecondary,
                            selectedTextColor = colors.textPrimary,
                            unselectedTextColor = colors.textSecondary,
                        )

                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = stringResource(Res.string.about_tab_open_aqua),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                fontWeight = if (selectedTab == AboutTab.OPEN_AQUA) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_openaqua),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        selected = selectedTab == AboutTab.OPEN_AQUA,
                        onClick = { onSelectTab(AboutTab.OPEN_AQUA) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(8.dp),
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = stringResource(Res.string.about_tab_supported_devices),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                fontWeight = if (selectedTab == AboutTab.SUPPORTED_DEVICES) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_device),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        selected = selectedTab == AboutTab.SUPPORTED_DEVICES,
                        onClick = { onSelectTab(AboutTab.SUPPORTED_DEVICES) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        content = content,
    )
}

@Composable
private fun AboutOpenAquaContent(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val uriHandler = LocalUriHandler.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ScreenHeaderSection(
                title = stringResource(Res.string.about_screen_title),
                subtitle = stringResource(Res.string.about_screen_subtitle),
            )

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
                            title = stringResource(Res.string.about_build_info_title),
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

                    // Dependencies Card
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
                            title = stringResource(Res.string.about_dependencies_title),
                        )

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(metrics.radiusMedium))
                                    .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusMedium)),
                        ) {
                            OpenAquaBuildInfo.LIBRARIES.forEachIndexed { index, library ->
                                val rowBackground =
                                    if (index % 2 == 1) {
                                        colors.surfaceLowest
                                    } else {
                                        Color.Transparent
                                    }

                                LibraryRow(
                                    library = library,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .background(rowBackground)
                                            .padding(horizontal = metrics.spacingLg, vertical = metrics.spacingMd),
                                )
                            }
                        }
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
fun BuildInfoRow(
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

@Composable
private fun LibraryRow(
    library: OpenAquaBuildInfo.Library,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = library.artifact,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            )
            Text(
                text = library.coordinates,
                fontSize = 11.sp,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                color = colors.textMuted,
            )
        }
        Text(
            text = library.version,
            fontSize = 12.sp,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            color = colors.primaryBright,
        )
    }
}
