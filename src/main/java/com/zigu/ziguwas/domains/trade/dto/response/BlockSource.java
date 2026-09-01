package com.zigu.ziguwas.domains.trade.dto.response;

/**
 * 대여 불가 기간의 출처를 나타냅니다.
 */
public enum BlockSource {
    RESERVATION, // 승인/진행 중인 거래로 인한 차단
    OWNER        // 등록자가 직접 설정한 차단 (추후 지원)
}
