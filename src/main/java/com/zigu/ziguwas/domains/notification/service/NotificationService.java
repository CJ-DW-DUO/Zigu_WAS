package com.zigu.ziguwas.domains.notification.service;

import com.zigu.ziguwas.domains.notification.dto.response.NotificationListResDto;
import com.zigu.ziguwas.domains.notification.entity.Notification;
import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.notification.repository.NotificationRepository;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 이벤트 페이로드를 바탕으로 알림을 생성합니다.
     *
     * @param event 알림 생성 이벤트
     */
    public void createNotification(NotificationCreatedEvent event) {
        // 1. 알림 엔티티 생성
        Notification notification = Notification.create(
                event.receiverUserId(),
                event.type(),
                event.title(),
                event.content()
        );

        // 2. 알림 저장
        notificationRepository.save(notification);
    }

    /**
     * 로그인 사용자의 알림 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 알림 목록 페이지
     */
    public Page<NotificationListResDto> getMyNotifications(Long userId, Pageable pageable) {
        // 최신순 페이지 조회 후 DTO 변환
        return notificationRepository.findAllByUserIdOrderByRecTimeDesc(userId, pageable)
                .map(NotificationListResDto::fromEntity);
    }

    /**
     * 로그인 사용자의 미읽음 알림 개수를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 미읽음 알림 개수
     */
    public long getUnreadCount(Long userId) {
        // 미읽음 카운트 조회
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 특정 알림을 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     * @param notificationId 알림 ID
     */
    public void markAsRead(Long userId, String notificationId) {
        // 1. 본인 소유 알림 조회
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND)
        );

        // 2. 읽음 상태 업데이트
        notification.markAsRead();
    }
}


