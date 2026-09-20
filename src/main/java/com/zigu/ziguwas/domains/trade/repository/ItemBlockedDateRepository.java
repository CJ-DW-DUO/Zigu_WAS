package com.zigu.ziguwas.domains.trade.repository;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.trade.entity.ItemBlockedDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ItemBlockedDateRepository extends JpaRepository<ItemBlockedDate, Long> {

    // 아이템의 등록자 지정 차단 날짜 전체 조회 (관리 화면, 캘린더 표시용)
    List<ItemBlockedDate> findAllByItem(Item item);

    boolean existsByItemAndBlockedDate(Item item, LocalDate blockedDate);

    /**
     * 주어진 기간과 겹치는 등록자 지정 차단 날짜가 있는지 확인합니다. (대여 신청/승인 검증용)
     */
    boolean existsByItemAndBlockedDateBetween(Item item, LocalDate startDate, LocalDate endDate);

    void deleteByItemAndBlockedDateIn(Item item, List<LocalDate> blockedDates);
}
