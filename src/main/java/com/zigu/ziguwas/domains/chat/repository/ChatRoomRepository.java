package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    Optional<ChatRoom> findById(String id);
}
