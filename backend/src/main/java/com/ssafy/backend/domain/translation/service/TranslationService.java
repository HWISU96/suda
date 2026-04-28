package com.ssafy.backend.domain.translation.service;

import com.ssafy.backend.domain.translation.dto.SignToSpeechRequestDto;
import com.ssafy.backend.domain.translation.dto.SignToSpeechResponseDto;

public interface TranslationService {
  SignToSpeechResponseDto translateSignToSpeech(SignToSpeechRequestDto requestDto);
}
