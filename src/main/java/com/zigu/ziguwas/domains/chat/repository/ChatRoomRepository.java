package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 두 사용자가 있는 채팅방이 존재하는지
    @Query("""
            select cr from ChatRoom cr
            where cr.chatId in (select p1.chatRoom.chatId from ChatParticipant p1 where p1.user = :user1)
              and cr.chatId in (select p2.chatRoom.chatId from ChatParticipant p2 where p2.user = :user2)
            """)
    Optional<ChatRoom> findOneByParticipants(@Param("user1") User user1, @Param("user2") User user2);
}
