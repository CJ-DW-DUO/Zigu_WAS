package com.zigu.ziguwas.domains.chat.scheduler;

import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import com.zigu.ziguwas.domains.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 보관기간이 지난 채팅 메시지를 매일 정리합니다.
 *
 * Mongo의 TTL 인덱스를 쓰지 않고 배치로 처리하는 이유:
 * 채팅 메시지는 S3에 올라간 이미지를 참조하는데, TTL은 문서만 지우기 때문에
 * 문서와 함께 imageUrl이 사라지면 어떤 S3 객체가 만료된 채팅 이미지였는지 알 수 없게 됩니다.
 * 그래서 S3 객체를 먼저 지우고 문서를 지우는 순서를 배치가 직접 보장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageRetentionScheduler {

    private final ChatMessageRepository chatMessageRepository;
    private final S3Service s3Service;

    // 메시지 보관기간(일). 이 기간이 지난 메시지는 이미지와 함께 영구 삭제된다.
    @Value("${chat.message.retention-days}")
    private int retentionDays;

    // 한 번에 메모리로 들고 올 메시지 수
    private static final int BATCH_SIZE = 500;

    // 한 번 실행에서 처리할 최대 배치 수 (BATCH_SIZE * 이 값 = 1회 실행 상한)
    // 첫 실행처럼 밀린 데이터가 많더라도 새벽 배치가 무한정 도는 것을 막기 위한 안전장치
    private static final int MAX_BATCH_PER_RUN = 200;

    /**
     * 매일 새벽 4시(KST)에 실행됩니다.
     *
     * 트래픽이 가장 적은 시간대를 골라, 삭제로 인한 부하가 사용자에게 보이지 않도록 합니다.
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredMessages() {

        // 1. 보관 기준 시각 계산 (이 시각보다 앞선 메시지가 삭제 대상)
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        // 2. 오래된 순으로 배치 크기만큼 끊어서 조회
        // 처리한 메시지는 즉시 삭제되므로 페이지를 넘기지 않고 항상 첫 페이지만 다시 조회하면 된다.
        Pageable batch = PageRequest.of(0, BATCH_SIZE, Sort.by("timestamp").ascending());

        int deletedCount = 0;
        boolean reachedLimit = true;

        // S3 삭제에 실패해 문서를 남겨둔 메시지.
        // 이 메시지들은 계속 조회 결과의 맨 앞에 남으므로, 같은 실행 안에서 반복 재시도하지 않도록 기억해 둔다.
        Set<String> failedMessageIds = new HashSet<>();

        for (int i = 0; i < MAX_BATCH_PER_RUN; i++) {

            List<ChatMessage> expiredMessages = chatMessageRepository.findByTimestampBefore(cutoff, batch);

            // 2-1. 삭제할 메시지가 더 없으면 종료
            if (expiredMessages.isEmpty()) {
                reachedLimit = false;
                break;
            }

            List<String> deletableIds = new ArrayList<>();

            for (ChatMessage expiredMessage : expiredMessages) {

                // 2-2. 앞선 배치에서 이미 S3 삭제에 실패한 메시지는 건너뛴다. (다음날 배치가 다시 시도)
                if (failedMessageIds.contains(expiredMessage.getId())) {
                    continue;
                }

                // 2-3. 이미지 메시지라면 S3 객체를 먼저 삭제
                // 문서를 먼저 지우면 imageUrl을 알 수 없게 되어 S3에 고아 객체가 남는다.
                if (expiredMessage.getImageUrl() != null && !expiredMessage.getImageUrl().isEmpty()) {
                    try {
                        s3Service.deleteFile(expiredMessage.getImageUrl());
                    } catch (Exception e) {
                        // 이미지 한 건의 실패로 배치 전체가 멈추지 않도록 로그만 남기고 넘어간다.
                        // 문서를 남겨두면 다음날 배치가 다시 시도한다.
                        failedMessageIds.add(expiredMessage.getId());
                        log.warn("만료 채팅 이미지 S3 삭제 실패 - messageId={}, imageUrl={}",
                                expiredMessage.getId(), expiredMessage.getImageUrl(), e);
                        continue;
                    }
                }

                deletableIds.add(expiredMessage.getId());
            }

            // 2-4. 이번 배치에서 하나도 지우지 못했다면(전부 S3 삭제 실패) 같은 대상을 계속 붙잡지 않도록 종료
            if (deletableIds.isEmpty()) {
                reachedLimit = false;
                break;
            }

            // 2-5. 문서는 한 번의 쿼리로 일괄 삭제
            chatMessageRepository.deleteAllByIdIn(deletableIds);
            deletedCount += deletableIds.size();

            // 2-6. 배치 크기보다 적게 조회됐다면 남은 대상이 없으므로 조회를 한 번 더 하지 않고 종료
            if (expiredMessages.size() < BATCH_SIZE) {
                reachedLimit = false;
                break;
            }
        }

        // 3. 결과 기록
        log.info("만료 채팅 메시지 {}건 삭제 완료 (보관기간 {}일, 기준시각 {}, S3 삭제 실패 {}건)",
                deletedCount, retentionDays, cutoff, failedMessageIds.size());

        // 1회 실행 상한에 걸렸다면 남은 대상은 다음날 처리되므로, 반복 발생 시 상한 조정이 필요하다.
        if (reachedLimit) {
            log.warn("1회 실행 상한({}건)에 도달했습니다. 남은 만료 메시지는 다음 실행에서 처리됩니다.",
                    BATCH_SIZE * MAX_BATCH_PER_RUN);
        }
    }
}
