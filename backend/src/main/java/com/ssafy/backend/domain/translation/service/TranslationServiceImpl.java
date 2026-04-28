package com.ssafy.backend.domain.translation.service;

import com.ssafy.backend.domain.translation.dto.SignToSpeechRequestDto;
import com.ssafy.backend.domain.translation.dto.SignToSpeechResponseDto;
import com.ssafy.backend.domain.translation.exception.TranslationErrorCode;
import com.ssafy.backend.global.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TranslationServiceImpl implements TranslationService {

  private static final String MOCK_AUDIO_BASE64 = "BASE64_ENCODED_AUDIO";
  private static final String MOCK_AUDIO_MIME_TYPE = "audio/mpeg";

  @Override
  public SignToSpeechResponseDto translateSignToSpeech(SignToSpeechRequestDto requestDto) {
    List<String> words = requestDto.words();
    if (words == null || words.isEmpty()) {
      throw new BusinessException(TranslationErrorCode.EMPTY_WORDS);
    }

    String correctedText = String.join(" ", words);
    boolean requestTts = requestDto.requestTts() == null || requestDto.requestTts();

    return new SignToSpeechResponseDto(
        words,
        correctedText,
        requestTts ? MOCK_AUDIO_BASE64 : null,
        requestTts ? MOCK_AUDIO_MIME_TYPE : null,
        true);
  }
}
