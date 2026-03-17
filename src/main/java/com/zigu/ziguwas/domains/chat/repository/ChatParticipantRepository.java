package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    /**
     * 해당 채팅방에 사람이 있는지 검사하는 JPA 메소드
     *
     * @param room 방
     * @param user 사용자
     * @return T/F
     */
    boolean existsByChatRoomAndUser(ChatRoom room, User user);
}
