@file:Suppress("LongParameterList", "MagicNumber", "MaxLineLength", "TooManyFunctions")

package com.ssafy.mobile.feature.sign.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.mobile.core.vision.landmark.LandmarkFrameResult
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignDebugRoute(
    modifier: Modifier = Modifier,
    onBackToMain: () -> Unit = {},
    viewModel: SignDebugViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stopAndBack = {
        viewModel.stop()
        onBackToMain()
    }

    BackHandler(onBack = stopAndBack)

    SignDebugScreen(
        uiState = uiState,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onBackToMain = stopAndBack,
        onLandmarkFrame = viewModel::onLandmarkFrame,
        onCameraMetrics = viewModel::onCameraMetrics,
        onCycleResolution = viewModel::cycleResolution,
        onCycleTargetFps = viewModel::cycleTargetFps,
        onCycleAnalysisFrameInterval = viewModel::cycleAnalysisFrameInterval,
        onCycleThreshold = viewModel::cycleThreshold,
        onCycleSmoothing = viewModel::cycleSmoothing,
        modifier = modifier,
    )
}

@Composable
private fun SignDebugScreen(
    uiState: SignDebugUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBackToMain: () -> Unit,
    onLandmarkFrame: (LandmarkFrameResult) -> Unit,
    onCameraMetrics: (CameraPerformanceMetrics) -> Unit,
    onCycleResolution: () -> Unit,
    onCycleTargetFps: () -> Unit,
    onCycleAnalysisFrameInterval: () -> Unit,
    onCycleThreshold: () -> Unit,
    onCycleSmoothing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DebugHeader(
                isRunning = uiState.isRunning,
                isModelReady = uiState.isModelReady,
                onBackToMain = onBackToMain,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF111827), Color(0xFF020617)),
                        ),
                    ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp),
            ) {
                SignRecognitionScreen(
                    isSessionActive = uiState.isRunning,
                    cameraAnalysisSettings = uiState.cameraSettings,
                    showDebugOverlay = true,
                    onLandmarkFrameAvailable = onLandmarkFrame,
                    onCameraMetricsChanged = onCameraMetrics,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RunControls(
                    isRunning = uiState.isRunning,
                    onStart = onStart,
                    onStop = onStop,
                )
                RecognitionStatusCard(uiState)
                PerformanceCard(uiState)
                TuningCard(
                    uiState = uiState,
                    onCycleResolution = onCycleResolution,
                    onCycleTargetFps = onCycleTargetFps,
                    onCycleAnalysisFrameInterval = onCycleAnalysisFrameInterval,
                    onCycleThreshold = onCycleThreshold,
                    onCycleSmoothing = onCycleSmoothing,
                )
                uiState.errorMessage?.let { errorMessage ->
                    ErrorCard(message = errorMessage)
                }
            }
        }
    }
}

@Composable
private fun DebugHeader(
    isRunning: Boolean,
    isModelReady: Boolean,
    onBackToMain: () -> Unit,
) {
    Surface(color = Color(0xFF0F172A)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "수어 디버그",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "실기기 성능 측정",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onBackToMain) {
                    Text("메인으로")
                }
                StatusPill(
                    label =
                        when {
                            !isModelReady -> "모델 로딩 중"
                            isRunning -> "측정 중"
                            else -> "정지"
                        },
                    color =
                        when {
                            !isModelReady -> Color(0xFFF59E0B)
                            isRunning -> Color(0xFF22C55E)
                            else -> Color(0xFF64748B)
                        },
                )
            }
        }
    }
}

@Composable
private fun RunControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onStart,
            enabled = !isRunning,
            modifier = Modifier.weight(1f),
        ) {
            Text("시작")
        }
        OutlinedButton(
            onClick = onStop,
            enabled = isRunning,
            modifier = Modifier.weight(1f),
        ) {
            Text("정지")
        }
    }
}

@Composable
private fun RecognitionStatusCard(uiState: SignDebugUiState) {
    DebugCard(title = "인식 상태") {
        MetricRow("현재 단어", uiState.currentGloss)
        MetricRow("신뢰도", formatPercent(uiState.confidence))
        MetricRow("손 감지", if (uiState.hasHands) "감지됨" else "미감지")
        MetricRow(
            "포즈 / 왼손 / 오른손 / 입",
            "${uiState.poseLandmarkCount} / " +
                "${uiState.leftHandLandmarkCount} / " +
                "${uiState.rightHandLandmarkCount} / " +
                "${uiState.lipLandmarkCount}",
        )
        MetricRow(
            "시퀀스",
            "${uiState.sequenceFrameCount}/30 프레임, " +
                "손 프레임 ${uiState.sequenceHandFrameCount}",
        )
    }
}

@Composable
private fun PerformanceCard(uiState: SignDebugUiState) {
    DebugCard(title = "성능 지표") {
        MetricRow("카메라 FPS", formatDecimal(uiState.cameraFps))
        MetricRow("카메라 프레임", uiState.cameraFrameCount.toString())
        MetricRow("MediaPipe 처리", "${formatDecimal(uiState.mediaPipeMs)} ms")
        MetricRow("TFLite 추론", "${formatDecimal(uiState.tfliteInferenceMs)} ms")
        MetricRow("전체 지연", "${formatDecimal(uiState.pipelineLatencyMs)} ms")
        MetricRow(
            "분석 해상도",
            if (uiState.analysisImageSize.width == 0) {
                "-"
            } else {
                "${uiState.analysisImageSize.width}x${uiState.analysisImageSize.height}"
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TuningCard(
    uiState: SignDebugUiState,
    onCycleResolution: () -> Unit,
    onCycleTargetFps: () -> Unit,
    onCycleAnalysisFrameInterval: () -> Unit,
    onCycleThreshold: () -> Unit,
    onCycleSmoothing: () -> Unit,
) {
    DebugCard(title = "튜닝 설정") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TuningButton(
                label =
                    "해상도 ${uiState.cameraSettings.targetResolution.width}x" +
                        uiState.cameraSettings.targetResolution.height,
                onClick = onCycleResolution,
            )
            TuningButton(
                label = "목표 FPS ${uiState.cameraSettings.targetFps}",
                onClick = onCycleTargetFps,
            )
            TuningButton(
                label = "분석 간격 ${uiState.cameraSettings.analysisFrameInterval}",
                onClick = onCycleAnalysisFrameInterval,
            )
            TuningButton(
                label = "임계값 ${formatPercent(uiState.recognitionConfig.confidenceThreshold)}",
                onClick = onCycleThreshold,
            )
            TuningButton(
                label =
                    "스무딩 ${uiState.recognitionConfig.smoothingRequiredVotes}/" +
                        uiState.recognitionConfig.smoothingWindowSize,
                onClick = onCycleSmoothing,
            )
            TuningButton(
                label = "시퀀스 고정 ${uiState.recognitionConfig.sequenceLength}",
                onClick = {},
                enabled = false,
            )
        }
        Text(
            text =
                "기본값: 640x480, 15 FPS, " +
                    "분석 간격 1, 시퀀스 30, 임계값 0.80",
            modifier = Modifier.padding(top = 10.dp),
            color = Color(0xFFCBD5E1),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TuningButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(label)
    }
}

@Composable
private fun ErrorCard(message: String) {
    DebugCard(title = "오류", containerColor = Color(0xFF7F1D1D)) {
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DebugCard(
    title: String,
    containerColor: Color = Color(0xFF1E293B),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .background(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(100.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun formatPercent(value: Float): String = String.format(Locale.US, "%.1f%%", value * 100f)
