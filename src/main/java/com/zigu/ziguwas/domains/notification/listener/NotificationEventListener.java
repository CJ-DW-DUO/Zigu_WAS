package com.zigu.ziguwas.domains.notification.listener;

import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * 알림 생성 이벤트를 수신하여 알림 저장을 위임합니다.
     *
     * 트랜잭션 커밋 이후에만 실행되어,
     * 본 비즈니스 로직 롤백 시 유령 알림이 남지 않도록 합니다.
     *
     * @param event 알림 생성 이벤트
     */
    @Async // 비동기 처리를 통해 알림 생성이 메인 트랜잭션에 영향을 주지 않도록 합니다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        // 1. 실제 알림 저장 로직은 NotificationService에 위임
        notificationService.createNotification(event);
    }
}


