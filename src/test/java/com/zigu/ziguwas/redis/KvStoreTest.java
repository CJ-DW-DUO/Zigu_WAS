package com.zigu.ziguwas.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 키-값 저장소 검증 — 실제 PostgreSQL 에 붙어(env 가드) Redis 와 같은 의미로 동작하는지 확인한다. (테스트마다 롤백)
 * 실행: ZIGU_VERIFY_STEP=kv ./gradlew test --tests '*KvStoreTest'
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "ZIGU_VERIFY_STEP", matches = "kv")
@Import(RedisService.class)
class KvStoreTest {

    @Autowired RedisService store;
    @Autowired TestEntityManager em;

    @Test
    void 만료_전에는_값을_만료_후에는_null을_준다() throws Exception {
        store.setDataExpire("k-live", "v", 60_000);
        store.setDataExpire("k-short", "v", 50);
        Thread.sleep(120);

        assertEquals("v", store.getData("k-live"));
        assertNull(store.getData("k-short"));
        assertNull(store.getData("없는키"));
    }

    @Test
    void 같은_키를_다시_쓰면_덮어쓰고_만료도_새로_잡는다() throws Exception {
        store.setDataExpire("k", "first", 50);
        store.setDataExpire("k", "second", 60_000);
        Thread.sleep(120);

        assertEquals("second", store.getData("k"));
    }

    @Test
    void 삭제하면_사라지고_없는_키_삭제도_조용히_넘어간다() {
        store.setDataExpire("k", "v", 60_000);
        store.deleteData("k");
        store.deleteData("k");

        assertNull(store.getData("k"));
    }

    @Test
    void 긴_키와_긴_값도_저장된다_JWT_크기() {
        String jwtLike = "BLACKLIST:" + "x".repeat(600);
        String refresh = "r".repeat(900);
        store.setDataExpire(jwtLike, refresh, 60_000);

        assertEquals(refresh, store.getData(jwtLike));
    }

    @Test
    void 정리_배치는_만료된_항목만_지운다() throws Exception {
        store.setDataExpire("old", "v", 20);
        store.setDataExpire("fresh", "v", 60_000);
        Thread.sleep(80);

        store.deleteExpired();
        em.clear();

        // getData 는 만료를 읽을 때 걸러내므로, 실제로 행이 지워졌는지는 테이블을 직접 센다
        assertEquals(0L, em.getEntityManager().createQuery("select count(e) from KvEntry e where e.id = 'old'", Long.class).getSingleResult());
        assertEquals("v", store.getData("fresh"));
    }
}
