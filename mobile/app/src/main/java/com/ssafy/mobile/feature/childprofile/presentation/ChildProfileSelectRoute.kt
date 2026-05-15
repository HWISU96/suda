package com.ssafy.mobile.feature.childprofile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ssafy.mobile.core.ui.components.AppLoadingIndicator
import com.ssafy.mobile.core.ui.components.AppSecondaryButton
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
                    .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "누가 사용할까요?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "학습과 리포트에 사용할 아이 프로필을 선택해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(28.dp))

            when (uiState) {
                is ChildProfileSelectUiState.Loading -> {
                    LoadingContent(modifier = Modifier.weight(1f))
                }
                is ChildProfileSelectUiState.Success -> {
                    ProfileGrid(
                        profiles = uiState.profiles,
                        activeChildId = uiState.activeChildId,
                        isSelecting = isSelecting,
                        onProfileSelect = onProfileSelect,
                        onNavigateToCreate = onNavigateToCreate,
                        onNavigateToEdit = onNavigateToEdit,
                        modifier = Modifier.weight(1f),
                    )
                }
                is ChildProfileSelectUiState.Empty -> {
                    EmptyContent(
                        onNavigateToCreate = onNavigateToCreate,
                        modifier = Modifier.weight(1f),
                    )
                }
                is ChildProfileSelectUiState.Error -> {
                    ErrorContent(
                        message = uiState.message,
                        onRetry = onRetry,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AppLoadingIndicator(message = "아이 목록을 불러오는 중입니다.")
    }
}

@Composable
private fun ProfileGrid(
    profiles: List<ChildProfile>,
    activeChildId: Long?,
    isSelecting: Boolean,
    onProfileSelect: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(PROFILE_GRID_COLUMNS),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        itemsIndexed(
            items = profiles,
            key = { _, profile -> profile.childId },
        ) { index, profile ->
            ProfileTile(
                profile = profile,
                paletteIndex = index,
                isSelected = profile.childId == activeChildId,
                enabled = !isSelecting,
                onClick = { onProfileSelect(profile.childId) },
                onEditClick = { onNavigateToEdit(profile.childId) },
            )
        }

        item(key = "add_child_profile") {
            AddProfileTile(
                enabled = !isSelecting,
                onClick = onNavigateToCreate,
            )
        }
    }
}

@Composable
private fun ProfileTile(
    profile: ChildProfile,
    paletteIndex: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = profileAvatarColors(paletteIndex)

    Column(
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable(
                        enabled = enabled,
                        onClick = onClick,
                    ),
            shape = RoundedCornerShape(PROFILE_TILE_RADIUS_DP.dp),
            color = colors.container,
            contentColor = colors.content,
            tonalElevation = 2.dp,
            border =
                if (isSelected) {
                    BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile.name.firstOrNull()?.toString() ?: "?",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.content,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (profile.age != null) {
            Text(
                text = "${profile.age}세",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (isSelected) {
            SelectedProfilePill()
        } else {
            TextButton(
                onClick = onEditClick,
                enabled = enabled,
            ) {
                Text(text = "수정")
            }
        }
    }
}

@Composable
private fun AddProfileTile(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable(
                        enabled = enabled,
                        onClick = onClick,
                    ),
            shape = RoundedCornerShape(PROFILE_TILE_RADIUS_DP.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "아이 추가",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "새 프로필 만들기",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelectedProfilePill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(top = 8.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = "현재 선택됨",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun EmptyContent(
    onNavigateToCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "아직 등록된 아이가 없어요.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        AddProfileTile(
            enabled = true,
            onClick = onNavigateToCreate,
            modifier = Modifier.fillMaxWidth(EMPTY_PROFILE_TILE_WIDTH_FRACTION),
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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

@Composable
private fun profileAvatarColors(index: Int): ProfileAvatarColors {
    val colorScheme = MaterialTheme.colorScheme
    return when (index % PROFILE_AVATAR_PALETTE_SIZE) {
        PRIMARY_PROFILE_PALETTE_INDEX ->
            ProfileAvatarColors(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
        SECONDARY_PROFILE_PALETTE_INDEX ->
            ProfileAvatarColors(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer)
        TERTIARY_PROFILE_PALETTE_INDEX ->
            ProfileAvatarColors(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer)
        else -> ProfileAvatarColors(colorScheme.errorContainer, colorScheme.onErrorContainer)
    }
}

private data class ProfileAvatarColors(
    val container: Color,
    val content: Color,
)

private const val DISABLED_ALPHA = 0.5f
private const val PROFILE_GRID_COLUMNS = 2
private const val PROFILE_TILE_RADIUS_DP = 28
private const val PROFILE_AVATAR_PALETTE_SIZE = 4
private const val EMPTY_PROFILE_TILE_WIDTH_FRACTION = 0.56f
private const val PRIMARY_PROFILE_PALETTE_INDEX = 0
private const val SECONDARY_PROFILE_PALETTE_INDEX = 1
private const val TERTIARY_PROFILE_PALETTE_INDEX = 2
