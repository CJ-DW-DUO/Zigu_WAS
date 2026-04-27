package com.zigu.ziguwas.domains.notification.controller;

import com.zigu.ziguwas.domains.notification.dto.response.NotificationListResDto;
import com.zigu.ziguwas.domains.notification.dto.response.NotificationUnreadCountResDto;
import com.zigu.ziguwas.domains.notification.service.NotificationService;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 내 알림 목록을 페이지 단위로 조회합니다.
     *
     * @param customUserDetails 인증 사용자 정보
     * @param pageable 페이지 정보
     * @return 알림 목록 페이지
     */
    @GetMapping
    public ResponseEntity<Page<NotificationListResDto>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Pageable pageable
    ) {
        // 1. 로그인 사용자 기준으로 알림 목록을 조회
        return ResponseEntity.ok(
                notificationService.getMyNotifications(customUserDetails.getUserId(), pageable)
        );
    }

}


