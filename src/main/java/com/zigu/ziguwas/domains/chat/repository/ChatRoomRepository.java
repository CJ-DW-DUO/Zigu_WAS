package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
}
