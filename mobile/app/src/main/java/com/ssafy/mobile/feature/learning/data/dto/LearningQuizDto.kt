package com.ssafy.mobile.feature.learning.data.dto

import com.google.gson.annotations.SerializedName
import com.ssafy.mobile.feature.learning.domain.model.LearningQuizAnswerResult
import com.ssafy.mobile.feature.learning.domain.model.LearningQuizQuestion
import com.ssafy.mobile.feature.learning.domain.model.LearningQuizResult
import com.ssafy.mobile.feature.learning.domain.model.LearningQuizResultAnswer
import com.ssafy.mobile.feature.learning.domain.model.LearningQuizSession
import com.ssafy.mobile.feature.learning.domain.model.LearningQuizSessionQuestion
import com.ssafy.mobile.feature.learning.domain.model.LearningQuizSessionStatus

data class LearningQuizSessionRequestDto(
    @SerializedName("childProfileId")
    val childProfileId: Long,
    @SerializedName("categoryId")
    val categoryId: Long,
    @SerializedName("difficulty")
    val difficulty: String,
    @SerializedName("totalQuestionCount")
    val totalQuestionCount: Int,
)

data class LearningQuizSessionResponseDto(
    @SerializedName("sessionId")
    val sessionId: Long,
    @SerializedName("categoryId")
    val categoryId: Long? = null,
    @SerializedName("difficulty")
    val difficulty: String? = null,
    @SerializedName("totalQuestionCount")
    val totalQuestionCount: Int? = null,
    @SerializedName("currentQuestionNumber")
    val currentQuestionNumber: Int? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("imageUrls")
    val imageUrls: List<String>? = null,
    @SerializedName("questions")
    val questions: List<LearningQuizSessionQuestionDto>? = null,
) {
    fun toDomain(): LearningQuizSession =
        LearningQuizSession(
            sessionId = sessionId,
            categoryId = categoryId ?: 0L,
            difficulty = difficulty.orEmpty(),
            totalQuestionCount = totalQuestionCount ?: questions?.size ?: 0,
            currentQuestionNumber = currentQuestionNumber ?: DEFAULT_QUESTION_NUMBER,
            status = status.orEmpty(),
            imageUrls = imageUrls.orEmpty(),
            questions = questions.orEmpty().map { it.toDomain() },
        )
}

data class LearningQuizSessionQuestionDto(
    @SerializedName("questionId")
    val questionId: Long,
    @SerializedName("questionNumber")
    val questionNumber: Int,
    @SerializedName("targetText")
    val targetText: String? = null,
    @SerializedName("wordId")
    val wordId: Long? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    @SerializedName("audioUrl")
    val audioUrl: String? = null,
) {
    fun toDomain(): LearningQuizSessionQuestion =
        LearningQuizSessionQuestion(
            questionId = questionId,
            questionNumber = questionNumber,
            targetText = targetText.orEmpty(),
            wordId = wordId,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
        )
}

data class LearningQuizCurrentQuestionResponseDto(
    @SerializedName("sessionId")
    val sessionId: Long,
    @SerializedName("questionId")
    val questionId: Long,
    @SerializedName("wordId")
    val wordId: Long,
    @SerializedName("questionNumber")
    val questionNumber: Int,
    @SerializedName("totalQuestionCount")
    val totalQuestionCount: Int,
    @SerializedName("imageUrl")
    val imageUrl: String,
    @SerializedName("audioUrl")
    val audioUrl: String? = null,
    @SerializedName("targetText")
    val targetText: String? = null,
    @SerializedName("word")
    val word: String? = null,
    @SerializedName("displayText")
    val displayText: String? = null,
) {
    fun toDomain(): LearningQuizQuestion =
        LearningQuizQuestion(
            sessionId = sessionId,
            questionId = questionId,
            wordId = wordId,
            questionNumber = questionNumber,
            totalQuestionCount = totalQuestionCount,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            targetText = targetText ?: word ?: displayText,
        )
}

