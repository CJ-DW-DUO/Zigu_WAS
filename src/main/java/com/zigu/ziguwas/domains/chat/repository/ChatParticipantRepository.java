package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends MongoRepository<ChatParticipant, String> {

    /**
     * 해당 채팅방에 나가지 않은 사람이 있는지 검사하는 JPA 메소드
     *
     * 나간 사용자(leftAt != null)는 해당 채팅방이 숨겨진 상태이므로 참여자로 취급하지 않는다.
     *
     * @param chatRoomId 방ID
     * @param userId 사용자ID
     * @return T/F
     */
    boolean existsByChatRoomIdAndUserIdAndLeftAtIsNull(String chatRoomId, Long userId);

    /**
     * 사용자 기반으로 채팅에 참여중인 정보들을 모두 가져오기
     *
     * 나간 채팅방까지 포함하므로, 채팅방 목록 조회가 아니라
     * 기존 채팅방 재사용 여부를 판단할 때 사용한다.
     *
     * @param userId 사용자ID
     * @return 참가자 리스트
     */
    List<ChatParticipant> findAllByUserId(Long userId);

    /**
     * 사용자가 나가지 않은(목록에 보이는) 채팅방 참여 정보만 가져오기
     *
     * @param userId 사용자ID
     * @return 참가자 리스트
     */
    List<ChatParticipant> findAllByUserIdAndLeftAtIsNull(Long userId);

    /**
     * 특정 채팅방의 참여자 목록을 조회합니다.
     *
     * 메시지 알림 발송 시, 발신자를 제외한 수신자 계산에 사용됩니다.
     * 나간 참여자도 포함되며, 이들은 새 메시지 수신 시 다시 활성화됩니다.
     *
     * @param chatRoomId 채팅방
     * @return 해당 채팅방 참여자 리스트
     */
    List<ChatParticipant> findAllByChatRoomId(String chatRoomId);

    // 채팅방에 해당 유저가 있으면 참여자 객체 반환 (나간 상태여도 반환)
    Optional<ChatParticipant> findByChatRoomIdAndUserId(String chatRoomId, Long userId);

    /**
     * 특정 채팅방의 참여 정보를 모두 삭제합니다.
     *
     * 보관기간이 지난 채팅방을 정리할 때 사용합니다.
     *
     * @param chatRoomId 채팅방ID
     */
    void deleteAllByChatRoomId(String chatRoomId);
}
