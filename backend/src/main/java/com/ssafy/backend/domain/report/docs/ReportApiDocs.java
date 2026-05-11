package com.ssafy.backend.domain.report.docs;

import com.ssafy.backend.domain.learn.entity.LearnDifficulty;
import com.ssafy.backend.domain.learn.quiz.entity.QuizSessionStatus;
import com.ssafy.backend.domain.report.dto.ReportQuizSessionDetailResponse;
import com.ssafy.backend.domain.report.dto.ReportQuizSessionListResponse;
import com.ssafy.backend.global.docs.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "리포트 API", description = "아이별 퀴즈 기록 리포트 API")
public interface ReportApiDocs {

  @Operation(
      summary = "퀴즈 기록 목록 조회",
      description = "특정 아이의 퀴즈 기록 목록을 최신순으로 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiErrorCodes({"VALIDATION_INVALID_INPUT", "COMMON_UNAUTHORIZED", "CHILD_PROFILE_NOT_FOUND"})
  @ApiResponse(
      responseCode = "200",
      description = "성공",
      content = @Content(schema = @Schema(implementation = ReportQuizSessionListResponse.class)))
  ResponseEntity<ReportQuizSessionListResponse> getQuizSessions(
      @Parameter(hidden = true) Authentication authentication,
      @Parameter(description = "아이 프로필 ID", example = "1") Long childId,
      String from,
      String to,
      Long categoryId,
      LearnDifficulty difficulty,
      QuizSessionStatus status,
      int page,
      int size);

  @Operation(
      summary = "퀴즈 기록 상세 조회",
      description = "특정 아이의 특정 퀴즈 기록 상세와 문제별 답변 결과를 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiErrorCodes({
    "VALIDATION_INVALID_INPUT",
    "COMMON_UNAUTHORIZED",
    "CHILD_PROFILE_NOT_FOUND",
    "REPORT_NOT_FOUND"
  })
  @ApiResponse(
      responseCode = "200",
      description = "성공",
      content = @Content(schema = @Schema(implementation = ReportQuizSessionDetailResponse.class)))
  ResponseEntity<ReportQuizSessionDetailResponse> getQuizSessionDetail(
      @Parameter(hidden = true) Authentication authentication,
      @Parameter(description = "아이 프로필 ID", example = "1") Long childId,
      @Parameter(description = "퀴즈 세션 ID", example = "10") Long sessionId);
}
