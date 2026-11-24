package com.zigu.ziguwas.domains.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zigu.ziguwas.domains.user.dto.auth.request.PassWordUpdateReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.PasswordResetReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.SignupReqDto;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.exception.ErrorDto;
import com.zigu.ziguwas.exception.GlobalExceptionHandler;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/** S0 W-12(비밀번호 규칙을 FE 와 동일하게) · W-21(ErrorDto.code) 검증. */
class PasswordRuleAndErrorCodeTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String[] GOOD = {"abcd123!", "Zigu1234!@#$%^&*", "a1!aaaaa"};
    // 숫자 없음 / 특수문자 없음 / 영문 없음 / 7자 / 17자
    private static final String[] BAD = {"abcdefg!", "abcd1234", "12345678!", "ab12!cd", "abcd1234!abcd12345"};

    private SignupReqDto signup(String pw) {
        SignupReqDto dto = new SignupReqDto();
        dto.setEmail("a@snu.ac.kr");
        dto.setNickname("홍길동");
        dto.setPassword(pw);
        dto.setTermsAgreed(true);
        return dto;
    }

    private PasswordResetReqDto reset(String pw) {
        PasswordResetReqDto dto = new PasswordResetReqDto();
        dto.setEmail("a@snu.ac.kr");
        dto.setNewPassword(pw);
        dto.setConfirmPassword(pw);
        return dto;
    }

    private PassWordUpdateReqDto update(String pw) {
        return PassWordUpdateReqDto.builder().oldPassword("old").newPassword(pw).confirmPassword(pw).build();
    }

    @Test
    void 가입_재설정_변경_모두_같은_규칙() {
        for (String pw : GOOD) {
            assertTrue(validator.validate(signup(pw)).isEmpty(), "signup " + pw);
            assertTrue(validator.validate(reset(pw)).isEmpty(), "reset " + pw);
            assertTrue(validator.validate(update(pw)).isEmpty(), "update " + pw);
        }
        for (String pw : BAD) {
            assertFalse(validator.validate(signup(pw)).isEmpty(), "signup " + pw);
            assertFalse(validator.validate(reset(pw)).isEmpty(), "reset " + pw);
            assertFalse(validator.validate(update(pw)).isEmpty(), "update " + pw);
        }
    }

    @Test
    void 에러_응답에_code_추가_기존_필드_유지() throws Exception {
        // customExceptionHandler 가 protected 라 익명 하위 클래스로 호출
        var handler = new GlobalExceptionHandler() {
            ResponseEntity<?> handle(CustomException e) {
                return customExceptionHandler(e);
            }
        };
        ResponseEntity<?> res = handler.handle(new CustomException(ErrorCode.INCORRECT_PASSWORD));
        JsonNode json = new ObjectMapper().valueToTree(res.getBody());

        assertEquals(401, json.get("status").asInt());
        assertEquals(ErrorCode.INCORRECT_PASSWORD.getMessage(), json.get("message").asText());
        assertEquals("INCORRECT_PASSWORD", json.get("code").asText());
    }

    @Test
    void ErrorCode_없는_오류는_code_필드가_빠진다() {
        JsonNode json = new ObjectMapper().valueToTree(new ErrorDto(400, "요청 형식이 올바르지 않습니다."));
        assertFalse(json.has("code"));
    }
}
