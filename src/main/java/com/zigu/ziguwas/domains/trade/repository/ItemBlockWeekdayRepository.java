package com.zigu.ziguwas.domains.trade.repository;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.trade.entity.ItemBlockWeekday;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface ItemBlockWeekdayRepository extends JpaRepository<ItemBlockWeekday, Long> {

    // 아이템에 등록된 반복 차단 요일 전체 조회 (관리 화면, 겹침 검증, 캘린더 표시용)
    List<ItemBlockWeekday> findAllByItem(Item item);

    boolean existsByItemAndDayOfWeek(Item item, DayOfWeek dayOfWeek);

    void deleteByItemAndDayOfWeek(Item item, DayOfWeek dayOfWeek);
}
