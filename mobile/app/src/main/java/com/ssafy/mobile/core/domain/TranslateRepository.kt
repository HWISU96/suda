package com.ssafy.mobile.core.domain

import com.ssafy.mobile.core.model.SignTranslateResult
import com.ssafy.mobile.core.model.VoiceTranslateResult

/**
 * 서버 통역 API와의 소통 계약을 정의하는 도메인 인터페이스.
 * 구현체는 B 파트가 feature/conversation/data 레이어에서 작성합니다.
 */
interface TranslateRepository {
    /** 수어 단어 목록(gloss)을 서버로 전송하여 자연어 문장으로 교정받습니다. (PATOCH_004) */
    suspend fun translateSign(glosses: List<String>): SignTranslateResult

    /** 아이의 음성 텍스트를 서버로 전송하여 영유아 발음을 보정받습니다. (CHTOPA_002) */
    suspend fun translateVoice(rawText: String): VoiceTranslateResult
}
