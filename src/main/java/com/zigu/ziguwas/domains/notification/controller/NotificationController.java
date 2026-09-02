package com.zigu.ziguwas.domains.notification.controller;

import com.zigu.ziguwas.domains.notification.api.NotificationApi;
import com.zigu.ziguwas.domains.notification.dto.request.NotificationSettingReqDto;
import com.zigu.ziguwas.domains.notification.dto.request.PushTokenDeleteReqDto;
import com.zigu.ziguwas.domains.notification.dto.request.PushTokenRegisterReqDto;
import com.zigu.ziguwas.domains.notification.dto.response.NotificationUnreadCountResDto;
import com.zigu.ziguwas.domains.notification.service.NotificationService;
import com.zigu.ziguwas.domains.notification.service.PushTokenService;
import com.zigu.ziguwas.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;
    private final PushTokenService pushTokenService;

    /**
     * 내 알림 목록을 페이지 단위로 조회합니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @param pageable 페이지 정보
     * @return 알림 목록 페이지
     */
    @GetMapping
    public ResponseEntity<?> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Pageable pageable
    ) {
        // 1. 로그인 사용자 기준으로 알림 목록을 조회
        return ResponseEntity.ok(
                notificationService.getMyNotifications(customUserDetails.getUserId(), pageable)
        );
    }

    /**
     * 알림 단건을 조회합니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @param notificationId 조회할 알림 ID
     * @return 알림 상세 정보
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<?> getNotification(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable String notificationId
    ) {
        // 1. 본인 소유 알림만 조회하여 목록과 동일한 형태로 반환
        return ResponseEntity.ok(
                notificationService.getNotification(customUserDetails.getUserId(), notificationId)
        );
    }

    /**
     * 내 미읽음 알림 개수를 조회합니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @return 미읽음 알림 개수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        // 1. 사용자 기준 미읽음 개수 조회
        long unreadCount = notificationService.getUnreadCount(customUserDetails.getUserId());
        // 2. 응답 DTO로 감싸서 반환
        return ResponseEntity.ok(new NotificationUnreadCountResDto(unreadCount));
    }

    /**
     * 특정 알림을 읽음 처리합니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @param notificationId 읽음 처리할 알림 ID
     * @return 처리 성공 응답
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable String notificationId
    ) {
        // 1. 본인 소유 알림인지 검증 후 읽음 처리
        notificationService.markAsRead(customUserDetails.getUserId(), notificationId);
        // 2. 본문 없는 성공 응답
        return ResponseEntity.ok().build();
    }


    /**
     * 특정 채팅방에 대한 채팅 알림을 전부 읽음 처리합니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @param chatRoomId 채팅방 ID
     * @return 처리 성공 응답
     */
    @PatchMapping("/chat-rooms/{chatRoomId}/read")
    public ResponseEntity<?> markChatRoomNotificationsAsRead(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable String chatRoomId
    ) {
        // 1. 해당 채팅방의 참여자인지 검증 후, 채팅 알림 전체 읽음 처리
        notificationService.markChatRoomNotificationsAsRead(customUserDetails.getUserId(), chatRoomId);
        // 2. 본문 없는 성공 응답
        return ResponseEntity.ok().build();
    }

    /**
     * 내 모든 알림을 읽음 처리합니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @return 처리 성공 응답
     */
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        // 1. 사용자의 미읽음 알림 전체 읽음 처리
        notificationService.markAllAsRead(customUserDetails.getUserId());
        // 2. 본문 없는 성공 응답
        return ResponseEntity.ok().build();
    }


    /**
     * 사용자 알림 수신 여부를 가져옵니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @return 사용자 알림 수신 설정 정보
     */
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        return ResponseEntity.ok(notificationService.getNotificationSettings(customUserDetails.getUserId()));
    }


    /**
     * 사용자 알림 수신 여부를 업데이트합니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @return 처리 성공 응답
     */
    @PatchMapping("/settings")
    public ResponseEntity<?> updateSettings(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody NotificationSettingReqDto dto
    ){
        return ResponseEntity.ok(notificationService.updateNotificationSettings(customUserDetails.getUserId(), dto));
    }

    /**
     * 푸시 토큰을 등록합니다. 동일 토큰이 이미 있으면 갱신됩니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @param dto 등록할 푸시 토큰 정보
     * @return 등록 성공 응답
     */
    @PostMapping("/push-tokens")
    public ResponseEntity<?> registerPushToken(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody PushTokenRegisterReqDto dto
    ) {
        pushTokenService.register(customUserDetails.getUserId(), dto);
        return ResponseEntity.ok().build();
    }

    /**
     * 푸시 토큰을 삭제합니다. 로그아웃하거나 토큰이 더 이상 유효하지 않을 때 호출합니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @param dto 삭제할 푸시 토큰 정보
     * @return 삭제 성공 응답
     */
    @DeleteMapping("/push-tokens")
    public ResponseEntity<?> deletePushToken(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody PushTokenDeleteReqDto dto
    ) {
        pushTokenService.delete(customUserDetails.getUserId(), dto.getToken());
        return ResponseEntity.ok().build();
    }
}


