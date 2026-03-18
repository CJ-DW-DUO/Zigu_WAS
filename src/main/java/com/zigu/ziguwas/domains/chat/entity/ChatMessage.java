package com.zigu.ziguwas.domains.chat.entity;

import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne
    @JoinColumn(name = "chat_id" , nullable = false)
    private ChatRoom chatRoom; // 채팅방 ID

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // 작성자ID

    @Column
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;
    private String imageUrl;

}
