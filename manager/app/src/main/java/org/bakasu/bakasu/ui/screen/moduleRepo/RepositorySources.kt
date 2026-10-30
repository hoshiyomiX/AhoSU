package org.bakasu.bakasu.ui.screen.moduleRepo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.DeleteOutline
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.model.RepositoryApiType
import org.bakasu.bakasu.domain.model.RepositorySource
import org.bakasu.bakasu.ui.component.CatalogCard
import org.bakasu.bakasu.ui.component.CatalogCardHeading
import org.bakasu.bakasu.ui.component.ConfirmResult
import org.bakasu.bakasu.ui.component.SwipeableSnackbarHost
import org.bakasu.bakasu.ui.component.rememberConfirmDialog
import org.bakasu.bakasu.ui.component.rememberCustomDialog
import org.bakasu.bakasu.ui.component.repositoryErrorText
import org.bakasu.bakasu.ui.component.settings.AppBackButton
import org.bakasu.bakasu.ui.component.settings.SegmentedColumn
import org.bakasu.bakasu.ui.component.settings.SettingsDropdownRow
import org.bakasu.bakasu.ui.component.settings.SettingsTextFieldRow
import org.bakasu.bakasu.ui.navigation.LocalNavigator
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurEffect
import org.bakasu.bakasu.ui.theme.blurSource
import org.bakasu.bakasu.ui.util.LocalBackgroundBlurProvided
import org.bakasu.bakasu.ui.util.LocalSnackbarHost
import org.bakasu.bakasu.ui.util.adaptiveScaffoldWindowInsets
import org.bakasu.bakasu.ui.viewmodel.RepositorySourcesViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixHorizontalDivider
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun RepositorySource.shortUrl() = url.toHttpUrlOrNull()?.let {
    it.host + it.encodedPath.trimEnd('/')
} ?: url

@Composable
fun AddRepositoryScreen() {
    val viewModel = koinViewModel<RepositorySourcesViewModel>(key = "add-repository")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val customScroll = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val apiTypes = listOf(RepositoryApiType.KERNEL_SU, RepositoryApiType.MMRL)
    val apiTypeLabels = apiTypes.map { stringResource(it.resId) }
    val addForm = rememberCustomDialog { dismiss ->
        val urlField = rememberTextFieldState(initialText = state.url)
        LaunchedEffect(urlField) {
            snapshotFlow { urlField.text.toString() }.collect { value ->
                if (value != state.url) viewModel.setUrl(value)
            }
        }
        val dismissAddForm = {
            if (!state.busy) {
                viewModel.clearError()
                dismiss()
            }
        }
        OverlayDialog(
            show = true,
            onDismissRequest = dismissAddForm,
            title = stringResource(R.string.repo_add_repository),
        ) {
            Column {
                CompositionLocalProvider(LocalBackgroundBlurProvided provides true) {
                    SegmentedColumn(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        item {
                            SettingsDropdownRow(
                                title = stringResource(R.string.repo_api_type),
                                summary = apiTypeLabels[apiTypes.indexOf(state.apiType)],
                                showValue = false,
                                enabled = !state.busy,
                                items = apiTypeLabels,
                                selectedIndex = apiTypes.indexOf(state.apiType),
                                onSelectedIndexChange = { viewModel.setApiType(apiTypes[it]) },
                            )
                        }
                        item {
                            SettingsTextFieldRow(
                                title = stringResource(R.string.repo_server_url),
                                state = urlField,
                                error = state.error?.let { repositoryErrorText(it) }.orEmpty(),
                                enabled = !state.busy,
                                lineLimits = TextFieldLineLimits.SingleLine,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                                onKeyboardAction = {
                                    if (urlField.text.isNotBlank() && !state.busy) viewModel.add(url = urlField.text.toString())
                                },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    val loading = state.addingUrl == state.url
                    Button(
                        onClick = { viewModel.add() },
                        enabled = state.url.isNotBlank() && !state.busy && !loading,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.add), modifier = Modifier.alpha(if (loading) 0f else 1f))
                            if (loading) {
                                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                                    InfiniteProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MiuixTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = dismissAddForm,
                        enabled = !state.busy,
                    )
                }
            }
        }
    }
    LaunchedEffect(state.added?.url) {
        if (state.added != null) {
            viewModel.consumeAdded()
            if (addForm.isShown) {
                addForm.hide()
                viewModel.setUrl("")
            }
        }
    }
    val navigator = LocalNavigator.current
    val theme = koinInject<ThemeConfig>()
    val cards = koinInject<CardConfig>()
    val barColor = if (theme.isEnableBlur) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(cards.cardAlpha)
    }
    val layoutDirection = LocalLayoutDirection.current
    MiuixScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                modifier = Modifier.blurEffect(),
                title = stringResource(R.string.repo_configure),
                color = barColor,
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        snackbarHost = { SwipeableSnackbarHost(hostState = LocalSnackbarHost.current) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().blurSource(),
            state = customScroll,
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "add-repository") {
                if (!addForm.isShown) {
                    state.error?.let { error ->
                        Text(
                            repositoryErrorText(error),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                Button(
                    onClick = {
                        viewModel.clearError()
                        addForm.show()
                    },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.TwoTone.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.repo_add_repository), modifier = Modifier.padding(start = 8.dp))
                }
            }
            items(state.sources, key = { it.url }) { source ->
                RepositorySourceItem(
                    source = source,
                    busy = state.busy,
                    failed = state.failures.containsKey(source.url),
                    onRename = { name, onSaved -> viewModel.rename(source, name, onSaved) },
                    onRemove = { viewModel.remove(source) },
                )
            }
        }
    }
}

