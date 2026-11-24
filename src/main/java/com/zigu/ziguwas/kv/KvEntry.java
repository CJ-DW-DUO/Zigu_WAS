package com.zigu.ziguwas.kv;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 만료 시각이 있는 키-값 한 건. 이메일 인증코드·refresh 토큰·로그아웃한 access 토큰(블랙리스트)을 담는다. (예전 Redis 가 하던 일)
 *
 * 쓰기는 KvEntryRepository 의 upsert 로만 하므로 이 엔티티는 읽기 전용 매핑이다.
 * 일부러 UNLOGGED 테이블로 만들지 않았다 — DB 가 비정상 종료되면 내용이 비워져, 로그아웃한 토큰이 만료 전까지 다시 통하게 된다.
 */
@Entity
@Table(name = "kv_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class KvEntry {

    // 블랙리스트 키가 JWT 전문(약 200~300자)이라 varchar(255) 로는 모자랄 수 있다
    @Id
    @Column(columnDefinition = "text")
    private String id;

    // refresh 토큰 전문이 들어간다
    @Column(columnDefinition = "text", nullable = false)
    private String value;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
