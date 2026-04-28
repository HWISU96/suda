package com.ssafy.backend.domain.translation.exception;

import com.ssafy.backend.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TranslationErrorCode implements ErrorCode {
  EMPTY_WORDS("TRANSLATION_EMPTY_WORDS", HttpStatus.UNPROCESSABLE_ENTITY, "수어 단어 배열이 비어 있습니다."),
  SIGN_CORRECTION_FAILED(
      "TRANSLATION_SIGN_CORRECTION_FAILED", HttpStatus.BAD_GATEWAY, "수어 문맥 보정 처리에 실패했습니다."),
  TEXT_TO_SPEECH_FAILED(
      "TRANSLATION_TEXT_TO_SPEECH_FAILED", HttpStatus.BAD_GATEWAY, "음성 합성 처리에 실패했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;

  TranslationErrorCode(String code, HttpStatus httpStatus, String message) {
    this.code = code;
    this.httpStatus = httpStatus;
    this.message = message;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String getDomainTitle() {
    return "번역 처리 오류";
  }

  @Override
  public String getMessage() {
    return message;
  }
}
