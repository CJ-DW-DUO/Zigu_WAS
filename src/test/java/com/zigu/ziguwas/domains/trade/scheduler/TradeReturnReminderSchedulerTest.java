package com.zigu.ziguwas.domains.trade.scheduler;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.notification.entity.NotificationType;
import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 반납 알림: 반납일 당일부터 보내고, 당일과 기한 초과 문구를 나눈다. 시각을 기다리지 않도록 기준일을 직접 넘겨 시험한다. */
class TradeReturnReminderSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final TradeReturnReminderScheduler scheduler = new TradeReturnReminderScheduler(tradeRepository, publisher);

    private Trade trade(long id, LocalDate endDate, long renterId, long renteeId) {
        User renter = mock(User.class);
        when(renter.getId()).thenReturn(renterId);
        User rentee = mock(User.class);
        when(rentee.getId()).thenReturn(renteeId);
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(100L + id);
        when(item.getTitle()).thenReturn("카메라" + id);
        Trade trade = mock(Trade.class);
        when(trade.getId()).thenReturn(id);
        when(trade.getTradeEndate()).thenReturn(endDate);
        when(trade.getRenter()).thenReturn(renter);
        when(trade.getRentee()).thenReturn(rentee);
        when(trade.getItem()).thenReturn(item);
        return trade;
    }

    private List<NotificationCreatedEvent> run(Trade... trades) {
        when(tradeRepository.findAllByTradeStatusAndTradeEndateLessThanEqual(TradeStatus.IN_PROGRESS, TODAY))
                .thenReturn(List.of(trades));
        scheduler.remindReturns(TODAY);
        ArgumentCaptor<NotificationCreatedEvent> captor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(publisher, atLeast(0)).publishEvent(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void 반납일이_오늘이면_임대인과_임차인에게_당일_알림() {
        List<NotificationCreatedEvent> events = run(trade(1, TODAY, 10, 20));

        assertEquals(2, events.size());
        NotificationCreatedEvent renter = events.get(0);
        assertEquals(10L, renter.receiverUserId());
        assertEquals(NotificationType.RETURN_OVERDUE_RENTER, renter.type());
        assertEquals("카메라1을(를) 받았나요? 반납을 확인해 주세요.", renter.content());

        NotificationCreatedEvent rentee = events.get(1);
        assertEquals(20L, rentee.receiverUserId());
        assertEquals(NotificationType.RETURN_OVERDUE_RENTEE, rentee.type());
        assertEquals("오늘이 반납일이에요", rentee.title());
        assertEquals("카메라1 반납하는 날이에요. 물건을 반납해 주세요.", rentee.content());
    }

    @Test
    void 반납일이_어제면_기한_초과_문구() {
        List<NotificationCreatedEvent> events = run(trade(2, TODAY.minusDays(1), 10, 20));

        assertEquals(2, events.size());
        NotificationCreatedEvent rentee = events.get(1);
        assertEquals(20L, rentee.receiverUserId());
        assertEquals("반납 기한 초과", rentee.title());
        assertEquals("카메라2 반납 기간이 지났어요. 반납해 주세요.", rentee.content());
    }

    @Test
    void 반납일이_내일이면_알림_없음() {
        List<NotificationCreatedEvent> events = run(trade(3, TODAY.plusDays(1), 10, 20));

        assertTrue(events.isEmpty());
        verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void 어제_오늘_내일이_섞여_있으면_내일만_빠진다() {
        List<NotificationCreatedEvent> events = run(
                trade(1, TODAY.minusDays(1), 10, 20),
                trade(2, TODAY, 11, 21),
                trade(3, TODAY.plusDays(1), 12, 22));

        assertEquals(4, events.size());
        assertEquals(List.of(10L, 20L, 11L, 21L), events.stream().map(NotificationCreatedEvent::receiverUserId).toList());
        assertEquals(List.of("반납 확인 필요", "반납 기한 초과", "반납 확인 필요", "오늘이 반납일이에요"),
                events.stream().map(NotificationCreatedEvent::title).toList());
    }

    @Test
    void 실행_시각은_매일_오전_9시_KST_그대로() throws Exception {
        Scheduled scheduled = TradeReturnReminderScheduler.class.getMethod("remindOverdueReturns").getAnnotation(Scheduled.class);
        assertEquals("0 0 9 * * *", scheduled.cron());
        assertEquals("Asia/Seoul", scheduled.zone());
    }
}