@Composable
internal fun RepositorySourceItem(
    source: RepositorySource,
    busy: Boolean,
    failed: Boolean,
    onRename: (String, () -> Unit) -> Unit,
    onRemove: () -> Unit,
) {
    CatalogCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                CatalogCardHeading(source.displayName)
                Text(
                    source.shortUrl(),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!source.isBuiltIn) {
                RepositoryActions(source, busy, onRename, onRemove)
            }
        }
        if (source.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                source.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (failed) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.repo_refresh_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (source.moduleCount != null || source.isBuiltIn) {
            Spacer(Modifier.height(8.dp))
            MiuixHorizontalDivider(thickness = Dp.Hairline)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    source.moduleCount?.let { count ->
                        Text(
                            pluralStringResource(R.plurals.repo_module_count, count, count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (source.isBuiltIn) {
                    Text(
                        stringResource(R.string.repo_built_in),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryActions(
    source: RepositorySource,
    busy: Boolean,
    onRename: (String, () -> Unit) -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val confirm = rememberConfirmDialog()
    val scope = rememberCoroutineScope()
    val removeTitle = stringResource(R.string.repo_remove)
    val removeMessage = stringResource(R.string.repo_remove_message, source.displayName) + "\n\n" + source.url
    val removeAction = stringResource(R.string.repo_remove_action)
    val cancel = stringResource(R.string.cancel)
    var name by rememberSaveable(source.url) { mutableStateOf(source.displayName) }
    val edit = rememberCustomDialog { dismiss ->
        OverlayDialog(
            show = true,
            onDismissRequest = { if (!busy) dismiss() },
            title = stringResource(R.string.repo_edit_name),
        ) {
            Column {
                MiuixTextField(
                    value = name,
                    onValueChange = { name = it.take(100) },
                    label = stringResource(R.string.repo_display_name),
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.repo_display_name_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(
                        text = stringResource(R.string.repo_save_name),
                        onClick = { onRename(name, dismiss) },
                        enabled = !busy,
                    )
                    TextButton(
                        text = cancel,
                        onClick = dismiss,
                        enabled = !busy,
                    )
                }
            }
        }
    }
    MiuixIconButton(onClick = { expanded = true }, enabled = !busy, modifier = Modifier.size(48.dp)) {
        Icon(Icons.TwoTone.MoreVert, contentDescription = stringResource(R.string.repo_actions))
        OverlayListPopup(show = expanded, onDismissRequest = { expanded = false }) {
            ListPopupColumn {
                BasicComponent(
                    title = stringResource(R.string.repo_edit_name),
                    startAction = {
                        Icon(Icons.TwoTone.Edit, contentDescription = null, modifier = Modifier.size(24.dp))
                    },
                    onClick = {
                        expanded = false
                        name = source.displayName
                        edit.show()
                    },
                )
                BasicComponent(
                    title = removeAction,
                    titleColor = BasicComponentDefaults.titleColor(color = MaterialTheme.colorScheme.error),
                    startAction = {
                        Icon(
                            Icons.TwoTone.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = {
                        expanded = false
                        scope.launch {
                            if (confirm.isShown) return@launch
                            val result = confirm.awaitConfirm(removeTitle, removeMessage, confirm = removeAction, dismiss = cancel)
                            if (result == ConfirmResult.Confirmed) onRemove()
                        }
                    },
                )
            }
        }
    }
}
