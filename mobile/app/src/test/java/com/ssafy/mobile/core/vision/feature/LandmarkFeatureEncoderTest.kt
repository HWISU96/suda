package com.ssafy.mobile.core.vision.feature

import com.ssafy.mobile.core.vision.inference.SignModelContract
import com.ssafy.mobile.core.vision.landmark.HandLandmarks
import com.ssafy.mobile.core.vision.landmark.HandSide
import com.ssafy.mobile.core.vision.landmark.LandmarkFrameResult
import com.ssafy.mobile.core.vision.landmark.LandmarkGroup
import com.ssafy.mobile.core.vision.landmark.LandmarkGroupType
import com.ssafy.mobile.core.vision.landmark.LandmarkPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandmarkFeatureEncoderTest {
    @Test
    fun encodesLandmarkFrameToFixedLengthFeature() {
        val feature = LandmarkFeatureEncoder().encode(createFrame()).values

        assertEquals(SignModelContract.FEATURE_DIMENSION, feature.size)
        assertEquals(POSE_X_NORMALIZED, feature[SignFeatureSpec.POSE_OFFSET], FLOAT_DELTA)
        assertEquals(LEFT_HAND_X_NORMALIZED, feature[SignFeatureSpec.LEFT_HAND_OFFSET], FLOAT_DELTA)
        assertEquals(
            RIGHT_HAND_X_NORMALIZED,
            feature[SignFeatureSpec.RIGHT_HAND_OFFSET],
            FLOAT_DELTA,
        )
        assertEquals(LIPS_X_NORMALIZED, feature[SignFeatureSpec.LIPS_OFFSET], FLOAT_DELTA)
    }

    @Test
    fun forwardFillsMissingLandmarksFromPreviousFrame() {
        val encoder = LandmarkFeatureEncoder()
        encoder.encode(createFrame())

        val feature =
            encoder
                .encode(createFrame(leftHand = emptyList()))
                .values

        assertEquals(
            LEFT_HAND_X_NORMALIZED,
            feature[SignFeatureSpec.LEFT_HAND_OFFSET],
            FLOAT_DELTA,
        )
    }

    @Test
    fun resetClearsForwardFillState() {
        val encoder = LandmarkFeatureEncoder()
        encoder.encode(createFrame())
        encoder.reset()

        val feature =
            encoder
                .encode(createFrame(leftHand = emptyList()))
                .values

        assertEquals(
            INITIAL_EMPTY_X_NORMALIZED,
            feature[SignFeatureSpec.LEFT_HAND_OFFSET],
            FLOAT_DELTA,
        )
    }

    @Test
    fun encodesInitialEmptyFrameToFixedLengthNoneFrame() {
        val feature =
            LandmarkFeatureEncoder()
                .encode(LandmarkFrameResult.empty(timestampMs = TIMESTAMP_MS))
                .values

        assertEquals(SignModelContract.FEATURE_DIMENSION, feature.size)
        assertTrue(feature.all { it == 0f })
    }

    private fun createFrame(
        leftHand: List<LandmarkPoint> = createDefaultLeftHandLandmarks(),
    ): LandmarkFrameResult =
        LandmarkFrameResult(
            timestampMs = TIMESTAMP_MS,
            pose =
                LandmarkGroup(
                    type = LandmarkGroupType.POSE,
                    landmarks = createPoseLandmarks(),
                ),
            leftHand =
                HandLandmarks(
                    side = HandSide.LEFT,
                    landmarks = leftHand,
                ),
            rightHand =
                HandLandmarks(
                    side = HandSide.RIGHT,
                    landmarks =
                        createLandmarks(
                            count = SignFeatureSpec.HAND_LANDMARK_COUNT,
                            firstPoint = LandmarkPoint(0.25f, 0f, 0f),
                        ),
                ),
            lips =
                LandmarkGroup(
                    type = LandmarkGroupType.LIPS,
                    landmarks =
                        createLandmarks(
                            count = SignFeatureSpec.LIPS_LANDMARK_COUNT,
                            firstPoint = LandmarkPoint(1f, 0f, 0f),
                        ),
                ),
        )

    private fun createDefaultLeftHandLandmarks(): List<LandmarkPoint> =
        createLandmarks(
            count = SignFeatureSpec.HAND_LANDMARK_COUNT,
            firstPoint = LandmarkPoint(0.75f, 0f, 0f),
        )

    private fun createPoseLandmarks(): List<LandmarkPoint> =
        createLandmarks(
            count = SignFeatureSpec.POSE_LANDMARK_COUNT,
            firstPoint = LandmarkPoint(0.5f, 0f, 0f),
        ).toMutableList().also { landmarks ->
            landmarks[SignFeatureSpec.LEFT_SHOULDER_INDEX] = LandmarkPoint(0f, 0f, 0f)
            landmarks[SignFeatureSpec.RIGHT_SHOULDER_INDEX] = LandmarkPoint(1f, 0f, 0f)
        }

    private fun createLandmarks(
        count: Int,
        firstPoint: LandmarkPoint,
    ): List<LandmarkPoint> =
        List(count) { index ->
            if (index == 0) {
                firstPoint
            } else {
                LandmarkPoint(0f, 0f, 0f)
            }
        }

    private companion object {
        const val TIMESTAMP_MS = 1_000L
        const val FLOAT_DELTA = 0.0001f
        const val POSE_X_NORMALIZED = 0f
        const val LEFT_HAND_X_NORMALIZED = 0.25f
        const val RIGHT_HAND_X_NORMALIZED = -0.25f
        const val LIPS_X_NORMALIZED = 0.5f
        const val INITIAL_EMPTY_X_NORMALIZED = -0.5f
    }
}
