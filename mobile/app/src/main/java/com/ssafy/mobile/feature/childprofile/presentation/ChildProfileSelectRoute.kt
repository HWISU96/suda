package com.ssafy.mobile.feature.childprofile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ssafy.mobile.core.ui.components.AppBadge
import com.ssafy.mobile.core.ui.components.AppBadgeTone
import com.ssafy.mobile.core.ui.components.AppCard
import com.ssafy.mobile.core.ui.components.AppLoadingIndicator
import com.ssafy.mobile.core.ui.components.AppPrimaryButton
import com.ssafy.mobile.core.ui.components.AppSecondaryButton
import com.ssafy.mobile.core.ui.feedback.AppEmptyState
import com.ssafy.mobile.core.ui.feedback.AppErrorText
import com.ssafy.mobile.feature.childprofile.domain.model.ChildProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProfileSelectRoute(
    navController: NavController,
    onNavigateToHome: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChildProfileSelectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSelecting by viewModel.isSelecting.collectAsStateWithLifecycle()

    // 이전 화면에서 전달된 변경 플래그 확인
    val refreshNeeded by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("child_profile_changed", false)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    LaunchedEffect(refreshNeeded) {
        if (refreshNeeded) {
            viewModel.loadProfiles()
            navController.currentBackStackEntry?.savedStateHandle?.set(
                "child_profile_changed",
                false,
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                ChildProfileSelectNavigationEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    ChildProfileSelectScreen(
        uiState = uiState,
        isSelecting = isSelecting,
        onProfileSelect = viewModel::selectProfile,
        onRetry = viewModel::retry,
        onNavigateToCreate = onNavigateToCreate,
        onNavigateToEdit = onNavigateToEdit,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildProfileSelectScreen(
    uiState: ChildProfileSelectUiState,
    isSelecting: Boolean,
    onProfileSelect: (Long) -> Unit,
    onRetry: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "아이 선택",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "학습을 진행할 아이를 선택해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (uiState) {
                is ChildProfileSelectUiState.Loading -> {
                    LoadingContent()
                }
                is ChildProfileSelectUiState.Success -> {
                    ProfileList(
                        profiles = uiState.profiles,
                        activeChildId = uiState.activeChildId,
                        isSelecting = isSelecting,
                        onProfileSelect = onProfileSelect,
                        onNavigateToCreate = onNavigateToCreate,
                        onNavigateToEdit = onNavigateToEdit,
                    )
                }
                is ChildProfileSelectUiState.Empty -> {
                    EmptyContent(onNavigateToCreate = onNavigateToCreate)
                }
                is ChildProfileSelectUiState.Error -> {
                    ErrorContent(
                        message = uiState.message,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    AppLoadingIndicator(message = "아이 목록을 불러오는 중입니다.")
}

@Composable
private fun ProfileList(
    profiles: List<ChildProfile>,
    activeChildId: Long?,
    isSelecting: Boolean,
    onProfileSelect: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(profiles) { profile ->
            ProfileItem(
                profile = profile,
                isSelected = profile.childId == activeChildId,
                enabled = !isSelecting,
                onClick = { onProfileSelect(profile.childId) },
                onEditClick = { onNavigateToEdit(profile.childId) },
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            AppPrimaryButton(
                text = "아이 추가하기",
                onClick = onNavigateToCreate,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSelecting,
            )
        }
    }
}

@Composable
private fun ProfileItem(
    profile: ChildProfile,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    AppCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                ),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = profile.name.take(1),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (profile.age != null) {
                    Text(
                        text = "${profile.age}세",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isSelected) {
                AppBadge(
                    text = "현재 선택됨",
                    tone = AppBadgeTone.Primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            TextButton(
                onClick = onEditClick,
                enabled = enabled,
            ) {
                Text(
                    text = "수정",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun EmptyContent(onNavigateToCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppEmptyState(
            message = "등록된 아이가 없습니다.",
            modifier = Modifier.weight(1f),
        )
        AppPrimaryButton(
            text = "아이 프로필 만들기",
            onClick = onNavigateToCreate,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppErrorText(
            message = message,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AppSecondaryButton(
            text = "다시 시도",
            onClick = onRetry,
        )
    }
}

private const val DISABLED_ALPHA = 0.5f
