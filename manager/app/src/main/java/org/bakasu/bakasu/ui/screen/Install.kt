package org.bakasu.bakasu.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AutoFixHigh
import androidx.compose.material.icons.twotone.ExpandMore
import androidx.compose.material.icons.twotone.FileOpen
import androidx.compose.material.icons.twotone.FileUpload
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.model.LkmSelection
import org.bakasu.bakasu.ui.component.DialogHandle
import org.bakasu.bakasu.ui.component.HorizontalPagerWithInteraction
import org.bakasu.bakasu.ui.component.rememberConfirmDialog
import org.bakasu.bakasu.ui.component.rememberCustomDialog
import org.bakasu.bakasu.ui.component.settings.AppBackButton
import org.bakasu.bakasu.ui.component.settings.SegmentedColumn
import org.bakasu.bakasu.ui.component.settings.SettingsChooseDialog
import org.bakasu.bakasu.ui.component.settings.SettingsChooseRow
import org.bakasu.bakasu.ui.component.settings.SettingsValueRow
import org.bakasu.bakasu.ui.navigation.LocalNavigator
import org.bakasu.bakasu.ui.navigation.Route
import org.bakasu.bakasu.ui.rememberMaterial3BlurBackdrop
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurEffect
import org.bakasu.bakasu.ui.theme.blurSource
import org.bakasu.bakasu.ui.util.LocalBlurState
import org.bakasu.bakasu.ui.util.adaptiveScaffoldWindowInsets
import org.bakasu.bakasu.ui.viewmodel.InstallUiEvent
import org.bakasu.bakasu.ui.viewmodel.InstallViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallScreen(
    preselectedKernelUri: String? = null,
) {
    val viewModel = koinViewModel<InstallViewModel>()
    val installState by viewModel.state.collectAsStateWithLifecycle()
    val environment = installState.environment
    val themeConfig: ThemeConfig = koinInject()
    val context = LocalContext.current

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val isGKI = environment.isGki

    LaunchedEffect(installState.loading, isGKI, preselectedKernelUri) {
        if (installState.loading) return@LaunchedEffect

        when {
            preselectedKernelUri != null -> pagerState.scrollToPage(1)
            !isGKI -> pagerState.scrollToPage(0)
        }
    }

    val failedReboot = stringResource(R.string.failed_reboot)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is InstallUiEvent.Error -> {
                    val message = event.message.ifBlank { failedReboot }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(scrollBehavior) {
        snapshotFlow { scrollBehavior.state.heightOffsetLimit }
            .first { it < 0f }
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        topBar = {
            TopBar(
                onBack = { navigator.pop() },
                scrollBehavior = scrollBehavior,
                selectedTab = pagerState.currentPage,
                onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } },
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        HorizontalPagerWithInteraction(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .blurSource(),
        ) { page ->
            CompositionLocalProvider(
                LocalBlurState provides rememberMaterial3BlurBackdrop(
                    enableBlur = themeConfig.isEnableBlur,
                    pagerState = pagerState,
                    pagerPage = page,
                ),
            ) {
                if (installState.loading) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .blurSource(),
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingIndicator()
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
                        }
                    }
                } else {
                    when (page) {
                        0 -> LKMInstallPage(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                                .blurSource(),
                            topPadding = innerPadding.calculateTopPadding(),
                            bottomPadding = innerPadding.calculateBottomPadding(),
                            isGKI = environment.isGki,
                            rootAvailable = environment.rootAvailable,
                            isAbDevice = environment.isAbDevice,
                            currentKmi = environment.currentKmi,
                            supportedKmis = environment.supportedKmis,
                            activeSlotSuffix = environment.activeSlotSuffix,
                            inactiveSlotSuffix = environment.inactiveSlotSuffix,
                            availablePartitions = environment.availablePartitions,
                            defaultPartition = environment.defaultPartition,
                        )

                        1 -> Anykernel3InstallPage(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                                .blurSource(),
                            topPadding = innerPadding.calculateTopPadding(),
                            bottomPadding = innerPadding.calculateBottomPadding(),
                            rootAvailable = environment.rootAvailable,
                            isAbDevice = environment.isAbDevice,
                            activeSlotSuffix = environment.activeSlotSuffix,
                            preselectedKernelUri = preselectedKernelUri,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LKMInstallPage(
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    isGKI: Boolean,
    rootAvailable: Boolean,
    isAbDevice: Boolean,
    currentKmi: String,
    supportedKmis: List<String>,
    activeSlotSuffix: String,
    inactiveSlotSuffix: String,
    availablePartitions: List<String>,
    defaultPartition: String,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val selectFileTip = stringResource(
        id = R.string.select_file_tip,
        defaultPartition,
    )
    val installOnlySupportKoFile = stringResource(R.string.install_only_support_ko_file)
    val dialogTitle = stringResource(id = android.R.string.dialog_alert_title)
    val dialogContent = stringResource(id = R.string.install_inactive_slot_warning)

    var lkmInstallMethod by remember { mutableStateOf<InstallMethod?>(null) }
    var lkmSelection by remember { mutableStateOf<LkmSelection>(LkmSelection.KmiNone) }
    var lkmFileName by remember { mutableStateOf<String?>(null) }
    var allowShell by remember { mutableStateOf(false) }
    var enableAdb by remember { mutableStateOf(false) }
    var forceBackup by remember { mutableStateOf(false) }
    var partitionSelectionIndex by remember { mutableIntStateOf(0) }
    var hasCustomSelected by remember { mutableStateOf(false) }
    var advancedOptionsShown by remember { mutableStateOf(false) }

    val advRotation by animateFloatAsState(
        targetValue = if (advancedOptionsShown) 180f else 0f,
        label = "AdvRotation",
    )

    val lkmMethods = remember(rootAvailable, isAbDevice, isGKI, selectFileTip) {
        buildList {
            add(InstallMethod.SelectFile(summary = selectFileTip))
            if (isGKI && rootAvailable) {
                add(InstallMethod.DirectInstall)
                if (isAbDevice) {
                    add(InstallMethod.DirectInstallToInactiveSlot)
                }
            }
        }
    }

    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            lkmInstallMethod = InstallMethod.DirectInstallToInactiveSlot
        },
        onDismiss = null,
    )

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                lkmInstallMethod = InstallMethod.SelectFile(
                    uri,
                    summary = selectFileTip,
                )
            }
        }
    }

    val selectLkmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val isKo = isKoFile(context, uri)
                if (isKo) {
                    lkmSelection = LkmSelection.LkmUri(uri.toString())
                    lkmFileName = uri.toString()
                } else {
                    lkmSelection = LkmSelection.KmiNone
                    lkmFileName = null
                    Toast.makeText(
                        context,
                        installOnlySupportKoFile,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    val onMethodClick = { option: InstallMethod ->
        when (option) {
            is InstallMethod.SelectFile -> {
                selectImageLauncher.launch(
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/*"
                        putExtra(
                            Intent.EXTRA_MIME_TYPES,
                            arrayOf("application/octet-stream", "application/zip"),
                        )
                    },
                )
            }

            is InstallMethod.DirectInstall -> {
                lkmInstallMethod = option
            }

            is InstallMethod.DirectInstallToInactiveSlot -> {
                confirmDialog.showConfirm(dialogTitle, dialogContent)
            }

            is InstallMethod.HorizonKernel -> Unit
        }
    }

    val onLkmInstall = {
        lkmInstallMethod?.let { method ->
            when (method) {
                is InstallMethod.SelectFile,
                is InstallMethod.DirectInstall,
                is InstallMethod.DirectInstallToInactiveSlot,
                -> {
                    navigator.push(
                        Route.Flash.boot(
                            bootUri = if (method is InstallMethod.SelectFile) {
                                method.uri?.toString()
                            } else {
                                null
                            },
                            lkmUri = (lkmSelection as? LkmSelection.LkmUri)?.uri,
                            kmi = (lkmSelection as? LkmSelection.KmiString)?.value,
                            ota = method is InstallMethod.DirectInstallToInactiveSlot,
                            partition = availablePartitions.getOrNull(partitionSelectionIndex),
                            allowShell = allowShell,
                            enableAdb = enableAdb,
                            forceBackup = if (method is InstallMethod.SelectFile) forceBackup else false,
                        ),
                    )
                }

                is InstallMethod.HorizonKernel -> Unit
            }
        }
        Unit
    }

    val selectKmiDialog = rememberSelectKmiDialog(supportedKmis) { kmi ->
        kmi?.let {
            lkmSelection = LkmSelection.KmiString(it)
            onLkmInstall()
        }
    }

    val onClickNext = {
        if (isGKI && lkmSelection == LkmSelection.KmiNone && currentKmi.isBlank()) {
            selectKmiDialog.show()
        } else {
            onLkmInstall()
        }
    }

    val isOta = lkmInstallMethod is InstallMethod.DirectInstallToInactiveSlot
    val suffix = if (isOta) inactiveSlotSuffix else activeSlotSuffix
    val displayPartitions = availablePartitions.map { name ->
        if (defaultPartition == name) "$name (default)" else name
    }
    val defaultIndex =
        availablePartitions.indexOf(defaultPartition).takeIf { it >= 0 } ?: 0

    if (!hasCustomSelected && partitionSelectionIndex != defaultIndex) {
        partitionSelectionIndex = defaultIndex
    }

    val canSelectPartition =
        lkmInstallMethod is InstallMethod.DirectInstall ||
            lkmInstallMethod is InstallMethod.DirectInstallToInactiveSlot

    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Spacer(modifier = Modifier.height(topPadding))
        }

        item {
            SegmentedColumn {
                lkmMethods.forEach { method ->
                    val selected = method.javaClass == lkmInstallMethod?.javaClass
                    item(key = method.javaClass) {
                        RadioButtonPreference(
                            title = stringResource(id = method.label),
                            summary = method.summary,
                            selected = selected,
                            onClick = { onMethodClick(method) },
                        )
                    }
                }
            }
        }

        item {
            SegmentedColumn {
                expandableItem(
                    expanded = advancedOptionsShown,
                    topContent = {
                        SettingsValueRow(
                            icon = Icons.TwoTone.Settings,
                            title = stringResource(R.string.advanced_options),
                            onClick = {
                                advancedOptionsShown = !advancedOptionsShown
                            },
                            endContent = {
                                MiuixIcon(
                                    imageVector = Icons.TwoTone.ExpandMore,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onBackground,
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = advRotation
                                    },
                                )
                            },
                        )
                    },
                    bottomContent = {
                        item(visible = canSelectPartition && displayPartitions.isNotEmpty()) {
                            SettingsChooseRow(
                                icon = Icons.TwoTone.AutoFixHigh,
                                items = displayPartitions,
                                selectedIndex = partitionSelectionIndex,
                                title = "${stringResource(R.string.install_select_partition)} ($suffix)",
                                onSelectedIndexChange = {
                                    hasCustomSelected = true
                                    partitionSelectionIndex = it
                                },
                            )
                        }

                        item {
                            val hasLkmUri = lkmSelection is LkmSelection.LkmUri

                            SettingsValueRow(
                                icon = Icons.TwoTone.FileOpen,
                                title = stringResource(id = R.string.install_upload_lkm_file),
                                summary = stringResource(id = R.string.install_upload_lkm_file_summary),
                                onClick = {
                                    if (hasLkmUri) {
                                        lkmSelection = LkmSelection.KmiNone
                                        lkmFileName = null
                                    } else {
                                        selectLkmLauncher.launch(
                                            Intent(Intent.ACTION_GET_CONTENT).apply {
                                                type = "application/octet-stream"
                                            },
                                        )
                                    }
                                },
                                bottomContent = if (hasLkmUri) {
                                    {
                                        Text(
                                            text = lkmFileName ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                } else {
                                    null
                                },
                            )
                        }

                        item {
                            CheckboxPreference(
                                title = stringResource(id = R.string.allow_shell),
                                summary = stringResource(id = R.string.allow_shell_summary),
                                checked = allowShell,
                                onCheckedChange = { allowShell = it },
                            )
                        }

                        item {
                            CheckboxPreference(
                                title = stringResource(id = R.string.enable_adb),
                                summary = stringResource(id = R.string.enable_adb_summary),
                                checked = enableAdb,
                                onCheckedChange = { enableAdb = it },
                            )
                        }

                        item(visible = lkmInstallMethod is InstallMethod.SelectFile) {
                            CheckboxPreference(
                                title = stringResource(id = R.string.install_force_backup),
                                summary = stringResource(id = R.string.install_force_backup_summary),
                                checked = forceBackup,
                                onCheckedChange = { forceBackup = it },
                            )
                        }
                    },
                )
            }
        }

        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = lkmInstallMethod != null,
                onClick = onClickNext,
            ) {
                Text(
                    stringResource(id = R.string.install_next),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun Anykernel3InstallPage(
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    rootAvailable: Boolean,
    isAbDevice: Boolean,
    activeSlotSuffix: String,
    preselectedKernelUri: String?,
) {
    val navigator = LocalNavigator.current
    val summary = stringResource(R.string.horizon_kernel_summary)
    var ak3InstallMethod by remember { mutableStateOf<InstallMethod?>(null) }
    var skipKsud by remember { mutableStateOf(false) }
    var showSlotSelectionDialog by remember { mutableStateOf(false) }
    var advancedOptionsShown by remember { mutableStateOf(false) }

    val ak3AdvRotation by animateFloatAsState(
        targetValue = if (advancedOptionsShown) 180f else 0f,
        label = "Ak3AdvRotation",
    )

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                ak3InstallMethod = InstallMethod.HorizonKernel(uri = uri, summary = summary)
            }
        }
    }

    LaunchedEffect(preselectedKernelUri, isAbDevice) {
        preselectedKernelUri?.let { uriString ->
            try {
                val preselectedUri = uriString.toUri()
                ak3InstallMethod = InstallMethod.HorizonKernel(
                    uri = preselectedUri,
                    summary = summary,
                )
            } catch (_: Exception) {
            }
        }
    }

    val onClickNext = {
        (ak3InstallMethod as? InstallMethod.HorizonKernel)?.let { method ->
            method.uri?.let { uri ->
                if (isAbDevice) {
                    showSlotSelectionDialog = true
                } else {
                    navigator.push(
                        Route.KernelFlash(
                            kernelUri = uri.toString(),
                            selectedSlot = null,
                            skipKsud = skipKsud,
                        ),
                    )
                }
            }
        }
    }

    SettingsChooseDialog(
        show = showSlotSelectionDialog && isAbDevice,
        title = stringResource(R.string.select_slot_title),
        items = listOf(stringResource(R.string.slot_a), stringResource(R.string.slot_b)),
        selectedIndex = when (activeSlotSuffix.removePrefix("_")) {
            "b" -> 1
            else -> 0
        },
        onDismiss = { showSlotSelectionDialog = false },
        onSelectedIndexChange = { index ->
            val method = ak3InstallMethod as? InstallMethod.HorizonKernel
            method?.uri?.let { uri ->
                navigator.push(
                    Route.KernelFlash(
                        kernelUri = uri.toString(),
                        selectedSlot = if (index == 1) "b" else "a",
                        skipKsud = skipKsud,
                    ),
                )
            }
        },
    )

    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Spacer(modifier = Modifier.height(topPadding))
        }

        if (rootAvailable) {
            val horizonSelected = ak3InstallMethod is InstallMethod.HorizonKernel

            item {
                SegmentedColumn {
                    item {
                        RadioButtonPreference(
                            title = stringResource(R.string.GKI_install_methods),
                            summary = stringResource(R.string.ak3_select_zip),
                            startAction = {
                                MiuixIcon(
                                    imageVector = Icons.TwoTone.FileUpload,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            selected = horizonSelected,
                            onClick = {
                                selectImageLauncher.launch(
                                    Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "application/*"
                                        putExtra(
                                            Intent.EXTRA_MIME_TYPES,
                                            arrayOf("application/octet-stream", "application/zip"),
                                        )
                                    },
                                )
                            },
                        )
                    }
                }
            }

            item {
                SegmentedColumn {
                    expandableItem(
                        expanded = advancedOptionsShown,
                        topContent = {
                            SettingsValueRow(
                                icon = Icons.TwoTone.Settings,
                                title = stringResource(R.string.advanced_options),
                                onClick = {
                                    advancedOptionsShown = !advancedOptionsShown
                                },
                                endContent = {
                                    MiuixIcon(
                                        imageVector = Icons.TwoTone.ExpandMore,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onBackground,
                                        modifier = Modifier.graphicsLayer {
                                            rotationZ = ak3AdvRotation
                                        },
                                    )
                                },
                            )
                        },
                        bottomContent = {
                            item {
                                CheckboxPreference(
                                    title = stringResource(id = R.string.skip_ksud),
                                    summary = stringResource(id = R.string.skip_ksud_summary),
                                    checked = skipKsud,
                                    onCheckedChange = { skipKsud = it },
                                )
                            }
                        },
                    )
                }
            }
        }

        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = ak3InstallMethod != null,
                onClick = {
                    onClickNext()
                },
            ) {
                Text(
                    stringResource(id = R.string.install_next),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSelectKmiDialog(
    supportedKmi: List<String>,
    onSelected: (String?) -> Unit,
): DialogHandle = rememberCustomDialog { dismiss ->
    SettingsChooseDialog(
        show = true,
        title = stringResource(R.string.select_kmi),
        items = supportedKmi,
        selectedIndex = -1,
        onDismiss = dismiss,
        onSelectedIndexChange = { index ->
            onSelected(supportedKmi.getOrNull(index))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Column(modifier = Modifier.blurEffect()) {
        LargeFlexibleTopAppBar(
            title = {
                Text(stringResource(R.string.install))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            navigationIcon = {
                AppBackButton(
                    onClick = onBack,
                )
            },
            windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
            scrollBehavior = scrollBehavior,
        )

        PrimaryTabRow(
            selectedTabIndex = selectedTab.coerceAtMost(1),
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = { Text(stringResource(R.string.Lkm_install_methods)) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = { Text(stringResource(R.string.GKI_install_methods)) },
            )
        }
    }
}

private fun isKoFile(context: Context, uri: Uri): Boolean {
    val seg = uri.lastPathSegment ?: ""
    if (seg.endsWith(".ko", ignoreCase = true)) return true

    return try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && cursor.moveToFirst()) {
                val name = cursor.getString(idx)
                name?.endsWith(".ko", ignoreCase = true) == true
            } else {
                false
            }
        } ?: false
    } catch (_: Throwable) {
        false
    }
}

@Preview
@Composable
private fun SelectInstallPreview() {
    InstallScreen()
}

sealed class InstallMethod {
    data class SelectFile(
        val uri: Uri? = null,
        @param:StringRes override val label: Int = R.string.select_file,
        override val summary: String?,
    ) : InstallMethod()

    data object DirectInstall : InstallMethod() {
        override val label: Int
            get() = R.string.direct_install
    }

    data object DirectInstallToInactiveSlot : InstallMethod() {
        override val label: Int
            get() = R.string.install_inactive_slot
    }

    data class HorizonKernel(
        val uri: Uri? = null,
        val slot: String? = null,
        @param:StringRes override val label: Int = R.string.horizon_kernel,
        override val summary: String? = null,
    ) : InstallMethod()

    abstract val label: Int
    open val summary: String? = null
}
