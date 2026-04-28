package com.ssafy.mobile.core.vision

import com.ssafy.mobile.core.model.SignRecognitionEvent
import com.ssafy.mobile.core.vision.feature.LandmarkFeatureEncoder
import com.ssafy.mobile.core.vision.feature.SignFeatureSpec
import com.ssafy.mobile.core.vision.feature.SignSequenceBuffer
import com.ssafy.mobile.core.vision.inference.SignInferenceAdapter
import com.ssafy.mobile.core.vision.inference.SignInferenceResult
import com.ssafy.mobile.core.vision.landmark.HandLandmarks
import com.ssafy.mobile.core.vision.landmark.HandSide
import com.ssafy.mobile.core.vision.landmark.LandmarkFrameResult
import com.ssafy.mobile.core.vision.landmark.LandmarkGroup
import com.ssafy.mobile.core.vision.landmark.LandmarkGroupType
import com.ssafy.mobile.core.vision.landmark.LandmarkPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealSignRecognitionEngineTest {
    @Test
    fun emitsStartedAndReadyWhenStarted() =
        runBlocking {
            val engine = createEngine()
            val eventsDeferred =
                async {
                    withTimeout(TIMEOUT_MS) {
                        engine.events.take(2).toList()
                    }
                }

            yield()
            engine.start()

            val events = eventsDeferred.await()
            assertEquals(SignRecognitionEvent.Started, events[0])
            assertEquals(SignRecognitionEvent.Ready, events[1])
        }

    @Test
    fun ignoresDuplicatedStartCalls() =
        runBlocking {
            val engine = createEngine()
            val eventsDeferred =
                async {
                    withTimeout(TIMEOUT_MS) {
                        engine.events.take(2).toList()
                    }
                }

            yield()
            engine.start()
            engine.start()

            val events = eventsDeferred.await()
            assertEquals(listOf(SignRecognitionEvent.Started, SignRecognitionEvent.Ready), events)
        }

    @Test
    fun emitsPredictionWhenSequenceIsReady() =
        runBlocking {
            val engine = createEngine()
            val predictionDeferred =
                async {
                    withTimeout(TIMEOUT_MS) {
                        engine.events.filterIsInstance<SignRecognitionEvent.Prediction>().first()
                    }
                }

            yield()
            engine.start()
            engine.onLandmarkFrame(createFrame(timestampMs = 1L))
            engine.onLandmarkFrame(createFrame(timestampMs = 2L))

            val prediction = predictionDeferred.await()
            assertEquals(TEST_GLOSS, prediction.gloss)
            assertEquals(TEST_CONFIDENCE, prediction.confidence, FLOAT_DELTA)
            assertEquals(2L, prediction.timestampMs)
        }

    @Test
    fun ignoresFramesWhenStopped() =
        runBlocking {
            val inferenceAdapter = RecordingInferenceAdapter()
            val engine = createEngine(inferenceAdapter = inferenceAdapter)

            engine.onLandmarkFrame(createFrame(timestampMs = 1L))

            assertEquals(0, inferenceAdapter.predictCallCount)
        }

    @Test
    fun emitsStoppedAndClearsSessionState() =
        runBlocking {
            val inferenceAdapter = RecordingInferenceAdapter()
            val engine = createEngine(inferenceAdapter = inferenceAdapter)
            val stoppedDeferred =
                async {
                    withTimeout(TIMEOUT_MS) {
                        engine.events.filterIsInstance<SignRecognitionEvent.Stopped>().first()
                    }
                }

            yield()
            engine.start()
            engine.onLandmarkFrame(createFrame(timestampMs = 1L))
            engine.stop()
            engine.start()
            engine.onLandmarkFrame(createFrame(timestampMs = 2L))

            assertEquals(SignRecognitionEvent.Stopped, stoppedDeferred.await())
            assertEquals(0, inferenceAdapter.predictCallCount)
        }

    @Test
    fun emitsErrorWhenInferenceFails() =
        runBlocking {
            val engine =
                createEngine(
                    inferenceAdapter =
                        RecordingInferenceAdapter(
                            shouldThrow = true,
                        ),
                )
            val errorDeferred =
                async {
                    withTimeout(TIMEOUT_MS) {
                        engine.events.filterIsInstance<SignRecognitionEvent.Error>().first()
                    }
                }

            yield()
            engine.start()
            engine.onLandmarkFrame(createFrame(timestampMs = 1L))
            engine.onLandmarkFrame(createFrame(timestampMs = 2L))

            val error = errorDeferred.await()
            assertTrue(error.cause is IllegalStateException)
        }

    private fun createEngine(
        inferenceAdapter: SignInferenceAdapter = RecordingInferenceAdapter(),
    ): RealSignRecognitionEngine =
        RealSignRecognitionEngine(
            featureEncoder = LandmarkFeatureEncoder(),
            sequenceBuffer = SignSequenceBuffer(sequenceLength = TEST_SEQUENCE_LENGTH),
            inferenceAdapter = inferenceAdapter,
        )

    private class RecordingInferenceAdapter(
        private val shouldThrow: Boolean = false,
    ) : SignInferenceAdapter {
        var predictCallCount = 0
            private set

        override fun predict(sequence: FloatArray): SignInferenceResult {
            predictCallCount += 1
            check(!shouldThrow) { "테스트용 추론 실패" }
            return SignInferenceResult(
                gloss = TEST_GLOSS,
                confidence = TEST_CONFIDENCE,
            )
        }

        override fun close() = Unit
    }

    private fun createFrame(timestampMs: Long): LandmarkFrameResult =
        LandmarkFrameResult(
            timestampMs = timestampMs,
            pose =
                LandmarkGroup(
                    type = LandmarkGroupType.POSE,
                    landmarks = createPoseLandmarks(),
                ),
            leftHand =
                HandLandmarks(
                    side = HandSide.LEFT,
                    landmarks = createLandmarks(SignFeatureSpec.HAND_LANDMARK_COUNT),
                ),
            rightHand =
                HandLandmarks(
                    side = HandSide.RIGHT,
                    landmarks = createLandmarks(SignFeatureSpec.HAND_LANDMARK_COUNT),
                ),
            lips =
                LandmarkGroup(
                    type = LandmarkGroupType.LIPS,
                    landmarks = createLandmarks(SignFeatureSpec.LIPS_LANDMARK_COUNT),
                ),
        )

    private fun createPoseLandmarks(): List<LandmarkPoint> =
        createLandmarks(SignFeatureSpec.POSE_LANDMARK_COUNT).toMutableList().also { landmarks ->
            landmarks[SignFeatureSpec.LEFT_SHOULDER_INDEX] = LandmarkPoint(0f, 0f, 0f)
            landmarks[SignFeatureSpec.RIGHT_SHOULDER_INDEX] = LandmarkPoint(1f, 0f, 0f)
        }

    private fun createLandmarks(count: Int): List<LandmarkPoint> =
        List(count) { index ->
            LandmarkPoint(
                x = index.toFloat() / count,
                y = 0f,
                z = 0f,
            )
        }

    private companion object {
        const val TEST_SEQUENCE_LENGTH = 2
        const val TEST_GLOSS = "엄마"
        const val TEST_CONFIDENCE = 0.91f
        const val FLOAT_DELTA = 0.0001f
        const val TIMEOUT_MS = 1_000L
    }
}
