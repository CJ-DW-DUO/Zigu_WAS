package com.zigu.ziguwas.domains.university.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "University API", description = "대학교 관련 API 입니다.")
public interface UniversityApi {

    @Operation(summary = "모든 대학 정보 목록 조회", description = "시스템에 등록된 모든 대학교의 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<?> getAllUniversities();

    @Operation(summary = "대학 검색 정보 조회", description = "대학교 이름을 기반으로 검색된 대학교 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<?> getUniversities(@RequestParam String name);
}
