package com.zigu.ziguwas.domains.chat.scheduler;

import com.zigu.ziguwas.S3.S3Service;
import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 모든 참여자가 나간 뒤 보관기간이 지난 채팅방을 메시지와 함께 매일 정리합니다.
 *
 * 메시지 단위가 아니라 채팅방 단위로 정리하는 이유:
 * 메시지마다 보관기간을 적용하면 지금도 잘 쓰고 있는 채팅방에서 오래된 대화만 사라집니다.
 * 삭제 기준은 "마지막 참여자가 나간 시각(ChatRoom.closedAt)"이어야, 살아있는 채팅방의
 * 대화는 아무리 오래돼도 보존되고 완전히 버려진 채팅방만 통째로 정리됩니다.
 *
 * Mongo의 TTL 인덱스를 쓰지 않는 이유:
 * 채팅 메시지는 S3에 올라간 이미지를 참조하는데, TTL은 문서만 지우기 때문에
 * 문서와 함께 imageUrl이 사라지면 어떤 S3 객체가 만료된 채팅 이미지였는지 알 수 없게 됩니다.
 * 그래서 S3 객체를 먼저 지우고 문서를 지우는 순서를 배치가 직접 보장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomRetentionScheduler {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final S3Service s3Service;

    // 폐쇄된 채팅방의 보관기간(일). 이 기간이 지나면 채팅방과 메시지가 이미지까지 영구 삭제된다.
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

        // S3 삭제에 실패해 남겨둔 채팅방.
        // 이 방들은 계속 조회 결과의 맨 앞에 남으므로, 같은 실행 안에서 반복 재시도하지 않도록 기억해 둔다.
        Set<String> failedRoomIds = new HashSet<>();

        for (int i = 0; i < MAX_BATCH_PER_RUN; i++) {

            List<ChatRoom> expiredRooms = chatRoomRepository.findByClosedAtBefore(cutoff, batch);

            // 2-1. 삭제할 채팅방이 더 없으면 종료
            if (expiredRooms.isEmpty()) {
                reachedLimit = false;
                break;
            }

            int deletedInThisBatch = 0;

            for (ChatRoom expiredRoom : expiredRooms) {

                // 2-2. 앞선 배치에서 이미 S3 삭제에 실패한 채팅방은 건너뛴다. (다음날 배치가 다시 시도)
                if (failedRoomIds.contains(expiredRoom.getId())) {
                    continue;
                }

                // 2-3. 채팅방에 남은 S3 이미지를 먼저 삭제
                // 문서를 먼저 지우면 imageUrl을 알 수 없게 되어 S3에 고아 객체가 남는다.
                if (!deleteImagesOf(expiredRoom.getId())) {
                    failedRoomIds.add(expiredRoom.getId());
                    continue;
                }

                // 2-4. 메시지 -> 참여 정보 -> 채팅방 순서로 삭제
                // 참여 정보보다 채팅방을 먼저 지우면 갈 곳 없는 참여 정보가 남아
                // 해당 사용자의 채팅방 목록 조회 전체가 실패할 수 있다.
                deletedMessageCount += chatMessageRepository.countByChatRoomId(expiredRoom.getId());
                chatMessageRepository.deleteAllByChatRoomId(expiredRoom.getId());
                chatParticipantRepository.deleteAllByChatRoomId(expiredRoom.getId());
                chatRoomRepository.deleteById(expiredRoom.getId());

                deletedRoomCount++;
                deletedInThisBatch++;
            }

            // 2-5. 이번 배치에서 하나도 지우지 못했다면(전부 S3 삭제 실패) 같은 대상을 계속 붙잡지 않도록 종료
            if (deletedInThisBatch == 0) {
                reachedLimit = false;
                break;
            }

            // 2-6. 배치 크기보다 적게 조회됐다면 남은 대상이 없으므로 조회를 한 번 더 하지 않고 종료
            if (expiredRooms.size() < BATCH_SIZE) {
                reachedLimit = false;
                break;
            }
        }

        // 3. 결과 기록
        log.info("보관기간이 지난 채팅방 {}개(메시지 {}건) 삭제 완료 (보관기간 {}일, 기준시각 {}, S3 삭제 실패 {}개)",
                deletedRoomCount, deletedMessageCount, retentionDays, cutoff, failedRoomIds.size());

        // 1회 실행 상한에 걸렸다면 남은 대상은 다음날 처리되므로, 반복 발생 시 상한 조정이 필요하다.
        if (reachedLimit) {
            log.warn("1회 실행 상한({}개)에 도달했습니다. 남은 채팅방은 다음 실행에서 처리됩니다.",
                    BATCH_SIZE * MAX_BATCH_PER_RUN);
        }
    }

    /**
     * 채팅방에 첨부된 이미지를 S3에서 모두 삭제합니다.
     *
     * 한 건이라도 실패하면 채팅방 전체를 다음날로 미룬다. 문서를 남겨두면 다음 실행에서
     * 다시 시도할 수 있고, 이미 지워진 S3 객체를 한 번 더 지우는 것은 문제가 되지 않기 때문이다.
     *
     * @param chatRoomId 채팅방ID
     * @return 전부 삭제되었으면 true, 한 건이라도 실패했으면 false
     */
    private boolean deleteImagesOf(String chatRoomId) {
        List<ChatMessage> imageMessages = chatMessageRepository.findByChatRoomIdAndImageUrlNotNull(chatRoomId);

        for (ChatMessage imageMessage : imageMessages) {
            try {
                s3Service.deleteFile(imageMessage.getImageUrl());
            } catch (Exception e) {
                log.warn("만료 채팅 이미지 S3 삭제 실패 - chatRoomId={}, messageId={}, imageUrl={}",
                        chatRoomId, imageMessage.getId(), imageMessage.getImageUrl(), e);
                return false;
            }
        }

        return true;
    }
}
