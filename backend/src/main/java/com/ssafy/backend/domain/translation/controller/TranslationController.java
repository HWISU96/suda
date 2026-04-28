package com.ssafy.backend.domain.translation.controller;

import com.ssafy.backend.domain.translation.docs.TranslationApiDocs;
import com.ssafy.backend.domain.translation.dto.SignToSpeechRequestDto;
import com.ssafy.backend.domain.translation.dto.SignToSpeechResponseDto;
import com.ssafy.backend.domain.translation.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/translation")
public class TranslationController implements TranslationApiDocs {

  private final TranslationService translationService;

  public TranslationController(TranslationService translationService) {
    this.translationService = translationService;
  }

  @PostMapping("/sign-to-speech")
  @Override
  public ResponseEntity<SignToSpeechResponseDto> translateSignToSpeech(
      @Valid @RequestBody SignToSpeechRequestDto requestDto) {
    return ResponseEntity.ok(translationService.translateSignToSpeech(requestDto));
  }
}
