package com.zigu.ziguwas.domains.user.entity;

public enum VerificationStatus {
    CERTIFIED, // 인증됨
    NOT_CERTIFIED, // 미인증(신규가입)
    PASSWORD_RESET, // 비밀번호 변경
    BANNED // 정지됨
}
