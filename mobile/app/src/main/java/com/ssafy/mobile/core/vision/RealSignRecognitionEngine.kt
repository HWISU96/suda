package com.ssafy.mobile.core.vision

import com.ssafy.mobile.core.model.SignRecognitionEvent
import com.ssafy.mobile.core.vision.feature.LandmarkFeatureEncoder
import com.ssafy.mobile.core.vision.feature.SignSequenceBuffer
import com.ssafy.mobile.core.vision.inference.FakeSignInferenceAdapter
import com.ssafy.mobile.core.vision.inference.SignInferenceAdapter
import com.ssafy.mobile.core.vision.landmark.LandmarkFrameResult
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class RealSignRecognitionEngine(
    private val featureEncoder: LandmarkFeatureEncoder = LandmarkFeatureEncoder(),
    private val sequenceBuffer: SignSequenceBuffer = SignSequenceBuffer(),
    private val inferenceAdapter: SignInferenceAdapter = FakeSignInferenceAdapter(),
) : SignRecognitionEngine {
    @Inject
    constructor() : this(
        featureEncoder = LandmarkFeatureEncoder(),
        sequenceBuffer = SignSequenceBuffer(),
        inferenceAdapter = FakeSignInferenceAdapter(),
    )

    private val isStarted = AtomicBoolean(false)
    private val _events =
        MutableSharedFlow<SignRecognitionEvent>(
            extraBufferCapacity = EVENT_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    override val events: Flow<SignRecognitionEvent> = _events.asSharedFlow()

    override fun start() {
        if (!isStarted.compareAndSet(false, true)) {
            return
        }

        resetSessionState()
        _events.tryEmit(SignRecognitionEvent.Started)
        _events.tryEmit(SignRecognitionEvent.Ready)
    }

    override fun stop() {
        if (!isStarted.compareAndSet(true, false)) {
            return
        }

        resetSessionState()
        _events.tryEmit(SignRecognitionEvent.Stopped)
    }

    fun onLandmarkFrame(frame: LandmarkFrameResult) {
        if (!isStarted.get()) {
            return
        }

        runCatching {
            val feature = featureEncoder.encode(frame)
            sequenceBuffer.add(feature)
            val sequence = sequenceBuffer.buildSequenceInput() ?: return
            val result = inferenceAdapter.predict(sequence)

            _events.tryEmit(
                SignRecognitionEvent.Prediction(
                    gloss = result.gloss,
                    confidence = result.confidence,
                    timestampMs = frame.timestampMs,
                ),
            )
        }.onFailure { throwable ->
            _events.tryEmit(
                SignRecognitionEvent.Error(
                    message = "수어 인식 처리 중 오류가 발생했습니다.",
                    cause = throwable,
                ),
            )
        }
    }

    fun close() {
        stop()
        inferenceAdapter.close()
    }

    private fun resetSessionState() {
        featureEncoder.reset()
        sequenceBuffer.clear()
    }

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 64
    }
}
