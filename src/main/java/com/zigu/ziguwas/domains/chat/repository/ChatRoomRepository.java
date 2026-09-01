package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    Optional<ChatRoom> findById(String id);

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
