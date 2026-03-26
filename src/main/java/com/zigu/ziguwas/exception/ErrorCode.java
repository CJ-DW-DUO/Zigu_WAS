package com.zigu.ziguwas.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 유저 관련
    USER_CREATE_FAILED(500, "사용자 생성에 실패하였습니다."),
    USER_NOT_FOUND(404,"사용자를 찾을 수 없습니다."),

    // 인증 관련
    USER_NOT_CERTIFIED(400, "인증되지 않은 사용자 입니다."),
    UNAUTHORIZED_ACCESS(400, "허용되지 않은 접근입니다." ),
    NOT_MATCHED_UNIV_EMAIL(404, "해당 이메일은 대학 리스트에 존재하지 않습니다."),
    EMAIL_CONFLICTED(409, "해당 이메일은 이미 가입되어 있습니다."),
    EMAIL_SEND_FAILED(500, "이메일 전송에 실패하였습니다."),
    EMAIL_NOT_VERIFIED(401, "해당 이메일은 인증되지 않았습니다."),
    VERIFY_CODE_NOT_FOUND(404, "인증코드를 발급하지 않은 이메일 입니다."),
    VERIFY_CODE_NOT_MATCHED(400, "인증코드가 일치하지 않습니다."),
    NICKNAME_CONFLICT(409, "이미 사용중인 닉네임입니다."),
    INCORRECT_PASSWORD(401, "비밀번호가 일치하지 않습니다."),

    // 대학 관련
    UNIVERSITY_NOT_FOUND(404, "해당 대학교는 존재하지 않습니다."),

    // 기타 관련
    INTERNAL_SERVER_ERROR(500, "내부 서버 오류입니다."),

    // 아이템 관련
    ITEM_NOT_FOUND(404, "아이템을 찾을 수 없습니다." ),

    // 거래 관련
    TRADE_NOT_FOUND(404, "해당 거래내역을 찾을 수 없습니다." ),

    // 아이템 이미지 관련
    IMAGE_NOT_FOUND(404, "이미지를 찾을 수 없습니다." ),
    IMAGE_NOT_BELONG_TO_ITEM(400, "해당 item에 속하지않은 image 입니다." ),

    // 파일 관련
    FILE_UPLOAD_FAIL(400, "파일업로드 실패"),
    FAIL_DELETE_FILE(400, "파일 삭제 실패" );

    private final int status;
    private final String message;
}
