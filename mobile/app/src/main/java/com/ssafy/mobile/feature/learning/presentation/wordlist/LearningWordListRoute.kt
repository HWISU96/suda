package com.ssafy.mobile.feature.learning.presentation.wordlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.mobile.core.ui.components.AppErrorText
import com.ssafy.mobile.core.ui.components.AppLoadingIndicator
import com.ssafy.mobile.core.ui.components.AppNetworkImage
import com.ssafy.mobile.core.ui.components.AppPrimaryButton
import com.ssafy.mobile.feature.learning.domain.model.LearningWord

@Composable
fun LearningWordListRoute(
    onNavigateBack: () -> Unit,
    onStartQuiz: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearningWordListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            // 화면 이탈 시 오디오 정지
            viewModel.stopAudio()
        }
    }

    LearningWordListScreen(
        categoryName = viewModel.categoryName,
        uiState = uiState,
        onBackClick = onNavigateBack,
        actions =
            WordLearningActions(
                onPlayAudio = viewModel::playCurrentWordAudio,
                onStopAudio = viewModel::stopAudio,
                onNextClick = viewModel::nextWord,
                onPreviousClick = viewModel::previousWord,
                onStartQuiz = { onStartQuiz(viewModel.categoryId) },
                onRetryClick = viewModel::loadWords,
            ),
        modifier = modifier,
    )
}

internal data class WordLearningActions(
    val onPlayAudio: () -> Unit,
    val onStopAudio: () -> Unit,
    val onNextClick: () -> Unit,
    val onPreviousClick: () -> Unit,
    val onStartQuiz: () -> Unit,
    val onRetryClick: () -> Unit,
)

@Composable
internal fun LearningWordListScreen(
    categoryName: String?,
    uiState: LearningWordListUiState,
    onBackClick: () -> Unit,
    actions: WordLearningActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WordListHeader(
                categoryName = categoryName ?: "단어 학습",
                onBackClick = onBackClick,
            )

            Box(modifier = Modifier.weight(1f)) {
                when (uiState) {
                    is LearningWordListUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppLoadingIndicator()
                        }
                    }

                    is LearningWordListUiState.Success -> {
                        WordLearningCard(
                            state = uiState,
                            actions = actions,
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                        )
                    }

                    is LearningWordListUiState.Empty -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "준비된 단어가 없습니다.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    is LearningWordListUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            AppErrorText(text = uiState.message)
                            Spacer(modifier = Modifier.height(16.dp))
                            AppPrimaryButton(
                                text = "다시 시도",
                                onClick = actions.onRetryClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordListHeader(
    categoryName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 24.dp),
    ) {
        IconButton(onClick = onBackClick) {
            Text(
                text = "뒤로",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = categoryName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = "단어 카드를 넘기며 소리를 들어보세요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        )
    }
}

@Composable
private fun WordLearningCard(
    state: LearningWordListUiState.Success,
    actions: WordLearningActions,
    modifier: Modifier = Modifier,
) {
    val word = state.currentWord ?: return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            WordCardContent(
                word = word,
                audioState = state.audioState,
                onPlayAudio = actions.onPlayAudio,
                onStopAudio = actions.onStopAudio,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        WordNavigationControls(
            state = state,
            onPreviousClick = actions.onPreviousClick,
            onNextClick = actions.onNextClick,
            onStartQuiz = actions.onStartQuiz,
        )
    }
}

@Composable
private fun WordCardContent(
    word: LearningWord,
    audioState: AudioPlaybackState,
    onPlayAudio: () -> Unit,
    onStopAudio: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppNetworkImage(
            imageUrl = word.imageUrl,
            contentDescription = word.word,
            fallbackText = word.word,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
            placeholder = { WordFallback(word = word.word) },
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = word.word,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (!word.displayText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = word.displayText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AudioControlButton(
            audioState = audioState,
            onPlayClick = onPlayAudio,
            onStopClick = onStopAudio,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WordNavigationControls(
    state: LearningWordListUiState.Success,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onStartQuiz: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onPreviousClick,
            enabled = state.hasPrevious,
        ) {
            Text("◀", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("이전 단어")
        }

        Text(
            text = "${state.currentIndex + 1} / ${state.words.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.hasNext) {
            Button(
                onClick = onNextClick,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("다음")
                Spacer(modifier = Modifier.width(8.dp))
                Text("▶", fontSize = 18.sp)
            }
        } else {
            AppPrimaryButton(
                text = "퀴즈 풀기",
                onClick = onStartQuiz,
                modifier = Modifier.width(140.dp),
            )
        }
    }
}

@Composable
private fun AudioControlButton(
    audioState: AudioPlaybackState,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        when (audioState) {
            AudioPlaybackState.Playing -> MaterialTheme.colorScheme.primary
            AudioPlaybackState.Error -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        }

    val contentColor =
        when (audioState) {
            AudioPlaybackState.Playing -> MaterialTheme.colorScheme.onPrimary
            AudioPlaybackState.Error -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = {
                if (audioState == AudioPlaybackState.Playing) {
                    onStopClick()
                } else {
                    onPlayClick()
                }
            },
            enabled = audioState != AudioPlaybackState.Loading,
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(containerColor),
        ) {
            AudioControlIcon(audioState = audioState, contentColor = contentColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        val labelColor =
            if (audioState == AudioPlaybackState.Error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }

        Text(
            text =
                when (audioState) {
                    AudioPlaybackState.Loading -> "준비 중..."
                    AudioPlaybackState.Playing -> "재생 중"
                    AudioPlaybackState.Error -> "재생 실패"
                    else -> "소리 듣기"
                },
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AudioControlIcon(
    audioState: AudioPlaybackState,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    if (audioState == AudioPlaybackState.Loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = contentColor,
            strokeWidth = 3.dp,
        )
    } else {
        Text(
            text = if (audioState == AudioPlaybackState.Playing) "■" else "▶",
            fontSize = 32.sp,
            color = contentColor,
        )
    }
}

@Composable
private fun WordFallback(
    word: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer.copy(
                                    alpha = 0.5f,
                                ),
                            ),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word.firstOrNull()?.toString() ?: "",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Black,
            fontSize = 120.sp,
        )
    }
}
