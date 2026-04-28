package com.ssafy.mobile.core.vision.feature

import com.ssafy.mobile.core.vision.inference.SignModelContract
import java.util.ArrayDeque

class SignSequenceBuffer(
    private val sequenceLength: Int = SignModelContract.SEQUENCE_LENGTH,
) {
    private val frames = ArrayDeque<LandmarkFeatureFrame>(sequenceLength)

    val size: Int
        get() = frames.size

    val hasEnoughFrames: Boolean
        get() = frames.size == sequenceLength

    fun add(frame: LandmarkFeatureFrame) {
        if (frames.size == sequenceLength) {
            frames.removeFirst()
        }
        frames.addLast(frame)
    }

    fun buildSequenceInput(): FloatArray? {
        if (!hasEnoughFrames) {
            return null
        }

        val sequence = FloatArray(sequenceLength * SignModelContract.FEATURE_DIMENSION)
        var offset = 0
        frames.forEach { frame ->
            frame.values.copyInto(
                destination = sequence,
                destinationOffset = offset,
            )
            offset += SignModelContract.FEATURE_DIMENSION
        }
        return sequence
    }

    fun clear() {
        frames.clear()
    }
}
