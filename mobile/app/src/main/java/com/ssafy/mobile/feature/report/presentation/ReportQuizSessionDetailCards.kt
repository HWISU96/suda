package com.ssafy.mobile.feature.report.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.mobile.feature.report.domain.model.ReportQuizAnswer
import com.ssafy.mobile.feature.report.domain.model.ReportQuizSessionDetail
import java.util.Locale

@Composable
internal fun ReportQuizSessionSummaryCard(detail: ReportQuizSessionDetail) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = DETAIL_SUMMARY_ALPHA),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "${detail.categoryName} 퀴즈",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    "${detail.difficulty.toReportDifficultyLabel()} · " +
                        detail.status.toReportSessionStatusLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
            ReportQuizSessionSummaryMetricGrid(detail = detail)
            Spacer(modifier = Modifier.height(14.dp))
            ReportQuizSessionDateSection(detail = detail)
        }
    }
}

@Composable
private fun ReportQuizSessionSummaryMetricGrid(detail: ReportQuizSessionDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportQuizSessionDetailMetric(
                title = "정답",
                value = "${detail.correctCount}/${detail.totalQuestionCount}",
                modifier = Modifier.weight(1f),
            )
            ReportQuizSessionDetailMetric(
                title = "정답률",
                value = String.format(Locale.KOREA, "%.1f%%", detail.accuracyRate),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportQuizSessionDetailMetric(
                title = "평균 별점",
                value = String.format(Locale.KOREA, "%.1f/3", detail.averageStar),
                modifier = Modifier.weight(1f),
            )
            ReportQuizSessionDetailMetric(
                title = "총 별점",
                value = detail.totalStar?.let { "${it}점" } ?: "정보 없음",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReportQuizSessionDateSection(detail: ReportQuizSessionDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "시작 ${detail.startedAt.toReportQuizSessionDateTimeLabel()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "완료 ${detail.endedAt.toReportQuizSessionDateTimeLabel()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ReportQuizAnswerCard(answer: ReportQuizAnswer) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ANSWER_CARD_ALPHA),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${answer.questionNumber}번 문제",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = answer.targetText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = answer.correctnessLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (answer.isCorrect == false) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ReportQuizAnswerMetricRow(answer = answer)
            Spacer(modifier = Modifier.height(12.dp))
            ReportQuizAnswerTextRow(
                title = "인식 결과",
                value = answer.recognizedText.toDisplayText("인식 결과가 없어요."),
            )
            ReportQuizAnswerTextRow(
                title = "피드백",
                value = answer.feedback.toDisplayText("피드백이 없어요."),
            )
            Text(
                text = "답변 ${answer.answeredAt.toReportQuizSessionDateTimeLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportQuizAnswerMetricRow(answer: ReportQuizAnswer) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReportQuizSessionDetailMetric(
            title = "정답 여부",
            value = answer.correctnessLabel(),
            modifier = Modifier.weight(1f),
        )
        ReportQuizSessionDetailMetric(
            title = "별점",
            value = answer.star?.let { "$it/3" } ?: "정보 없음",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReportQuizAnswerTextRow(
    title: String,
    value: String,
) {
    Text(
        text = "$title: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReportQuizSessionDetailMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

private fun ReportQuizAnswer.correctnessLabel(): String =
    when (isCorrect) {
        true -> "정답"
        false -> "오답"
        null -> "미채점"
    }

private fun String?.toDisplayText(fallback: String): String =
    if (isNullOrBlank()) fallback else this

private const val DETAIL_SUMMARY_ALPHA = 0.45f
private const val ANSWER_CARD_ALPHA = 0.45f
