package com.zigu.ziguwas.domains.trade.scheduler;

import com.zigu.ziguwas.domains.notification.entity.NotificationType;
import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 반납 기한이 지났는데 아직 반납 처리되지 않은 거래를 매일 찾아
 * 임대인/임차인 양쪽에게 반납기한 초과 알림을 발송합니다.
 *
 * 상태(TradeStatus)를 늘리는 방식이 아니라, IN_PROGRESS 상태이면서
 * 반납예정일(tradeEndate)이 지난 거래를 매일 다시 조회하는 방식이라
 * 실제로 반납 처리(RETURNED)되기 전까지 매일 반복 발송됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeReturnReminderScheduler {

    private final TradeRepository tradeRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 매일 오전 9시(KST)에 실행됩니다.
     *
     * 읽기 전용 트랜잭션으로 감싸는 이유: Trade의 item/renter/rentee가
     * 지연로딩(LAZY)이라, 트랜잭션 밖에서 접근하면 LazyInitializationException이 발생합니다.
     */
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void remindOverdueReturns() {
        // 1. 대여중(IN_PROGRESS)인데 반납예정일이 이미 지난 거래를 조회
        List<Trade> overdueTrades = tradeRepository.findAllByTradeStatusAndTradeEndateBefore(
                TradeStatus.IN_PROGRESS,
                LocalDate.now()
        );

        // 2. 대상 거래마다 임대인/임차인 각각에게 알림 이벤트 발행
        for (Trade trade : overdueTrades) {
            notifyRenter(trade);
            notifyRentee(trade);
        }

        log.info("반납기한 초과 알림 발송 대상 거래 {}건 처리 완료", overdueTrades.size());
    }

    /**
     * 임대인(빌려준 사람)에게 반납 확인을 요청하는 알림을 발행합니다.
     */
    private void notifyRenter(Trade trade) {
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                trade.getRenter().getId(),
                NotificationType.RETURN_OVERDUE_RENTER,
                "반납 확인 필요",
                trade.getItem().getTitle() + "을(를) 받았나요? 반납을 확인해 주세요.",
                trade.getId().toString(),
                trade.getItem().getId()
        ));
    }

    /**
     * 임차인(빌린 사람)에게 반납을 독촉하는 알림을 발행합니다.
     */
    private void notifyRentee(Trade trade) {
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                trade.getRentee().getId(),
                NotificationType.RETURN_OVERDUE_RENTEE,
                "반납 기한 초과",
                trade.getItem().getTitle() + " 반납 기간이 지났어요. 반납해 주세요.",
                trade.getId().toString(),
                trade.getItem().getId()
        ));
    }
}
