package com.zigu.ziguwas.domains.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zigu.ziguwas.domains.user.dto.auth.request.SignupReqDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 가입 요청의 학번은 학교가 부여한 학생번호 전체(숫자 7~10자리). 필수는 아니다(없으면 통과). 프로필 수정은 서비스에서 바뀔 때만 검사한다(ProfileStudentNumberTest). */
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
}
