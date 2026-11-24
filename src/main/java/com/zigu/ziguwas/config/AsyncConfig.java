package com.zigu.ziguwas.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 알림 저장·푸시 발송 전용 비동기 실행기.
 *
 * 알림은 요청 스레드가 아니라 이 스레드들이 저장한다. 요청 스레드는 거래·채팅 트랜잭션이 끝난 뒤에도 DB 연결을
 * 쥐고 있는데(OSIV), 그 상태에서 알림 저장용 연결을 하나 더 기다리면 동시 요청이 풀 크기 근처일 때
 * 서로를 기다리며 전부 멈추고 알림도 저장되지 않는다. 별도 스레드로 빼면 요청은 두 번째 연결을 기다리지 않는다.
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    // ponytail: 스레드 2개·대기열 200 의 작은 실행기. 서버가 죽으면 대기 중인 알림은 사라지고(저장 전), 대기열이 차면 새 알림을 버리고
    //           로그만 남긴다. 유실을 허용할 수 없게 되면 알림을 DB 에 먼저 적는 아웃박스 테이블 + 배치로 바꾼다.
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notification-");
        // 꽉 차면 요청 스레드에서 돌리지(CallerRuns) 않고 버린다 — 그러면 원래 문제(요청 스레드가 연결을 더 기다림)가 되살아난다.
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("알림 대기열이 가득 차 알림 1건을 버렸습니다 (대기 {}건)", pool.getQueue().size()));
        // 종료할 때 이미 대기 중인 알림은 잠시 기다려 저장한다
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
