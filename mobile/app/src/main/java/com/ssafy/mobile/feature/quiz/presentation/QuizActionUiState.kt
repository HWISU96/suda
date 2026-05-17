@file:Suppress("LongMethod", "MagicNumber", "TooManyFunctions")

package com.ssafy.mobile.feature.quiz.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.mobile.core.ui.components.AppBadge
import com.ssafy.mobile.core.ui.components.AppBadgeTone
import com.ssafy.mobile.core.ui.components.ChunkyButton
import com.ssafy.mobile.core.ui.components.ChunkyButtonTone
import com.ssafy.mobile.feature.quiz.domain.model.QuizAnswer

internal data class QuizActionUiState(
    val recordButton: QuizButtonUiState,
    val nextButton: QuizButtonUiState,
    val recording: QuizRecordingUiState,
    val answerSubmitState: QuizAnswerSubmitState,
)

internal data class QuizButtonUiState(
    val text: String,
    val enabled: Boolean,
)

internal data class QuizRecordingUiState(
    val status: QuizRecordingStatus,
    val answerAttemptCount: Int?,
    val recognizedText: String?,
)

@Suppress("LongParameterList")
internal fun quizActionUiState(
    answer: QuizAnswer?,
    recordingStatus: QuizRecordingStatus,
    answerSubmitState: QuizAnswerSubmitState,
    isRecording: Boolean,
    isProcessing: Boolean,
    isSubmitting: Boolean,
    isCompletionPending: Boolean,
    isSaveFailed: Boolean,
    canSkipQuestion: Boolean,
    hasAnswered: Boolean,
    hasSuccessfulAnswer: Boolean,
    retryLimitReached: Boolean,
    remainingRetryCount: Int,
    canRecordAnswer: Boolean,
    canMoveNext: Boolean,
    isLastQuestion: Boolean,
): QuizActionUiState =
    QuizActionUiState(
        recordButton =
            QuizButtonUiState(
                text =
                    quizRecordButtonText(
                        isRecording = isRecording,
                        isProcessing = isProcessing,
                        isSubmitting = isSubmitting,
                        isCompletionPending = isCompletionPending,
                        isSaveFailed = isSaveFailed,
                        hasAnswered = hasAnswered,
                        remainingRetryCount = remainingRetryCount,
                    ),
                enabled = canRecordAnswer,
            ),
        nextButton =
            QuizButtonUiState(
                text =
                    quizNextActionText(
                        isCompletionPending = isCompletionPending,
                        isSaveFailed = isSaveFailed,
                        canSkipQuestion = canSkipQuestion,
                        hasAnswered = hasAnswered,
                        hasSuccessfulAnswer = hasSuccessfulAnswer,
                        retryLimitReached = retryLimitReached,
                        isLastQuestion = isLastQuestion,
                    ),
                enabled = canMoveNext,
            ),
        recording =
            QuizRecordingUiState(
                status = recordingStatus,
                answerAttemptCount = answer?.attemptCount,
                recognizedText = answer?.sttText,
            ),
        answerSubmitState = answerSubmitState,
    )

@Composable
internal fun QuizActionCard(
    actionState: QuizActionUiState,
    onAnswerClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRecording = actionState.recording.status.isRecordingStatus()
    val isBusy =
        actionState.recording.status == QuizRecordingStatus.Processing ||
            actionState.answerSubmitState == QuizAnswerSubmitState.Submitting
    val canMoveNext = actionState.nextButton.enabled

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (canMoveNext) {
            ChunkyButton(
                text = actionState.nextButton.text,
                onClick = onNextClick,
                enabled = true,
                tone = ChunkyButtonTone.Primary,
                modifier = Modifier.fillMaxWidth(0.62f),
            )
        } else {
            MicCircleButton(
                isRecording = isRecording,
                isBusy = isBusy,
                enabled = actionState.recordButton.enabled || isRecording,
                onClick = onAnswerClick,
            )
            Text(
                text = actionState.toShortGuideMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            AnswerSubmitStatusBadge(state = actionState.answerSubmitState)
        }
    }
}

