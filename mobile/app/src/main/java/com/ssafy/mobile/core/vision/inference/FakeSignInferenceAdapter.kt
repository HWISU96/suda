package com.ssafy.mobile.core.vision.inference

class FakeSignInferenceAdapter(
    private val result: SignInferenceResult = DEFAULT_RESULT,
) : SignInferenceAdapter {
    private var isClosed = false

    override fun predict(sequence: FloatArray): SignInferenceResult {
        check(!isClosed) { "이미 종료된 fake 수어 추론 어댑터입니다." }
        require(sequence.size == EXPECTED_SEQUENCE_INPUT_SIZE) {
            "Sequence 입력 크기는 $EXPECTED_SEQUENCE_INPUT_SIZE 이어야 합니다."
        }

        return result
    }

    override fun close() {
        isClosed = true
    }

    companion object {
        const val DEFAULT_GLOSS = "엄마"
        const val DEFAULT_CONFIDENCE = 0.92f
        const val EXPECTED_SEQUENCE_INPUT_SIZE =
            SignModelContract.SEQUENCE_LENGTH * SignModelContract.FEATURE_DIMENSION

        val DEFAULT_RESULT =
            SignInferenceResult(
                gloss = DEFAULT_GLOSS,
                confidence = DEFAULT_CONFIDENCE,
            )
    }
}
