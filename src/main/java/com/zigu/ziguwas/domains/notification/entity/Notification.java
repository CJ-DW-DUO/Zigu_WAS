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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    /**
     * 알림 생성 시 사용하는 정적 팩토리 메서드입니다.
     *
     * 생성 시점에 수신 시간(recTime)을 현재 시간으로 기록하고,
     * 읽음 상태는 기본값(false)으로 초기화합니다.
     */
    public static Notification create(
            User user,
            NotificationType notificationType,
            String notiTitle,
            String notiContent
    ) {
        return Notification.builder()
                .user(user)
                .notificationType(notificationType)
                .notiTitle(notiTitle)
                .notiContent(notiContent)
                .recTime(LocalDateTime.now())
                .isRead(false)
                .build();
    }

    /**
     * 알림을 읽음 상태로 변경합니다.
     *
     * 이미 읽은 알림은 중복 갱신하지 않습니다.
     */
    public void markAsRead() {
        if (Boolean.TRUE.equals(this.isRead)) {
            return;
        }
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
