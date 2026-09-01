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
     * 보관기간이 지난 채팅방을 정리할 때 사용합니다.
     *
     * @param chatRoomId 채팅방ID
     */
    void deleteAllByChatRoomId(String chatRoomId);

    /**
     * 특정 채팅방에서 이미지가 첨부된 메시지만 조회합니다.
     *
     * 채팅방을 삭제하기 전에 S3에 올라간 이미지를 먼저 지워야 하므로,
     * 텍스트 메시지까지 전부 들고 오지 않도록 이미지 메시지만 골라낸다.
     *
     * @param chatRoomId 채팅방ID
     * @return 이미지가 첨부된 메시지 목록
     */
    List<ChatMessage> findByChatRoomIdAndImageUrlNotNull(String chatRoomId);

    /**
     * 특정 채팅방의 메시지 개수를 조회합니다.
     *
     * 삭제 배치가 실제로 몇 건을 정리했는지 로그로 남기기 위해 사용합니다.
     *
     * @param chatRoomId 채팅방ID
     * @return 메시지 개수
     */
    long countByChatRoomId(String chatRoomId);
}
