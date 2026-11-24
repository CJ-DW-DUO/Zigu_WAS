package com.zigu.ziguwas.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDto {
    private final int status;
    private final String message;
    // ErrorCode 이름 (FE 가 문구 대신 코드로 분기할 수 있도록 추가). ErrorCode 와 무관한 오류는 null 이라 응답에서 빠진다.
    private final String code;

    public ErrorDto(int status, String message) {
        this(status, message, null);
    }
}
