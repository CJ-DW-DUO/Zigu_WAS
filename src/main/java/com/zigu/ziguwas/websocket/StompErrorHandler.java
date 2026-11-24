package com.zigu.ziguwas.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.exception.ErrorDto;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

/**
 * STOMP ERROR 프레임에 에러 내용을 실어 보내는 핸들러
 *
 * 기본 핸들러는 인터셉터(StompHandler)에서 던진 예외를 "Failed to send message to ExecutorSubscribableChannel..." 로
 * 감싸 message 헤더에 넣고 본문을 비워 보낸다. 그러면 클라이언트가 토큰 만료·인증 오류인지 알아볼 수 없다.
 * 그래서 CustomException 의 ErrorCode 와 JWT 파싱 예외를 REST 와 같은 ErrorDto 형태로 message 헤더와 본문에 담는다.
 */
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Message<byte[]> handleInternal(
            StompHeaderAccessor errorHeaderAccessor,
            byte[] errorPayload,
            Throwable cause,
            StompHeaderAccessor clientHeaderAccessor
    ) {
        ErrorDto error = toErrorDto(cause);

        // 우리가 아는 예외가 아니면(내부 오류 등) 기본 동작 그대로 둔다.
        if (error == null) {
            return super.handleInternal(errorHeaderAccessor, errorPayload, cause, clientHeaderAccessor);
        }

        errorHeaderAccessor.setMessage(error.getMessage());
        errorHeaderAccessor.setContentType(MimeTypeUtils.APPLICATION_JSON);

        try {
            return MessageBuilder.createMessage(
                    objectMapper.writeValueAsString(error).getBytes(StandardCharsets.UTF_8),
                    errorHeaderAccessor.getMessageHeaders());
        } catch (JsonProcessingException e) {
            return super.handleInternal(errorHeaderAccessor, errorPayload, cause, clientHeaderAccessor);
        }
    }

    /**
     * 예외(와 그 원인들) 중에서 클라이언트에 알려줄 수 있는 것을 ErrorDto 로 바꿉니다.
     *
     * @param cause 인터셉터에서 던져진 예외 (Spring 이 MessageDeliveryException 으로 감싸 온다)
     * @return 변환 결과, 해당하는 예외가 없으면 null
     */
    private ErrorDto toErrorDto(Throwable cause) {
        for (Throwable t = cause; t != null; t = t.getCause()) {
            if (t instanceof CustomException customException) {
                ErrorCode errorCode = customException.getErrorCode();
                return new ErrorDto(errorCode.getStatus(), errorCode.getMessage());
            }
            // 만료·위조·형식 오류 토큰은 jjwt 예외로 올라온다. REST 필터와 같이 401 로 알린다.
            if (t instanceof JwtException) {
                return new ErrorDto(401, ErrorCode.JWT_TOKEN_PARSING_ERROR.getMessage());
            }
        }
        return null;
    }
}
