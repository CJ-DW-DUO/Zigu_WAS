package com.zigu.ziguwas.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 유저 관련
    USER_CREATE_FAILED(500, "사용자 생성에 실패하였습니다."),
    USER_NOT_FOUND(404,"사용자를 찾을 수 없습니다."),
    MISS_USER_ID(400, "해당 유저 ID를 입력하지 않았습니다."),

    // 인증 관련
    USER_NOT_CERTIFIED(401, "인증되지 않은 사용자 입니다."),
    UNAUTHORIZED_ACCESS(403, "허용되지 않은 접근입니다." ),
    TOKEN_NOT_FOUND(403, "인증 토큰이 존재하지 않습니다."),
    UNAUTHENTICATED_ACCESS(401, "인증 정보가 올바르지 않습니다."),
    NOT_MATCHED_UNIV_EMAIL(404, "해당 이메일은 대학 리스트에 존재하지 않습니다."),
    EMAIL_CONFLICTED(409, "해당 이메일은 이미 가입되어 있습니다."),
    EMAIL_SEND_FAILED(500, "이메일 전송에 실패하였습니다."),
    EMAIL_NOT_VERIFIED(401, "해당 이메일은 인증되지 않았습니다."),
    VERIFY_CODE_NOT_FOUND(404, "인증코드를 발급하지 않은 이메일 입니다."),
    VERIFY_CODE_NOT_MATCHED(400, "인증코드가 일치하지 않습니다."),
    NICKNAME_CONFLICT(409, "이미 사용중인 닉네임입니다."),
    INCORRECT_PASSWORD(401, "비밀번호가 일치하지 않습니다."),
    INVALID_OLD_PASSWORD(401, "기존 비밀번호와 입력하신 비밀번호는 일치하지 않습니다." ),
    PASSWORD_MISMATCH(401, "새로운 비밀번호와 일치하지 않습니다." ),
    SAME_AS_OLD_PASSWORD(401, "새로운 비밀번호와 기존비밀번호가 동일합니다." ),
    BLACKLIST_TOKEN(401, "탈퇴하거나 로그아웃 된 access token 입니다." ),
    JWT_TOKEN_PARSING_ERROR(400, "토큰 만료 혹은 파싱 에러"),

    // 대학 관련
    UNIVERSITY_NOT_FOUND(404, "해당 대학교는 존재하지 않습니다."),

    // 기타 관련
    INTERNAL_SERVER_ERROR(500, "내부 서버 오류입니다."),

    // 아이템 관련
    ITEM_NOT_FOUND(404, "아이템을 찾을 수 없습니다." ),
    ITEM_ALREADY_RENTING(400, "이미 해당 매물은 대여중입니다."),
    ITEM_NOT_RENTING(400, "해당 매물이 현재 대여중이지 않습니다."),
    WITHDRAWN_USER_ITEM(404, "탈퇴한 작성자의 매물 게시글은 상세 정보를 확인할 수 없습니다."),

    // 거래 관련
    TRADE_NOT_FOUND(404, "해당 거래내역을 찾을 수 없습니다." ),
    RENTER_NOT_MATCHED(400, "로그인정보와 임대인정보가 일치하지 않습니다."),
    TRADE_STATUS_NOT_REQUESTED(400, "거래가 요청상태가 아닙니다."),
    TRADE_STATUS_NOT_INPROGRESSED(400, "거래가 대여중 상태가 아닙니다."),
    SELF_TRADE_NOT_ALLOWED(400, "자기 자신의 물건을 빌릴 수 없습니다."),
    ACTIVE_TRADE_EXISTS(400, "진행중인 거래가 있습니다." ),

    // 아이템 이미지 관련
    IMAGE_NOT_FOUND(404, "이미지를 찾을 수 없습니다." ),
    IMAGE_NOT_BELONG_TO_ITEM(400, "해당 item에 속하지않은 image 입니다." ),

    // 채팅 관련
    CHATROOM_NOT_CREATED(500, "채팅방이 만들어지지 않았습니다."),
    CHATROOM_NOT_FOUND(404, "해당 채팅방은 존재하지 않습니다."),
    CHATMATE_NOT_FOUND(404, "채팅방 상대를 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(404, "채팅 메시지를 찾을 수 없습니다"),
    CHAT_PARTICIPANT_NOT_FOUND(404, "채팅방 참여자를 찾을 수 없습니다."),
    CHATROOM_ALREADY_EXISTS(409, "해당 물품에 대한 채팅방이 이미 존재합니다."),

    // 알림 관련
    NOTIFICATION_NOT_FOUND(404, "해당 알림을 찾을 수 없습니다."),
    NOTIFICATION_USER_NOT_MATCHED(400, "알림 수신자와 로그인 정보가 일치하지 않습니다."),

    // 파일 관련
    FILE_UPLOAD_FAIL(400, "파일업로드 실패"),
    FAIL_DELETE_FILE(400, "파일 삭제 실패" ),

    // 검색 관련
    SEARCH_HISTORY_NOT_FOUND(404, "검색기록이 존재하지 않습니다."),

    // 신고 관련
    SELF_REPORT_NOT_ALLOWED(400, "본인이 등록한 물건 게시글은 신고할 수 없습니다."),
    ALREADY_REPORTED_ITEM(400, "이미 신고 접수된 게시글입니다.");

    private final int status;
    private final String message;
}
