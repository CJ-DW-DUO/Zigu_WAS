package com.zigu.ziguwas.kv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 만료 시간이 있는 키-값 저장소. 예전에는 Redis 에 저장했고 지금은 PostgreSQL 의 kv_entry 테이블에 저장한다.
 * (메서드 이름·사용법은 그대로라 호출하는 쪽은 바뀐 게 없다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyValueService {

    private final KvEntryRepository kvEntryRepository;

    // 인증 코드 저장 (이메일을 key로, 인증번호를 value로 저장, 5분간 유효)
    public void setDataExpire(String key, String value, long duration) {
        kvEntryRepository.upsert(key, value, LocalDateTime.now().plus(Duration.ofMillis(duration)));
    }

    // 저장된 인증 코드 가져오기 (만료됐으면 null)
    public String getData(String key) {
        return kvEntryRepository.findValue(key, LocalDateTime.now()).orElse(null);
    }

    // 인증 완료 후 코드 삭제
    public void deleteData(String key) {
        kvEntryRepository.deleteByKey(key);
    }

    /**
     * 만료된 항목을 매시 정각에 지웁니다. 공간을 되찾는 용도일 뿐이고, 만료 판단은 getData 가 읽을 때 한다.
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void deleteExpired() {
        int deleted = kvEntryRepository.deleteExpired(LocalDateTime.now());
        log.info("만료된 키-값 {}건 삭제", deleted);
    }
}
