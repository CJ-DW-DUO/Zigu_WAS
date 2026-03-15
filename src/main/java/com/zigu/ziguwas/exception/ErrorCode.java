package com.zigu.ziguwas.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 유저 관련
    NOT_FOUND_USER(404,"유저를 찾을 수 없습니다."),

    // 인증 관련
    NOT_CERTIFIED_USER(400, "인증되지 않은 사용자 입니다."),
    UNAUTHORIZED_ACCESS(400, "허용되지 않은 접근입니다." ),

    // 기타 관련
    INTERNAL_SERVER_ERROR(500, "내부 서버 오류입니다."),

    // 아이템 관련
    NOT_FOUND_ITEM(404, "아이템을 찾을 수 없습니다." ),

    // 아이템 이미지 관련
    NOT_FOUND_IMAGE(404, "이미지를 찾을 수 없습니다." ),

    // 파일 관련
    FAIL_UPLOAD_FILE(400, "파일업로드 실패"),
    FAIL_DELETE_FILE(400, "파일 삭제 실패" );

    private final int status;
    private final String message;
}
