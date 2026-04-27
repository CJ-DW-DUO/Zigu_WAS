package com.zigu.ziguwas.domains.notification.entity;

import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noti_id")
    private Long notiId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "noti_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "noti_title", nullable = false)
    private String notiTitle;

    @Column(name = "noti_content", nullable = false)
    private String notiContent;

    @Column(name = "rec_time", nullable = false)
    private LocalDateTime recTime;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
