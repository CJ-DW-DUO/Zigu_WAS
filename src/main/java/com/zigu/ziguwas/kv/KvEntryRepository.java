package com.zigu.ziguwas.kv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface KvEntryRepository extends JpaRepository<KvEntry, String> {

    /**
     * 키가 없으면 넣고 있으면 덮어쓴다.
     *
     * JPA save() 는 SELECT 뒤 INSERT 라 같은 키가 동시에 들어오면(예: 같은 유저의 동시 로그인) 기본키 충돌로 실패할 수 있다.
     * DB 가 PostgreSQL 하나뿐이라 ON CONFLICT 를 그대로 쓴다.
     */
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO kv_entry (id, value, expires_at) VALUES (:key, :value, :expiresAt) "
            + "ON CONFLICT (id) DO UPDATE SET value = EXCLUDED.value, expires_at = EXCLUDED.expires_at", nativeQuery = true)
    void upsert(@Param("key") String key, @Param("value") String value, @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * 만료되지 않은 값만 돌려준다. 만료는 지우는 배치가 아니라 읽을 때 판단하므로 배치 주기와 상관없이 Redis 와 같은 의미가 된다.
     */
    @Query("select e.value from KvEntry e where e.id = :key and e.expiresAt > :now")
    Optional<String> findValue(@Param("key") String key, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("delete from KvEntry e where e.id = :key")
    void deleteByKey(@Param("key") String key);

    @Transactional
    @Modifying
    @Query("delete from KvEntry e where e.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
