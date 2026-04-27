package com.zigu.ziguwas.domains.notification.service;

import com.zigu.ziguwas.domains.notification.dto.response.NotificationListResDto;
import com.zigu.ziguwas.domains.notification.entity.Notification;
import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.notification.repository.NotificationRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(NotificationCreatedEvent event) {
        // 1. 수신 사용자 조회
        User receiver = userRepository.findById(event.receiverUserId()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 알림 엔티티 생성
        Notification notification = Notification.create(
                receiver,
                event.type(),
                event.title(),
                event.content()
        );

        // 3. 알림 저장
        notificationRepository.save(notification);
    }

    /**
     * 로그인 사용자의 알림 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return 알림 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<NotificationListResDto> getMyNotifications(Long userId, Pageable pageable) {
        // 1. 사용자 존재 여부 확인
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 최신순 페이지 조회 후 DTO 변환
        return notificationRepository.findAllByUserOrderByRecTimeDesc(user, pageable)
                .map(NotificationListResDto::fromEntity);
    }

    /**
     * 로그인 사용자의 미읽음 알림 개수를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 미읽음 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        // 1. 사용자 존재 여부 확인
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 미읽음 카운트 조회
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * 특정 알림을 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     * @param notificationId 알림 ID
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        // 1. 사용자 존재 여부 확인
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 본인 소유 알림 조회
        Notification notification = notificationRepository.findByNotiIdAndUser(notificationId, user).orElseThrow(
                () -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND)
        );

        // 3. 읽음 상태 업데이트
        notification.markAsRead();
    }
}


