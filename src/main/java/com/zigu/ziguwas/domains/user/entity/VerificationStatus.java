package com.zigu.ziguwas.domains.user.entity;

/**
 * @author 최정
 * @since 2026.03.12
 *
 * User 인증상태 열거형
 */
public enum VerificationStatus {
    CERTIFIED, // 인증됨
    NOT_CERTIFIED, // 미인증(신규가입)
    PASSWORD_RESET, // 비밀번호 변경
    BANNED // 정지됨
}