@Composable
private fun MicCircleButton(
    isRecording: Boolean,
    isBusy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !isBusy,
        modifier = Modifier.size(88.dp),
        shape = CircleShape,
        color =
            when {
                isRecording -> Color(0xFFFF8F8F)
                enabled -> Color(0xFF76CFF0)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        contentColor = Color.White,
        shadowElevation = 10.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = Color.White,
                )
            } else {
                Text(
                    text = if (isRecording) "■" else "🎙",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AnswerSubmitStatusBadge(
    state: QuizAnswerSubmitState,
    modifier: Modifier = Modifier,
) {
    val message = state.toStatusMessage() ?: return

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AppBadge(
            text = message,
            tone = state.toBadgeTone(),
        )
    }
}

private fun QuizActionUiState.toShortGuideMessage(): String =
    when {
        recording.status.isRecordingStatus() -> "다 말했으면 한 번 더 눌러요."
        recording.status == QuizRecordingStatus.Processing -> "목소리를 확인하고 있어요."
        answerSubmitState == QuizAnswerSubmitState.Submitting -> "답변을 저장하고 있어요."
        answerSubmitState is QuizAnswerSubmitState.Error -> answerSubmitState.message
        answerSubmitState is QuizAnswerSubmitState.SaveFailed -> answerSubmitState.message
        recording.answerAttemptCount != null -> "천천히 다시 말해볼까요?"
        else -> "버튼을 누르고 단어를 말해요."
    }

private fun QuizRecordingStatus.isRecordingStatus(): Boolean =
    this == QuizRecordingStatus.Recording ||
        this == QuizRecordingStatus.FallbackRecording

private fun QuizAnswerSubmitState.toStatusMessage(): String? =
    when (this) {
        QuizAnswerSubmitState.Idle -> null
        QuizAnswerSubmitState.Submitting -> "답변을 저장하고 있어요..."
        QuizAnswerSubmitState.Success -> "답변을 저장했어요."
        is QuizAnswerSubmitState.CompletionPending -> message
        is QuizAnswerSubmitState.SaveFailed -> message
        is QuizAnswerSubmitState.Error -> message
    }

private fun QuizAnswerSubmitState.toBadgeTone(): AppBadgeTone =
    when (this) {
        QuizAnswerSubmitState.Success -> AppBadgeTone.Success
        QuizAnswerSubmitState.Submitting -> AppBadgeTone.Primary
        QuizAnswerSubmitState.Idle -> AppBadgeTone.Neutral
        is QuizAnswerSubmitState.CompletionPending,
        is QuizAnswerSubmitState.SaveFailed,
        is QuizAnswerSubmitState.Error,
        -> AppBadgeTone.Error
    }

private fun quizRecordButtonText(
    isRecording: Boolean,
    isProcessing: Boolean,
    isSubmitting: Boolean,
    isCompletionPending: Boolean,
    isSaveFailed: Boolean,
    hasAnswered: Boolean,
    remainingRetryCount: Int,
): String =
    when {
        isRecording -> "다 말했어요"
        isProcessing -> "확인하고 있어요"
        isSubmitting -> "저장하고 있어요"
        isCompletionPending -> "저장 완료"
        isSaveFailed -> quizRetryButtonText(remainingRetryCount)
        hasAnswered && remainingRetryCount == 0 -> "저장 완료"
        hasAnswered -> quizRetryButtonText(remainingRetryCount)
        else -> "말하기 시작"
    }

private fun quizNextActionText(
    isCompletionPending: Boolean,
    isSaveFailed: Boolean,
    canSkipQuestion: Boolean,
    hasAnswered: Boolean,
    hasSuccessfulAnswer: Boolean,
    retryLimitReached: Boolean,
    isLastQuestion: Boolean,
): String =
    when {
        isCompletionPending -> "결과 보기 다시 시도"
        isSaveFailed -> "답변 저장 다시 시도"
        else ->
            quizNextButtonText(
                canSkipQuestion = canSkipQuestion,
                hasAnswered = hasAnswered,
                hasSuccessfulAnswer = hasSuccessfulAnswer,
                retryLimitReached = retryLimitReached,
                isLastQuestion = isLastQuestion,
            )
    }
