@file:Suppress("LongMethod", "MagicNumber", "TooManyFunctions")

package com.ssafy.mobile.feature.learning.presentation.wordlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.mobile.core.ui.components.AppInlineErrorText
import com.ssafy.mobile.core.ui.components.AppNetworkImage
import com.ssafy.mobile.core.ui.components.ChunkyButton
import com.ssafy.mobile.core.ui.components.ChunkyButtonTone
import com.ssafy.mobile.core.ui.components.FlipCard
import com.ssafy.mobile.core.ui.components.SudaMascot
import com.ssafy.mobile.core.ui.components.SudaMascotImage
import com.ssafy.mobile.feature.learning.domain.model.LearningWord

@Composable
fun LearningWordListRoute(
    onNavigateBack: () -> Unit,
    onStartQuiz: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearningWordListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAudio()
        }
    }

    LearningWordListScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        actions =
            WordLearningActions(
                onPlayAudio = viewModel::playCurrentWordAudio,
                onStopAudio = viewModel::stopAudio,
                onNextClick = viewModel::nextWord,
                onPreviousClick = viewModel::previousWord,
                onStartQuiz = { onStartQuiz(viewModel.categoryId, viewModel.difficulty) },
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
    uiState: LearningWordListUiState,
    onBackClick: () -> Unit,
    actions: WordLearningActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color(0xFFD7FFF1),
                                    Color(0xFFE8FFF8),
                                    Color(0xFFD2F7EC),
                                ),
                        ),
                    ),
        ) {
            WordLearningSparkles()
            Column(modifier = Modifier.fillMaxSize()) {
                TopLearningBar(
                    uiState = uiState,
                    onCloseClick = onBackClick,
                )

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                ) {
                    when (uiState) {
                        is LearningWordListUiState.Loading -> {
                            WordListMessageState(
                                mascot = SudaMascot.Loading,
                                title = "단어 카드를 준비하고 있어요",
                                description = "곧 말놀이를 시작할 수 있어요.",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        is LearningWordListUiState.Success -> {
                            WordLearningCard(
                                state = uiState,
                                actions = actions,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        is LearningWordListUiState.Empty -> {
                            WordListMessageState(
                                mascot = SudaMascot.Empty,
                                title = "준비된 단어가 없어요",
                                description = "다른 주제를 골라볼까요?",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        is LearningWordListUiState.Error -> {
                            WordListErrorState(
                                message = uiState.message,
                                onRetryClick = actions.onRetryClick,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                if (uiState is LearningWordListUiState.Success) {
                    WordNavigationControls(
                        state = uiState,
                        onPreviousClick = actions.onPreviousClick,
                        onNextClick = actions.onNextClick,
                        onStartQuiz = actions.onStartQuiz,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopLearningBar(
    uiState: LearningWordListUiState,
    onCloseClick: () -> Unit,
) {
    val progress =
        when (uiState) {
            is LearningWordListUiState.Success -> {
                if (uiState.words.isEmpty()) {
                    0f
                } else {
                    (uiState.currentIndex + 1).toFloat() / uiState.words.size
                }
            }
            else -> 0f
        }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "wordLearningProgress",
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, top = 22.dp, end = 22.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onCloseClick,
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(15.dp),
            color = Color(0xFFFFA2A2),
            contentColor = Color(0xFFC95A5A),
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "×",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text =
                    if (uiState is LearningWordListUiState.Success) {
                        "${uiState.currentIndex + 1} / ${uiState.words.size}"
                    } else {
                        "0 / 0"
                    },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color(0xFF263238),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.92f)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fraction = animatedProgress)
                            .height(12.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF73CDED)),
                )
            }
        }

        Spacer(modifier = Modifier.width(68.dp))
    }
}

@Composable
private fun WordLearningCard(
    state: LearningWordListUiState.Success,
    actions: WordLearningActions,
    modifier: Modifier = Modifier,
) {
    val word = state.currentWord ?: return
    var isFlipped by remember { mutableStateOf(false) }

    LaunchedEffect(word) {
        isFlipped = false
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedContent(
            targetState = word,
            label = "wordFlashCard",
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 8.dp),
        ) { currentWord ->
            FlipCard(
                isFlipped = isFlipped,
                onFlip = { isFlipped = !isFlipped },
                front = {
                    FlashWordCardFront(
                        word = currentWord,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                back = {
                    FlashWordCardBack(
                        word = currentWord,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        AudioOnlyButton(
            audioState = state.audioState,
            onPlayClick = actions.onPlayAudio,
            onStopClick = actions.onStopAudio,
        )
    }
}

@Composable
private fun FlashWordCardFront(
    word: LearningWord,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.White,
                                    Color(0xFFF8FFFC),
                                ),
                        ),
                    ).padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                AppNetworkImage(
                    imageUrl = word.imageUrl,
                    contentDescription = word.word,
                    fallbackText = word.word,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 230.dp),
                    contentScale = ContentScale.Fit,
                    placeholder = { WordFallback(word = word.word) },
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = word.word,
                style = MaterialTheme.typography.displayMedium,
                color = Color(0xFF242424),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FlashWordCardBack(
    word: LearningWord,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SudaMascotImage(
                mascot = SudaMascot.WordCard,
                contentDescription = null,
                modifier = Modifier.size(164.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = word.word,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AudioOnlyButton(
    audioState: AudioPlaybackState,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    val isPlaying = audioState == AudioPlaybackState.Playing
    val isLoading = audioState == AudioPlaybackState.Loading

    Surface(
        onClick = {
            if (isPlaying) {
                onStopClick()
            } else {
                onPlayClick()
            }
        },
        enabled = !isLoading,
        modifier = Modifier.size(88.dp),
        shape = CircleShape,
        color =
            if (isPlaying) {
                Color(0xFF57BFE4)
            } else {
                Color(0xFF7CD4F0)
            },
        contentColor =
            if (isPlaying) {
                Color.White
            } else {
                Color.White
            },
        shadowElevation = 10.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = if (isPlaying) "■" else "🔊",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun WordNavigationControls(
    state: LearningWordListUiState.Success,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChunkyButton(
            text = "‹ 이전",
            onClick = onPreviousClick,
            enabled = state.hasPrevious,
            tone = ChunkyButtonTone.Secondary,
            modifier = Modifier.weight(1f),
        )
        ChunkyButton(
            text = if (state.hasNext) "다음 ›" else "퀴즈 시작 ›",
            onClick = if (state.hasNext) onNextClick else onStartQuiz,
            tone = if (state.hasNext) ChunkyButtonTone.Warning else ChunkyButtonTone.Success,
            modifier = Modifier.weight(1.45f),
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
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
                            ),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = word.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 72.sp,
                )
            }
        }
    }
}

@Composable
private fun WordLearningSparkles() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "✦",
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 150.dp, end = 42.dp),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "✦",
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 44.dp, bottom = 148.dp),
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun WordListMessageState(
    mascot: SudaMascot,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SudaMascotImage(
            mascot = mascot,
            contentDescription = null,
            modifier = Modifier.size(160.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WordListErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SudaMascotImage(
            mascot = SudaMascot.ErrorNetwork,
            contentDescription = null,
            modifier = Modifier.size(150.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        AppInlineErrorText(text = message)
        Spacer(modifier = Modifier.height(24.dp))
        ChunkyButton(
            text = "다시 시도",
            onClick = onRetryClick,
            tone = ChunkyButtonTone.Primary,
        )
    }
}
