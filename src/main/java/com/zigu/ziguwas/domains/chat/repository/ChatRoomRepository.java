package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    Optional<ChatRoom> findById(String id);

    /**
     * 같은 물품에 대해 이 요청자가 만든 채팅방을 조회합니다. (나간 방도 포함 — 다시 들어올 때 같은 방을 쓰기 위해)
     *
     * @param itemId 물품 ID
     * @param requesterId 채팅을 건 사용자 ID (물품 주인이 아닌 쪽)
     * @return 있으면 그 채팅방 (유니크 키가 있어 최대 1개)
     */
    Optional<ChatRoom> findByItemIdAndRequesterId(Long itemId, Long requesterId);

    /**
     * 내가 나가지 않은 채팅방 중 주어진 물품들에 걸린 방과 그 방의 상대방을 한 번에 조회합니다. (거래 응답의 chatRoomId 용)
     *
     * 각 행은 [채팅방 ID(String), 물품 ID(Long), 상대방 userId(Long)] 이다.
     */
    @Query("select r.id, r.itemId, other.userId from ChatRoom r, ChatParticipant me, ChatParticipant other"
            + " where me.chatRoomId = r.id and other.chatRoomId = r.id"
            + " and me.userId = :userId and me.leftAt is null and other.userId <> :userId"
            + " and r.itemId in :itemIds")
    List<Object[]> findMyRoomsWithCounterparts(@Param("userId") Long userId, @Param("itemIds") Collection<Long> itemIds);

    /**
     * 폐쇄된 지 보관기간이 지난 채팅방을 오래 폐쇄된 순으로 조회합니다.
     *
     * 한 번에 전부 들고 오면 메모리를 크게 쓰므로, Pageable로 배치 크기만큼 끊어서 가져온다.
     *
     * @param closedAt 보관 기준 시각 (이 시각보다 앞서 폐쇄된 채팅방이 삭제 대상)
     * @param pageable 배치 크기와 정렬
     * @return 삭제 대상 채팅방 목록
     */
    List<ChatRoom> findByClosedAtBefore(LocalDateTime closedAt, Pageable pageable);
}
