package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 두 사용자가 하나의 아이템에 엮여있는 채팅방이 존재하는지
    @Query("""
    select cr from ChatRoom cr
    join ChatParticipant p1 on p1.chatRoom = cr and p1.user = :user1
    join ChatParticipant p2 on p2.chatRoom = cr and p2.user = :user2
    where cr.item = :item
    """)
    Optional<ChatRoom> findByItemAndParticipants(@Param("item") Item item,
                                                 @Param("user1") User user1,
                                                 @Param("user2") User user2);

}
