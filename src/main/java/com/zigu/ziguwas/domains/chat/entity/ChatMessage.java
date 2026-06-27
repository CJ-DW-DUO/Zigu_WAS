package com.zigu.ziguwas.domains.chat.entity;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//@Entity
@Document("chat_messages")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatMessage {

    @Id
    private String id;

    // RDB와 NoSQL PK 타입 차이로 인하여 String
    private String chatRoomId;

    // RDB와 NoSQL PK 타입 차이로 인하여 Long
    private Long senderId;

    private String message;

    private LocalDateTime timestamp;

    private String imageUrl;

}
