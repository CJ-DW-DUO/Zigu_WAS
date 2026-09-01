package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 조회 하한선 이후에 보낸 메시지 중 가장 최근의 메시지 조회
     *
     * 채팅방을 나갔다가 다시 활성화된 사용자에게는 나가기 이전의 메시지를 미리보기로
     * 노출하면 안 되므로, 참여자별 조회 하한선(visibleFrom)을 함께 넘긴다.
     *
     * @param chatRoomId 채팅방ID
     * @param timestamp 조회 하한선 (이 시각보다 뒤에 생성된 메시지만 대상)
     * @return 가장 최근의 메시지 엔티티
     */
    Optional<ChatMessage> findFirstByChatRoomIdAndTimestampAfterOrderByTimestampDesc(String chatRoomId, LocalDateTime timestamp);


    /**
     * 특정 채팅방의 메시지를 조회 하한선 이후만 페이징하여 조회
     *
     * @param chatRoomId 채팅방ID
     * @param timestamp 조회 하한선 (이 시각보다 뒤에 생성된 메시지만 대상)
     * @param pageable 페이지 번호와 페이지 사이즈
     * @return 페이지 사이즈에 맞는 채팅 메시지들
     */
    Slice<ChatMessage> findByChatRoomIdAndTimestampAfter(String chatRoomId, LocalDateTime timestamp, Pageable pageable);

    /**
     * 특정 채팅방의 메시지를 모두 삭제합니다.
     *
     * 모든 참여자가 나가 채팅방이 되살아날 수 없게 되었을 때 정리 용도로 사용합니다.
     *
     * @param chatRoomId 채팅방ID
     */
    void deleteAllByChatRoomId(String chatRoomId);

    /**
     * 보관기간이 지난 메시지를 오래된 순으로 조회합니다.
     *
     * 한 번에 전부 들고 오면 메모리를 크게 쓰므로, Pageable로 배치 크기만큼 끊어서 가져온다.
     *
     * @param timestamp 보관 기준 시각 (이 시각보다 앞선 메시지가 삭제 대상)
     * @param pageable 배치 크기와 정렬
     * @return 삭제 대상 메시지 목록
     */
    List<ChatMessage> findByTimestampBefore(LocalDateTime timestamp, Pageable pageable);

    /**
     * 주어진 ID 목록에 해당하는 메시지를 한 번의 쿼리로 삭제합니다.
     *
     * @param ids 삭제할 메시지 ID 목록
     */
    void deleteAllByIdIn(List<String> ids);
}
