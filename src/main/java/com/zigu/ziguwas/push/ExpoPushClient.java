package com.zigu.ziguwas.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Expo Push API(https://exp.host/--/api/v2/push/send)를 호출하는 클라이언트입니다.
 *
 * Expo가 FCM/APNs를 대신 중계해주기 때문에, 이 클라이언트는 firebase-admin 같은
 * 별도 SDK 없이 순수 REST 호출만으로 iOS/Android 푸시를 통합 발송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpoPushClient {

    private static final String EXPO_PUSH_API_URL = "https://exp.host/--/api/v2/push/send";
    // Expo가 요청 1건당 허용하는 메시지 최대 개수 (공식 문서 기준)
    private static final int MAX_BATCH_SIZE = 100;

    private final RestTemplate expoRestTemplate;

    /**
     * 메시지 목록을 Expo Push API로 발송합니다.
     *
     * 100개 단위로 잘라 순차 발송하며, 특정 배치 통신 자체가 실패해도(네트워크 오류, 타임아웃 등)
     * 예외를 던지지 않고 그 배치만 건너뛴 채 나머지 배치를 계속 시도합니다.
     * (알림 생성/저장 같은 핵심 흐름이 외부 API 장애 때문에 함께 실패하면 안 되기 때문입니다.)
     *
     * @param messages 발송할 메시지 목록 (100개를 넘어도 내부에서 자동 분할)
     * @return 정상적으로 통신된 배치들의 발송 결과 티켓 목록 (요청 순서 보장)
     */
    public List<ExpoPushTicket> send(List<ExpoPushMessage> messages) {
        List<ExpoPushTicket> tickets = new ArrayList<>();

        for (int i = 0; i < messages.size(); i += MAX_BATCH_SIZE) {
            List<ExpoPushMessage> batch = messages.subList(i, Math.min(i + MAX_BATCH_SIZE, messages.size()));
            tickets.addAll(sendBatch(batch));
        }

        return tickets;
    }

    private List<ExpoPushTicket> sendBatch(List<ExpoPushMessage> batch) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<List<ExpoPushMessage>> request = new HttpEntity<>(batch, headers);

        try {
            ExpoPushResponse response = expoRestTemplate.postForObject(EXPO_PUSH_API_URL, request, ExpoPushResponse.class);
            if (response == null || response.data() == null) {
                log.warn("Expo Push API 응답이 비어있습니다. batchSize={}", batch.size());
                return List.of();
            }
            return response.data();
        } catch (RestClientException e) {
            log.warn("Expo Push API 호출 실패. batchSize={}", batch.size(), e);
            return List.of();
        }
    }
}
