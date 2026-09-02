package com.zigu.ziguwas.domains.notification.service;

import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.notification.dto.request.NotificationSettingReqDto;
import com.zigu.ziguwas.domains.notification.dto.response.NotificationListResDto;
import com.zigu.ziguwas.domains.notification.dto.response.NotificationSettingResDto;
import com.zigu.ziguwas.domains.notification.entity.Notification;
import com.zigu.ziguwas.domains.notification.entity.NotificationCategory;
import com.zigu.ziguwas.domains.notification.entity.NotificationType;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final PushNotificationService pushNotificationService;

    /**
     * 이벤트 페이로드를 바탕으로 알림을 생성합니다.
     *
     * @param event 알림 생성 이벤트
     */
    public void createNotification(NotificationCreatedEvent event) {

        // 1. 해당 알림을 유저가 켰는지 확인
        User user = userRepository.findById(event.receiverUserId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(!user.getNotificationSetting().isAllowed(event.type().getCategory())) {
            return;
        }

        // 2. 알림 엔티티 생성
        Notification notification = Notification.create(
                event.receiverUserId(),
                event.type(),
                event.title(),
                event.content(),
                event.referenceId(),
                event.itemId(),
                event.itemTitle()
        );

        // 3. 알림 저장
        notificationRepository.save(notification);

        // 4. 저장된 알림을 바탕으로 등록된 기기에 푸시 발송
        pushNotificationService.send(notification);
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
     * 알림 단건을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param notificationId 알림 ID
     * @return 목록 조회와 동일한 형태의 알림 상세 응답
     */
    public NotificationListResDto getNotification(Long userId, String notificationId) {
        // 1. 본인 소유 알림만 조회 (존재하지 않거나 타인 소유면 404)
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND)
        );

        // 2. 목록 응답과 동일한 DTO로 변환
        return NotificationListResDto.fromEntity(notification);
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
        notificationRepository.save(notification);
    }

    /**
     * 특정 채팅방에 대한 사용자의 채팅 알림을 모두 읽음 처리합니다.
     *
     * 사용자가 채팅방에 입장했을 때 호출되며, 앱이 아직 불러오지 않은 알림까지
     * 서버에서 한 번에 읽음 처리하기 위해 사용합니다.
     *
     * @param userId 사용자 ID
     * @param chatRoomId 채팅방 ID
     */
    public void markChatRoomNotificationsAsRead(Long userId, String chatRoomId) {
        // 1. 요청자가 해당 채팅방의 (나가지 않은) 참여자인지 검증
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull(chatRoomId, userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 2. 해당 채팅방의 미읽음 채팅 알림만 조회
        List<Notification> unreadChatNotifications = notificationRepository
                .findAllByUserIdAndNotificationTypeAndReferenceIdAndIsReadFalse(userId, NotificationType.CHAT, chatRoomId);

        // 3. 전부 읽음 처리 후 일괄 저장
        unreadChatNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadChatNotifications);
    }

    /**
     * 로그인 사용자의 모든 미읽음 알림을 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     */
    public void markAllAsRead(Long userId) {
        // 1. 사용자의 미읽음 알림 전체 조회
        List<Notification> unreadNotifications = notificationRepository.findAllByUserIdAndIsReadFalse(userId);

        // 2. 전부 읽음 처리 후 일괄 저장
        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);
    }

    public NotificationSettingResDto getNotificationSettings(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        return NotificationSettingResDto.builder()
                .userId(userId)
                .chatNotiEnabled(user.getNotificationSetting().isAllowed(NotificationCategory.CHAT))
                .tradeNotiEnabled(user.getNotificationSetting().isAllowed(NotificationCategory.TRADE))
                .marketingNotiEnabled(user.getNotificationSetting().isAllowed(NotificationCategory.MARKETING))
                .build();
    }

    /**
     * 사용자 알림 수신 여부를 업데이트합니다.
     * @param userId 사용자 ID
     * @param dto 변경할 알림들
     * @return 변경 사항들
     */
    @Transactional
    public NotificationSettingResDto updateNotificationSettings(Long userId, NotificationSettingReqDto dto) {

        // 1. 사용자 불러오기
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 알림 설정 업데이트
        user.getNotificationSetting().updateNotificationSetting(
                dto.isChatNotiEnabled(),
                dto.isTradeNotiEnabled(),
                dto.isMarketingNotiEnabled()
        );


        // 3. 반영
        userRepository.save(user);

        // 4. 변경사항들 반환
        return NotificationSettingResDto.builder()
                .userId(userId)
                .chatNotiEnabled(user.getNotificationSetting().isAllowed(NotificationCategory.CHAT))
                .tradeNotiEnabled(user.getNotificationSetting().isAllowed(NotificationCategory.TRADE))
                .marketingNotiEnabled(user.getNotificationSetting().isAllowed(NotificationCategory.MARKETING))
                .build();
    }
}


