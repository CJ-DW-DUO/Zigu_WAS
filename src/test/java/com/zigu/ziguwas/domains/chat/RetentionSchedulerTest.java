package com.zigu.ziguwas.domains.chat;

import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.chat.repository.ChatMessageRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.chat.scheduler.ChatRoomRetentionScheduler;
import com.zigu.ziguwas.domains.notification.entity.Notification;
import com.zigu.ziguwas.domains.notification.entity.NotificationType;
import com.zigu.ziguwas.domains.notification.repository.NotificationRepository;
import com.zigu.ziguwas.domains.notification.scheduler.NotificationRetentionScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 보관 배치 검증 — 실제 PostgreSQL 에 붙어(env 가드) 91일 지난 것만 지워지는지 확인한다. (테스트마다 롤백)
 * 실행: ZIGU_VERIFY_STEP=retention ./gradlew test --tests '*RetentionSchedulerTest'
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "ZIGU_VERIFY_STEP", matches = "retention")
@Import({ChatRoomRetentionScheduler.class, NotificationRetentionScheduler.class})
class RetentionSchedulerTest {

    @Autowired TestEntityManager em;
    @Autowired ChatRoomRepository rooms;
    @Autowired ChatMessageRepository messages;
    @Autowired ChatParticipantRepository participants;
    @Autowired NotificationRepository notifications;
    @Autowired ChatRoomRetentionScheduler chatScheduler;
    @Autowired NotificationRetentionScheduler notificationScheduler;

    private ChatRoom roomWithMessage(LocalDateTime closedAt) {
        ChatRoom room = rooms.save(ChatRoom.builder().itemId(1L).closedAt(closedAt).build());
        messages.save(ChatMessage.builder().chatRoomId(room.getId()).senderId(1L).message("hi").timestamp(LocalDateTime.now()).build());
        participants.save(ChatParticipant.builder().chatRoomId(room.getId()).userId(1L).build());
        return room;
    }

    @Test
    void 폐쇄된지_91일_지난_방은_메시지_참여자와_함께_삭제되고_나머지는_남는다() {
        ChatRoom expired = roomWithMessage(LocalDateTime.now().minusDays(91));
        ChatRoom recent = roomWithMessage(LocalDateTime.now().minusDays(10));
        ChatRoom open = roomWithMessage(null);
        em.flush();
        em.clear();

        chatScheduler.deleteExpiredChatRooms();
        em.flush();
        em.clear();

        assertTrue(rooms.findById(expired.getId()).isEmpty());
        assertEquals(0, messages.countByChatRoomId(expired.getId()));
        assertTrue(participants.findAllByChatRoomId(expired.getId()).isEmpty());
        assertTrue(rooms.findById(recent.getId()).isPresent());
        assertEquals(1, messages.countByChatRoomId(recent.getId()));
        assertTrue(rooms.findById(open.getId()).isPresent());
        assertEquals(1, participants.findAllByChatRoomId(open.getId()).size());
    }

    @Test
    void 수신_91일_지난_알림만_삭제된다() {
        Notification old = notificationAt(LocalDateTime.now().minusDays(91));
        Notification recent = notificationAt(LocalDateTime.now().minusDays(10));
        em.flush();
        em.clear();

        notificationScheduler.deleteExpiredNotifications();
        em.flush();
        em.clear();

        assertTrue(notifications.findById(old.getId()).isEmpty());
        assertTrue(notifications.findById(recent.getId()).isPresent());
    }

    private Notification notificationAt(LocalDateTime recTime) {
        return notifications.save(Notification.builder().userId(1L).notificationType(NotificationType.CHAT)
                .notiTitle("t").notiContent("c").recTime(recTime).isRead(false).build());
    }
}
