package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends MongoRepository<ChatParticipant, String> {

    /**
     * 해당 채팅방에 사람이 있는지 검사하는 JPA 메소드
     *
     * @param chatRoomId 방ID
     * @param userId 사용자ID
     * @return T/F
     */
    boolean existsByChatRoomIdAndUserId(String chatRoomId, Long userId);

    /**
     * 사용자 기반으로 채팅에 참여중인 정보들을 모두 가져오기
     *
     * @param userId 사용자ID
     * @return 참가자 리스트
     */
    List<ChatParticipant> findAllByUserId(Long userId);

    /**
     * 특정 채팅방의 참여자 목록을 조회합니다.
     *
     * 메시지 알림 발송 시, 발신자를 제외한 수신자 계산에 사용됩니다.
     *
     * @param chatRoomId 채팅방
     * @return 해당 채팅방 참여자 리스트
     */
    List<ChatParticipant> findAllByChatRoomId(String chatRoomId);

    // 채팅방에 해당 유저가 있으면 참여자 객체 반환
    Optional<ChatParticipant> findByChatRoomIdAndUserId(String chatRoomId, Long userId);
}
