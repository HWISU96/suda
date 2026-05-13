package com.ssafy.mobile.feature.mypage.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.mobile.core.ui.components.AppBadge
import com.ssafy.mobile.core.ui.components.AppBadgeTone
import com.ssafy.mobile.core.ui.components.AppCard
import com.ssafy.mobile.core.ui.components.AppDialog

@Composable
fun MyPageRoute(
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPageViewModel = hiltViewModel(),
) {
    val logoutState by viewModel.logoutState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(logoutState) {
        when (logoutState) {
            is MyPageLogoutState.Success -> {
                onLogoutSuccess()
            }
            is MyPageLogoutState.Error -> {
                snackbarHostState.showSnackbar((logoutState as MyPageLogoutState.Error).message)
                viewModel.resetLogoutState()
            }
            else -> Unit
        }
    }

    MyPageScreen(
        snackbarHostState = snackbarHostState,
        onLogoutClick = { showLogoutDialog = true },
        modifier = modifier,
    )

    if (showLogoutDialog) {
        AppDialog(
            title = "로그아웃할까요?",
            message = "현재 기기의 로그인 정보와 선택된 아이 정보가 초기화됩니다.",
            confirmText = "로그아웃",
            dismissText = "취소",
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyPageScreen(
    snackbarHostState: SnackbarHostState,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "마이페이지",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "보호자 계정과 아이 프로필 설정이 이곳에 표시됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MyPageMenuItem(
                title = "아이 프로필",
                description = "프로필 선택과 전환",
                onClick = {},
                enabled = false,
                badgeText = "준비 중",
            )
            MyPageMenuItem(
                title = "앱 설정",
                description = "권한, 알림, 학습 환경",
                onClick = {},
                enabled = false,
                badgeText = "준비 중",
            )
            MyPageMenuItem(
                title = "로그아웃",
                description = "로컬 세션 초기화",
                onClick = onLogoutClick,
                badgeText = "위험",
                badgeTone = AppBadgeTone.Error,
                destructive = true,
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.5f

@Composable
@Suppress("LongParameterList")
private fun MyPageMenuItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badgeText: String? = null,
    badgeTone: AppBadgeTone = AppBadgeTone.Neutral,
    destructive: Boolean = false,
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    val titleColor =
        if (destructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    AppCard(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(alpha),
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (badgeText != null) {
                AppBadge(
                    text = badgeText,
                    tone = badgeTone,
                )
            }

            if (enabled) {
                Text(
                    text = ">",
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (destructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
