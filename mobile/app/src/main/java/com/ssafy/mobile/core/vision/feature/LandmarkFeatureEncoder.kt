package com.ssafy.mobile.core.vision.feature

import com.ssafy.mobile.core.vision.inference.SignModelContract
import com.ssafy.mobile.core.vision.landmark.LandmarkFrameResult
import com.ssafy.mobile.core.vision.landmark.LandmarkPoint
import kotlin.math.sqrt

class LandmarkFeatureEncoder {
    private var previousPose: List<LandmarkPoint>? = null
    private var previousLeftHand: List<LandmarkPoint>? = null
    private var previousRightHand: List<LandmarkPoint>? = null
    private var previousLips: List<LandmarkPoint>? = null
    private var previousShoulderWidth: Float? = null

    fun encode(frame: LandmarkFrameResult): LandmarkFeatureFrame {
        val pose =
            fillLandmarks(
                frame.pose.landmarks,
                previousPose,
                SignFeatureSpec.POSE_LANDMARK_COUNT,
            )
        val leftHand =
            fillLandmarks(
                frame.leftHand.landmarks,
                previousLeftHand,
                SignFeatureSpec.HAND_LANDMARK_COUNT,
            )
        val rightHand =
            fillLandmarks(
                frame.rightHand.landmarks,
                previousRightHand,
                SignFeatureSpec.HAND_LANDMARK_COUNT,
            )
        val lips =
            fillLandmarks(
                frame.lips.landmarks,
                previousLips,
                SignFeatureSpec.LIPS_LANDMARK_COUNT,
            )
        val normalizer = createNormalizer(pose)
        val values =
            FloatArray(SignModelContract.FEATURE_DIMENSION).also { output ->
                var offset = 0
                offset = writeLandmarks(output, offset, pose, normalizer)
                offset = writeLandmarks(output, offset, leftHand, normalizer)
                offset = writeLandmarks(output, offset, rightHand, normalizer)
                writeLandmarks(output, offset, lips, normalizer)
            }

        previousPose = pose
        previousLeftHand = leftHand
        previousRightHand = rightHand
        previousLips = lips

        return LandmarkFeatureFrame(
            timestampMs = frame.timestampMs,
            values = values,
        )
    }

    fun reset() {
        previousPose = null
        previousLeftHand = null
        previousRightHand = null
        previousLips = null
        previousShoulderWidth = null
    }

    private fun fillLandmarks(
        current: List<LandmarkPoint>,
        previous: List<LandmarkPoint>?,
        requiredCount: Int,
    ): List<LandmarkPoint> =
        List(requiredCount) { index ->
            current.getOrNull(index)
                ?: previous?.getOrNull(index)
                ?: ZERO_POINT
        }

    private fun createNormalizer(pose: List<LandmarkPoint>): LandmarkNormalizer {
        val leftShoulder = pose[SignFeatureSpec.LEFT_SHOULDER_INDEX]
        val rightShoulder = pose[SignFeatureSpec.RIGHT_SHOULDER_INDEX]
        val shoulderCenter =
            LandmarkPoint(
                x = (leftShoulder.x + rightShoulder.x) / 2f,
                y = (leftShoulder.y + rightShoulder.y) / 2f,
                z = (leftShoulder.z + rightShoulder.z) / 2f,
            )
        val shoulderWidth = leftShoulder.distance2dTo(rightShoulder)
        val scale =
            when {
                shoulderWidth > MIN_SHOULDER_WIDTH ->
                    shoulderWidth.also { width ->
                        previousShoulderWidth = width
                    }
                previousShoulderWidth != null -> previousShoulderWidth
                else -> DEFAULT_SCALE
            }

        return LandmarkNormalizer(
            center = shoulderCenter,
            scale = scale ?: DEFAULT_SCALE,
        )
    }

    private fun writeLandmarks(
        output: FloatArray,
        startOffset: Int,
        landmarks: List<LandmarkPoint>,
        normalizer: LandmarkNormalizer,
    ): Int {
        var offset = startOffset
        landmarks.forEach { landmark ->
            val normalized = normalizer.normalize(landmark)
            output[offset++] = normalized.x
            output[offset++] = normalized.y
            output[offset++] = normalized.z
        }
        return offset
    }

    private data class LandmarkNormalizer(
        val center: LandmarkPoint,
        val scale: Float,
    ) {
        fun normalize(point: LandmarkPoint): LandmarkPoint =
            LandmarkPoint(
                x = (point.x - center.x) / scale,
                y = (point.y - center.y) / scale,
                z = (point.z - center.z) / scale,
            )
    }

    private fun LandmarkPoint.distance2dTo(other: LandmarkPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    private companion object {
        const val MIN_SHOULDER_WIDTH = 0.0001f
        const val DEFAULT_SCALE = 1f
        val ZERO_POINT = LandmarkPoint(0f, 0f, 0f)
    }
}

class LandmarkFeatureFrame(
    val timestampMs: Long,
    val values: FloatArray,
) {
    init {
        require(values.size == SignModelContract.FEATURE_DIMENSION) {
            "Feature length must be ${SignModelContract.FEATURE_DIMENSION}."
        }
    }
}

object SignFeatureSpec {
    const val POSE_LANDMARK_COUNT = 33
    const val HAND_LANDMARK_COUNT = 21
    const val LIPS_LANDMARK_COUNT = 40
    const val COORDINATE_SIZE = 3
    const val LEFT_SHOULDER_INDEX = 11
    const val RIGHT_SHOULDER_INDEX = 12
    const val POSE_OFFSET = 0
    const val LEFT_HAND_OFFSET = POSE_LANDMARK_COUNT * COORDINATE_SIZE
    const val RIGHT_HAND_OFFSET = LEFT_HAND_OFFSET + HAND_LANDMARK_COUNT * COORDINATE_SIZE
    const val LIPS_OFFSET = RIGHT_HAND_OFFSET + HAND_LANDMARK_COUNT * COORDINATE_SIZE
}
