package com.zigu.ziguwas.domains.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zigu.ziguwas.domains.item.dto.request.ItemRegisterReqDto;
import com.zigu.ziguwas.domains.item.dto.request.ItemUpdateReqDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 대여 가격은 0 이상 999,999 이하만 허용(0원은 기존대로 허용). */
class ItemPriceValidationTest {

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private String body(long price) {
        return "{\"title\":\"t\",\"category\":\"ELECTRONICS\",\"dayPerPrice\":" + price + ",\"description\":\"d\"}";
    }

    private long priceViolations(long price) throws Exception {
        return validator.validate(mapper.readValue(body(price), ItemRegisterReqDto.class)).stream()
                .filter(v -> v.getPropertyPath().toString().equals("dayPerPrice")).count();
    }

    private long updatePriceViolations(long price) throws Exception {
        return validator.validate(mapper.readValue(body(price), ItemUpdateReqDto.class)).stream()
                .filter(v -> v.getPropertyPath().toString().equals("dayPerPrice")).count();
    }

    @Test
    void 등록_가격은_0이상_999999이하만_통과() throws Exception {
        assertEquals(0, priceViolations(0));
        assertEquals(0, priceViolations(999999));
        assertEquals(1, priceViolations(1000000));
        assertEquals(1, priceViolations(-1));
    }

    @Test
    void 수정도_같은_규칙() throws Exception {
        assertEquals(0, updatePriceViolations(0));
        assertEquals(0, updatePriceViolations(999999));
        assertEquals(1, updatePriceViolations(1000000));
        assertEquals(1, updatePriceViolations(-5));
        assertTrue(validator.validate(mapper.readValue(body(1000000), ItemUpdateReqDto.class)).stream()
                .anyMatch(v -> v.getMessage().contains("999,999")));
    }
}
