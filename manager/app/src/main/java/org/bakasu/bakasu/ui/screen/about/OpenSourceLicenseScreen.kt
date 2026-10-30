package org.bakasu.bakasu.ui.screen.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.LocalPolice
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.util.withJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.R
import org.bakasu.bakasu.ui.component.WarningCard
import org.bakasu.bakasu.ui.component.settings.AppBackButton
import org.bakasu.bakasu.ui.component.settings.SettingsValueRow
import org.bakasu.bakasu.ui.component.settings.lazySegmentColumn
import org.bakasu.bakasu.ui.navigation.LocalNavigator
import org.bakasu.bakasu.ui.screen.LabelText
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurEffect
import org.bakasu.bakasu.ui.theme.blurSource
import org.bakasu.bakasu.ui.util.adaptiveScaffoldWindowInsets
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun OpenSourceLicenseScreen() {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior(
        state = rememberTopAppBarState(
            initialHeightOffset = -154f,
            initialHeightOffsetLimit = -154f, // from debugger
        ),
    )

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    // from https://github.com/mikepenz/AboutLibraries#setup
    val context = LocalContext.current
    val libraries by produceState(initialValue = Libs(emptyList(), emptySet()), context) {
        value = withContext(Dispatchers.IO) {
            Libs.Builder().withJson(context, R.raw.aboutlibraries).build()
        }
    }

    var selectedLibrary by remember { mutableStateOf<Library?>(null) }
    val uriHandler = LocalUriHandler.current

    MiuixScaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            SmallTopAppBar(
                modifier = Modifier.blurEffect(),
                title = stringResource(id = R.string.open_source_license),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    AppBackButton(
                        onClick = { navigator.pop() },
                        icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                        modifier = Modifier.size(36.dp),
                        containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.1f,
                        ),
                    )
                },
                color =
                    if (themeConfig.isEnableBlur) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer.copy(cardConfig.cardAlpha)
                    },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blurSource(),
            contentPadding = paddingValues,
        ) {
            lazySegmentColumn(libraries.libraries) { _, lib ->
                SettingsValueRow(
                    title = lib.name,
                    summary = lib.author,
                    bottomContent = {
                        Row {
                            lib.licenses.forEach {
                                LabelText(it.name)
                            }
                        }
                    },
                    onClick = {
                        selectedLibrary = lib
                    },
                    endContent = {
                        lib.artifactVersion?.let {
                            Text(it)
                        }
                    },
                )
            }
        }
        OverlayDialog(
            show = selectedLibrary != null,
            onDismissRequest = { selectedLibrary = null },
            title = selectedLibrary?.name,
        ) {
            val library = selectedLibrary
            if (library != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        WarningCard(
                            color = MaterialTheme.colorScheme.tertiary,
                            renderBackground = false,
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.LocalPolice,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            message = stringResource(
                                R.string.license,
                                library.licenses.joinToString(separator = ", ") { it.name },
                            ),
                        )
                    }

                    items(library.licenses.toList()) { license ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Row {
                                    Text(
                                        text = license.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                license.url?.let { url ->
                                                    uriHandler.openUri(url)
                                                }
                                            },
                                    )
                                }

                                Spacer(modifier = Modifier.size(8.dp))

                                Text(
                                    text = license.licenseContent
                                        ?: stringResource(R.string.no_license_text),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            library.website?.let { url ->
                                TextButton(
                                    text = stringResource(R.string.visit_home_page),
                                    onClick = { uriHandler.openUri(url) },
                                )
                            }
                            Button(
                                modifier = Modifier.padding(start = 8.dp),
                                onClick = { selectedLibrary = null },
                            ) {
                                Text(text = stringResource(R.string.close))
                            }
                        }
                    }
                }
            }
        }
    }
}
