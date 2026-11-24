package com.zigu.ziguwas.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // 인증 코드 저장 (이메일을 key로, 인증번호를 value로 저장, 5분간 유효)
    public void setDataExpire(String key, String value, long duration) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        Duration expireDuration = Duration.ofMillis(duration);
        valueOperations.set(key, value, expireDuration);
    }

    // 값이 없을 때만 저장 (이미 있으면 false). 같은 키로 동시에 들어온 요청 중 하나만 통과시키는 용도
    public boolean setDataIfAbsent(String key, String value, long duration) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofMillis(duration)));
    }

    // 저장된 인증 코드 가져오기
    public String getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 인증 완료 후 코드 삭제
    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

}
