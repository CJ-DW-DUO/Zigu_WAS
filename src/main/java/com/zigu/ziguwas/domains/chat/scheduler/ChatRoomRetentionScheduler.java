package com.zigu.ziguwas.domains.chat.scheduler;

import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.chat.repository.ChatMessageRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모든 참여자가 나간 뒤 보관기간이 지난 채팅방을 메시지와 함께 매일 정리합니다.
 *
 * 메시지 단위가 아니라 채팅방 단위로 정리하는 이유:
 * 메시지마다 보관기간을 적용하면 지금도 잘 쓰고 있는 채팅방에서 오래된 대화만 사라집니다.
 * 삭제 기준은 "마지막 참여자가 나간 시각(ChatRoom.closedAt)"이어야, 살아있는 채팅방의
 * 대화는 아무리 오래돼도 보존되고 완전히 버려진 채팅방만 통째로 정리됩니다.
 *
 * 채팅 이미지의 S3 객체는 지우지 않습니다.
 * 메시지의 imageUrl 은 클라이언트가 보낸 문자열을 그대로 저장한 값이라, 이 값으로 S3 객체를 지우면
 * 다른 사용자의 이미지 URL 을 넣어 그 객체를 지울 수 있습니다. (S3Service.deleteFile 은 URL 마지막 / 뒤를 같은 버킷의 키로 삭제)
 * 그래서 방과 메시지 문서만 지우고, S3 에 남는 고아 객체는 감수합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomRetentionScheduler {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;

    // 폐쇄된 채팅방의 보관기간(일). 이 기간이 지나면 채팅방과 메시지가 영구 삭제된다.
    @Value("${chat.room.retention-days}")
    private int retentionDays;

    // 한 번에 메모리로 들고 올 채팅방 수
    private static final int BATCH_SIZE = 100;

    // 한 번 실행에서 처리할 최대 배치 수 (BATCH_SIZE * 이 값 = 1회 실행 상한)
    // 첫 실행처럼 밀린 데이터가 많더라도 새벽 배치가 무한정 도는 것을 막기 위한 안전장치
    private static final int MAX_BATCH_PER_RUN = 100;

    /**
     * 매일 새벽 4시(KST)에 실행됩니다.
     *
     * 트래픽이 가장 적은 시간대를 골라, 삭제로 인한 부하가 사용자에게 보이지 않도록 합니다.
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredChatRooms() {

        // 1. 보관 기준 시각 계산 (이 시각보다 앞서 폐쇄된 채팅방이 삭제 대상)
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        // 2. 오래 폐쇄된 순으로 배치 크기만큼 끊어서 조회
        // 처리한 채팅방은 즉시 삭제되므로 페이지를 넘기지 않고 항상 첫 페이지만 다시 조회하면 된다.
        Pageable batch = PageRequest.of(0, BATCH_SIZE, Sort.by("closedAt").ascending());

        int deletedRoomCount = 0;
        long deletedMessageCount = 0;
        boolean reachedLimit = true;

        for (int i = 0; i < MAX_BATCH_PER_RUN; i++) {

            List<ChatRoom> expiredRooms = chatRoomRepository.findByClosedAtBefore(cutoff, batch);

            // 2-1. 삭제할 채팅방이 더 없으면 종료
            if (expiredRooms.isEmpty()) {
                reachedLimit = false;
                break;
            }

            for (ChatRoom expiredRoom : expiredRooms) {

                // 2-2. S3 이미지는 지우지 않는다. (imageUrl 이 클라이언트 입력이라 남의 객체를 지울 수 있음)
                // ponytail: 고아 객체 감수. 업로드 시 서버가 채팅 이미지 전용 키 접두사(예: chat/)로 올리고, 배치는 그 접두사의 키만 지우도록 바꾸면 다시 켤 수 있다.

                // 2-3. 메시지 -> 참여 정보 -> 채팅방 순서로 삭제
                // 참여 정보보다 채팅방을 먼저 지우면 갈 곳 없는 참여 정보가 남아
                // 해당 사용자의 채팅방 목록 조회 전체가 실패할 수 있다.
                deletedMessageCount += chatMessageRepository.countByChatRoomId(expiredRoom.getId());
                chatMessageRepository.deleteAllByChatRoomId(expiredRoom.getId());
                chatParticipantRepository.deleteAllByChatRoomId(expiredRoom.getId());
                chatRoomRepository.deleteById(expiredRoom.getId());

                deletedRoomCount++;
            }

            // 2-4. 배치 크기보다 적게 조회됐다면 남은 대상이 없으므로 조회를 한 번 더 하지 않고 종료
            if (expiredRooms.size() < BATCH_SIZE) {
                reachedLimit = false;
                break;
            }
        }

        // 3. 결과 기록
        log.info("보관기간이 지난 채팅방 {}개(메시지 {}건) 삭제 완료 (보관기간 {}일, 기준시각 {})",
                deletedRoomCount, deletedMessageCount, retentionDays, cutoff);

        // 1회 실행 상한에 걸렸다면 남은 대상은 다음날 처리되므로, 반복 발생 시 상한 조정이 필요하다.
        if (reachedLimit) {
            log.warn("1회 실행 상한({}개)에 도달했습니다. 남은 채팅방은 다음 실행에서 처리됩니다.",
                    BATCH_SIZE * MAX_BATCH_PER_RUN);
        }
    }
}
