@file:Suppress("MagicNumber")

package com.ssafy.mobile.feature.quiz.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.mobile.R
import com.ssafy.mobile.core.ui.components.AppCard
import com.ssafy.mobile.feature.quiz.domain.model.QuizAnswer

@Composable
internal fun QuizStarResultCard(
    answer: QuizAnswer,
    remainingRetryCount: Int,
    modifier: Modifier = Modifier,
) {
    QuizFeedbackEffects(
        eventKey = answer.feedbackEventKey(),
        cue = answer.toFeedbackCue(),
    )

    AppCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        val eventKey = answer.feedbackEventKey()

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StarRewardBurst(
                eventKey = eventKey,
                isCorrect = answer.isCorrect == true,
            )
            Text(
                text = answer.toRewardTitle(remainingRetryCount),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StarRewardBurst(
    eventKey: String?,
    isCorrect: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale = remember(eventKey) { Animatable(0.7f) }
    val rotation = remember(eventKey) { Animatable(-10f) }
    val infiniteTransition = rememberInfiniteTransition(label = "starRewardTwinkle")
    val sparkleScale =
        infiniteTransition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.12f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "sparkleScale",
        )
    val sparkleAlpha =
        infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "sparkleAlpha",
        )

    LaunchedEffect(eventKey) {
        if (eventKey == null) return@LaunchedEffect

        scale.snapTo(0.7f)
        rotation.snapTo(-10f)
        scale.animateTo(
            targetValue = 1.18f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        )
        rotation.animateTo(
            targetValue = 4f,
            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
        )
        rotation.animateTo(
            targetValue = 0f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
        )
    }

    StarRewardBurstContent(
        isCorrect = isCorrect,
        starScale = scale.value,
        starRotation = rotation.value,
        sparkleScale = sparkleScale.value,
        sparkleAlpha = sparkleAlpha.value,
        modifier = modifier,
    )
}

@Composable
private fun StarRewardBurstContent(
    isCorrect: Boolean,
    starScale: Float,
    starRotation: Float,
    sparkleScale: Float,
    sparkleAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .size(184.dp),
        contentAlignment = Alignment.Center,
    ) {
        RewardSparkle(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 30.dp, y = 12.dp),
            size = 44,
            scale = sparkleScale,
            alpha = sparkleAlpha,
        )
        RewardSparkle(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-24).dp, y = (-8).dp),
            size = 38,
            scale = sparkleScale * 0.92f,
            alpha = sparkleAlpha,
        )
        Image(
            painter =
                painterResource(
                    if (isCorrect) {
                        R.drawable.star_reward_glow
                    } else {
                        R.drawable.star_reward_full
                    },
                ),
            contentDescription = null,
            modifier =
                Modifier
                    .size(164.dp)
                    .graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                        rotationZ = starRotation
                    },
        )
    }
}

@Composable
private fun RewardSparkle(
    size: Int,
    scale: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.star_reward_small),
        contentDescription = null,
        modifier =
            modifier
                .size(size.dp)
                .scale(scale)
                .alpha(alpha),
    )
}

private fun QuizAnswer.toRewardTitle(remainingRetryCount: Int): String =
    when {
        star == null -> "확인하고 있어요"
        isCorrect == true -> "잘했어요!"
        remainingRetryCount > 0 -> "한 번 더 해볼까요?"
        else -> "괜찮아요!"
    }

private fun QuizAnswer.feedbackEventKey(): String? =
    star?.let { "${questionId}_${attemptCount}_$it" }

private fun QuizAnswer.toFeedbackCue(): QuizFeedbackCue? =
    when {
        star == null -> null
        isCorrect == true -> QuizFeedbackCue.Correct
        else -> QuizFeedbackCue.Retry
    }
