package com.zigu.ziguwas.domains.notification.listener;

import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
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
     * fallbackExecution 활성화로 트랜잭션이 있는 경우 커밋 대개, 완료 후 리스너가 실행되어 (거래가)롤백되면 알림이 생기지 않습니다.
     * fallbackExecution 활성화로 트랜잭션이 없는 경우 즉시 리스너를 실행하여 (채팅이)드롭되지 않습니다.
     * @param event 알림 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        // 1. 실제 알림 저장 로직은 NotificationService에 위임
        notificationService.createNotification(event);
    }
}


