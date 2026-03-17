package com.zigu.ziguwas.domains.chatroom.repository;

import com.zigu.ziguwas.domains.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
}
