package com.zigu.ziguwas.domains.notification;

import com.zigu.ziguwas.config.AsyncConfig;
import com.zigu.ziguwas.domains.notification.event.NotificationCreatedEvent;
import com.zigu.ziguwas.domains.notification.listener.NotificationEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 알림 저장이 요청 스레드가 아닌 전용 실행기에서 돌고, 대기열이 차도 예외 없이 버려지는지 확인한다. */
class NotificationAsyncTest {

    @Test
    void 알림_리스너는_전용_실행기에서_비동기로_돈다() throws Exception {
        Async async = NotificationEventListener.class
                .getMethod("handleNotificationCreated", NotificationCreatedEvent.class)
                .getAnnotation(Async.class);

        assertNotNull(async, "요청 스레드에서 알림을 저장하면 연결 풀이 고갈된다 — @Async 필요");
        assertEquals("notificationExecutor", async.value());
    }

    @Test
    void 대기열이_가득_차도_예외_없이_버리고_스레드는_2개뿐() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().notificationExecutor();
        executor.initialize();
        CountDownLatch release = new CountDownLatch(1);
        try {
            Executor e = executor;
            // 스레드 2 + 대기열 200 = 202건까지 받고, 그 뒤 50건은 버려진다(예외 없음)
            for (int i = 0; i < 252; i++) {
                e.execute(() -> {
                    try {
                        release.await();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            assertEquals(2, executor.getPoolSize());
            assertEquals(200, executor.getThreadPoolExecutor().getQueue().size());
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }
}
