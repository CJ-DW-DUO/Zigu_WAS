package com.zigu.ziguwas.domains.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

//@Entity
@Document(collection="chat_rooms")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatRoom {

    @Id
    private String id;

    private Long itemId;
}
