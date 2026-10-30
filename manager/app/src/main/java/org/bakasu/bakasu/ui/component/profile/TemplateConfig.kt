package org.bakasu.bakasu.ui.component.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Article
import androidx.compose.material.icons.automirrored.twotone.ReadMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.model.AppProfile
import org.bakasu.bakasu.ui.component.NetworkRefreshContent
import org.bakasu.bakasu.ui.component.settings.SettingsChooseRow
import org.bakasu.bakasu.ui.util.ActivityResumeEffect
import org.bakasu.bakasu.ui.viewmodel.TemplateUiAction
import org.bakasu.bakasu.ui.viewmodel.TemplateViewModel
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * @author weishu
 * @date 2023/10/21.
 */
@Composable
fun TemplateConfig(
    profile: AppProfile,
    onViewTemplate: (id: String) -> Unit = {},
    onProfileChange: (AppProfile) -> Unit,
) {
    val viewModel = koinViewModel<TemplateViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    ActivityResumeEffect(viewModel) {
        viewModel.dispatch(TemplateUiAction.Refresh())
    }

    var template by rememberSaveable(profile.rootTemplate) {
        mutableStateOf(profile.rootTemplate ?: "")
    }
    val profileTemplates = listOf("None") + uiState.profileTemplates
    val profileTemplateNames = listOf("None") + uiState.profileTemplateNames
    val currentIndex = profileTemplates.indexOf(template).let { if (it == -1) 0 else it }

    SettingsChooseRow(
        icon = Icons.AutoMirrored.TwoTone.Article,
        title = stringResource(R.string.profile_template),
        items = profileTemplateNames,
        selectedIndex = currentIndex,
        dialogEmptyContent = if (profileTemplates.size == 1) {
            {
                NetworkRefreshContent(
                    offline = uiState.isOffline,
                    onRetry = {
                        scope.launch {
                            viewModel.dispatch(TemplateUiAction.Refresh(synchronize = true))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                )
            }
        } else {
            null
        },
        itemEndActions = { index ->
            if (index != 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.TwoTone.ReadMore,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(35.dp)
                        .clip(CircleShape)
                        .clickable {
                            onViewTemplate(profileTemplates[index])
                        }
                        .padding(5.dp),
                )
            }
        },
        onSelectedIndexChange = { index ->
            if (index == 0) {
                template = ""
            } else {
                template = profileTemplates[index]

                uiState.templateList
                    .firstOrNull { it.id == template }
                    ?.let { templateInfo ->
                        onProfileChange(
                            profile.copy(
                                rootTemplate = template,
                                rootUseDefault = false,
                                uid = templateInfo.uid,
                                gid = templateInfo.gid,
                                groups = templateInfo.groups,
                                capabilities = templateInfo.capabilities,
                                context = templateInfo.context,
                                rules = templateInfo.rules.joinToString("\n"),
                                namespace = templateInfo.namespace,
                            ),
                        )
                    }
            }
        },
    )
}
