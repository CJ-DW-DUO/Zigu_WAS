package com.zigu.ziguwas.domains.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zigu.ziguwas.domains.user.dto.auth.request.SignupReqDto;
import com.zigu.ziguwas.domains.user.dto.mypage.request.MyPageProfileReqDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 학번은 학교가 부여한 학생번호 전체(숫자 7~10자리). 가입에서는 여전히 필수가 아니다(없으면 통과). */
class StudentNumberValidationTest {

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private long signupViolations(String studentNumber) throws Exception {
        String sn = studentNumber == null ? "null" : "\"" + studentNumber + "\"";
        SignupReqDto dto = mapper.readValue("{\"email\":\"a@snu.ac.kr\",\"nickname\":\"홍길동\",\"studentNumber\":" + sn
                + ",\"password\":\"Zigu123!@#\",\"termsAgreed\":true}", SignupReqDto.class);
        Set<ConstraintViolation<SignupReqDto>> v = validator.validate(dto);
        return v.stream().filter(x -> x.getPropertyPath().toString().equals("studentNumber")).count();
    }

    private long profileViolations(String studentNumber) throws Exception {
        String sn = studentNumber == null ? "null" : "\"" + studentNumber + "\"";
        MyPageProfileReqDto dto = mapper.readValue("{\"studentNumber\":" + sn + "}", MyPageProfileReqDto.class);
        return validator.validate(dto).stream().filter(x -> x.getPropertyPath().toString().equals("studentNumber")).count();
    }

    @Test
    void 가입은_7에서_10자리_숫자만_통과() throws Exception {
        for (String ok : new String[]{"1234567", "20261234", "2026123456"}) {
            assertEquals(0, signupViolations(ok), ok);
        }
        for (String bad : new String[]{"26", "123456", "12345678901", "abc1234567", "2026 1234", ""}) {
            assertEquals(1, signupViolations(bad), bad);
        }
        // 필수는 아니다: 보내지 않으면 통과
        assertEquals(0, signupViolations(null));
    }

    @Test
    void 프로필_수정도_같은_규칙() throws Exception {
        assertEquals(0, profileViolations("1234567"));
        assertEquals(0, profileViolations("2026123456"));
        assertEquals(1, profileViolations("26"));
        assertEquals(1, profileViolations("12345678901"));
        assertEquals(0, profileViolations(null));
        assertTrue(validator.validate(mapper.readValue("{\"studentNumber\":\"26\"}", MyPageProfileReqDto.class)).stream()
                .anyMatch(v -> v.getMessage().equals("학번은 7~10자리 숫자로 입력해주세요.")));
    }
}
