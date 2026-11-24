package com.zigu.ziguwas.config;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * "오늘" 날짜 기준을 한국 시간(KST)으로 고정한다.
 * JVM 시간대는 UTC 로 유지한다(FE 가 서버 시각을 UTC 로 해석) — 날짜(LocalDate) 판단에만 이 클래스를 쓴다.
 */
public final class Kst {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private Kst() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