data class LearningQuizAnswerResponseDto(
    @SerializedName("sessionId")
    val sessionId: Long? = null,
    @SerializedName("questionId")
    val questionId: Long? = null,
    @SerializedName("wordId")
    val wordId: Long? = null,
    @SerializedName("targetText")
    val targetText: String? = null,
    @SerializedName("recognizedText")
    val recognizedText: String? = null,
    @SerializedName("isCorrect")
    val isCorrect: Boolean? = null,
    @SerializedName("star")
    val star: Int? = null,
    @SerializedName("feedback")
    val feedback: String? = null,
    @SerializedName("hasNext")
    val hasNext: Boolean? = null,
    @SerializedName("nextQuestionNumber")
    val nextQuestionNumber: Int? = null,
) {
    fun toDomain(
        fallbackSessionId: Long,
        fallbackQuestionId: Long,
    ): LearningQuizAnswerResult {
        val normalizedScore =
            normalizeQuizAnswerScore(
                recognizedText = recognizedText,
                isCorrect = isCorrect,
                star = star,
            )

        return LearningQuizAnswerResult(
            sessionId = sessionId ?: fallbackSessionId,
            questionId = questionId ?: fallbackQuestionId,
            wordId = wordId ?: UNKNOWN_WORD_ID,
            targetText = targetText.orEmpty(),
            recognizedText = normalizedScore.recognizedText,
            isCorrect = normalizedScore.isCorrect,
            star = normalizedScore.star,
            feedback = feedback,
            hasNext = hasNext ?: false,
            nextQuestionNumber = nextQuestionNumber,
        )
    }
}

data class LearningQuizSessionStatusRequestDto(
    @SerializedName("status")
    val status: String,
)

data class LearningQuizSessionStatusResponseDto(
    @SerializedName("sessionId")
    val sessionId: Long,
    @SerializedName("status")
    val status: String,
    @SerializedName("endedAt")
    val endedAt: String,
) {
    fun toDomain(): LearningQuizSessionStatus =
        LearningQuizSessionStatus(
            sessionId = sessionId,
            status = status,
            endedAt = endedAt,
        )
}

data class LearningQuizResultResponseDto(
    @SerializedName("sessionId")
    val sessionId: Long,
    @SerializedName("totalQuestionCount")
    val totalQuestionCount: Int,
    @SerializedName("correctCount")
    val correctCount: Int,
    @SerializedName("totalStar")
    val totalStar: Int,
    @SerializedName("answers")
    val answers: List<LearningQuizResultAnswerDto>,
) {
    fun toDomain(): LearningQuizResult {
        val normalizedAnswers = answers.map { it.toDomain() }

        return LearningQuizResult(
            sessionId = sessionId,
            totalQuestionCount = totalQuestionCount,
            correctCount = normalizedAnswers.count { it.isCorrect },
            totalStar = normalizedAnswers.sumOf { it.star },
            answers = normalizedAnswers,
        )
    }
}

data class LearningQuizResultAnswerDto(
    @SerializedName("questionId")
    val questionId: Long,
    @SerializedName("wordId")
    val wordId: Long,
    @SerializedName("targetText")
    val targetText: String,
    @SerializedName("recognizedText")
    val recognizedText: String? = null,
    @SerializedName("isCorrect")
    val isCorrect: Boolean,
    @SerializedName("star")
    val star: Int,
    @SerializedName("feedback")
    val feedback: String? = null,
) {
    fun toDomain(): LearningQuizResultAnswer {
        val normalizedScore =
            normalizeQuizAnswerScore(
                recognizedText = recognizedText,
                isCorrect = isCorrect,
                star = star,
            )

        return LearningQuizResultAnswer(
            questionId = questionId,
            wordId = wordId,
            targetText = targetText,
            recognizedText = normalizedScore.recognizedText,
            isCorrect = normalizedScore.isCorrect,
            star = normalizedScore.star,
            feedback = feedback,
        )
    }
}

private const val DEFAULT_QUESTION_NUMBER = 1
private const val UNKNOWN_WORD_ID = -1L
private const val DEFAULT_QUIZ_STAR = 1
private const val NO_SPEECH_QUIZ_STAR = 0

private data class NormalizedQuizAnswerScore(
    val recognizedText: String,
    val isCorrect: Boolean,
    val star: Int,
)

private fun normalizeQuizAnswerScore(
    recognizedText: String?,
    isCorrect: Boolean?,
    star: Int?,
): NormalizedQuizAnswerScore {
    val normalizedRecognizedText = recognizedText.orEmpty().trim()
    if (normalizedRecognizedText.isBlank()) {
        return NormalizedQuizAnswerScore(
            recognizedText = "",
            isCorrect = false,
            star = NO_SPEECH_QUIZ_STAR,
        )
    }

    return NormalizedQuizAnswerScore(
        recognizedText = normalizedRecognizedText,
        isCorrect = isCorrect ?: false,
        star = star ?: DEFAULT_QUIZ_STAR,
    )
}
