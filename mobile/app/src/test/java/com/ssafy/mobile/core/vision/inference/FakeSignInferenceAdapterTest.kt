package com.ssafy.mobile.core.vision.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FakeSignInferenceAdapterTest {
    @Test
    fun returnsDefaultFakePredictionForValidSequence() {
        val result = FakeSignInferenceAdapter().predict(createSequence())

        assertEquals(FakeSignInferenceAdapter.DEFAULT_GLOSS, result.gloss)
        assertEquals(FakeSignInferenceAdapter.DEFAULT_CONFIDENCE, result.confidence, FLOAT_DELTA)
    }

    @Test
    fun returnsConfiguredFakePrediction() {
        val adapter =
            FakeSignInferenceAdapter(
                result =
                    SignInferenceResult(
                        gloss = CUSTOM_GLOSS,
                        confidence = CUSTOM_CONFIDENCE,
                    ),
            )

        val result = adapter.predict(createSequence())

        assertEquals(CUSTOM_GLOSS, result.gloss)
        assertEquals(CUSTOM_CONFIDENCE, result.confidence, FLOAT_DELTA)
    }

    @Test
    fun rejectsInvalidSequenceSize() {
        assertThrows(IllegalArgumentException::class.java) {
            FakeSignInferenceAdapter().predict(FloatArray(1))
        }
    }

    @Test
    fun rejectsPredictionAfterClose() {
        val adapter = FakeSignInferenceAdapter()

        adapter.close()

        assertThrows(IllegalStateException::class.java) {
            adapter.predict(createSequence())
        }
    }

    private fun createSequence(): FloatArray =
        FloatArray(FakeSignInferenceAdapter.EXPECTED_SEQUENCE_INPUT_SIZE)

    private companion object {
        const val FLOAT_DELTA = 0.0001f
        const val CUSTOM_GLOSS = "안녕"
        const val CUSTOM_CONFIDENCE = 0.88f
    }
}
