package com.zigu.ziguwas.domains.notification.controller;

import com.zigu.ziguwas.domains.notification.api.NotificationApi;
import com.zigu.ziguwas.domains.notification.dto.request.NotificationSettingReqDto;
import com.zigu.ziguwas.domains.notification.dto.response.NotificationUnreadCountResDto;
import com.zigu.ziguwas.domains.notification.service.NotificationService;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

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
}


