package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.trade.dto.response.BlockSource;
import com.zigu.ziguwas.domains.trade.dto.response.ItemBlockRangeResDto;
import com.zigu.ziguwas.domains.trade.dto.response.ItemBlockWeekdayListResDto;
import com.zigu.ziguwas.domains.trade.dto.response.ItemBlockedDateListResDto;
import com.zigu.ziguwas.domains.trade.entity.ItemBlockWeekday;
import com.zigu.ziguwas.domains.trade.entity.ItemBlockedDate;
import com.zigu.ziguwas.domains.trade.repository.ItemBlockWeekdayRepository;
import com.zigu.ziguwas.domains.trade.repository.ItemBlockedDateRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 등록자가 직접 설정하는 아이템 대여 불가 차단(특정 날짜 / 반복 요일)을 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class ItemBlockService {

    private final ItemRepository itemRepository;
    private final ItemBlockedDateRepository itemBlockedDateRepository;
    private final ItemBlockWeekdayRepository itemBlockWeekdayRepository;

    private Item getOwnedItem(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        return item;
    }

    /**
     * 특정 날짜(들)를 대여 불가로 차단합니다.
     * 이미 차단된 날짜는 조용히 건너뛰어 중복 등록되지 않도록 합니다.
     */
    @Transactional
    public void addBlockedDates(Long itemId, List<LocalDate> dates, Long userId) {
        Item item = getOwnedItem(itemId, userId);

        LocalDate today = LocalDate.now();
        if (dates.stream().anyMatch(date -> date.isBefore(today))) {
            throw new CustomException(ErrorCode.PAST_DATE_NOT_BLOCKABLE);
        }

        List<ItemBlockedDate> toSave = dates.stream()
                .distinct()
                .filter(date -> !itemBlockedDateRepository.existsByItemAndBlockedDate(item, date))
                .map(date -> ItemBlockedDate.builder()
                        .item(item)
                        .blockedDate(date)
                        .build())
                .toList();

        itemBlockedDateRepository.saveAll(toSave);
    }

    /**
     * 차단해둔 특정 날짜(들)를 해제합니다.
     */
    @Transactional
    public void removeBlockedDates(Long itemId, List<LocalDate> dates, Long userId) {
        Item item = getOwnedItem(itemId, userId);
        itemBlockedDateRepository.deleteByItemAndBlockedDateIn(item, dates);
    }

    /**
     * 등록자 본인이 설정한 차단 날짜 목록을 조회합니다. (등록/수정 화면용)
     */
    @Transactional(readOnly = true)
    public ItemBlockedDateListResDto getBlockedDates(Long itemId, Long userId) {
        Item item = getOwnedItem(itemId, userId);

        List<LocalDate> blockedDates = itemBlockedDateRepository.findAllByItem(item).stream()
                .map(ItemBlockedDate::getBlockedDate)
                .sorted()
                .toList();

        return ItemBlockedDateListResDto.builder()
                .blockedDates(blockedDates)
                .build();
    }

    /**
     * 매주 반복되는 특정 요일(들)을 대여 불가로 차단합니다.
     * 이미 차단된 요일은 조용히 건너뛰어 중복 등록되지 않도록 합니다.
     */
    @Transactional
    public void addBlockedWeekdays(Long itemId, List<DayOfWeek> daysOfWeek, Long userId) {
        Item item = getOwnedItem(itemId, userId);

        List<ItemBlockWeekday> toSave = daysOfWeek.stream()
                .distinct()
                .filter(day -> !itemBlockWeekdayRepository.existsByItemAndDayOfWeek(item, day))
                .map(day -> ItemBlockWeekday.builder()
                        .item(item)
                        .dayOfWeek(day)
                        .build())
                .toList();

        itemBlockWeekdayRepository.saveAll(toSave);
    }

    /**
     * 차단해둔 반복 요일을 해제합니다.
     */
    @Transactional
    public void removeBlockedWeekday(Long itemId, DayOfWeek dayOfWeek, Long userId) {
        Item item = getOwnedItem(itemId, userId);
        itemBlockWeekdayRepository.deleteByItemAndDayOfWeek(item, dayOfWeek);
    }

    /**
     * 등록자 본인이 설정한 반복 차단 요일 목록을 조회합니다. (등록/수정 화면용)
     */
    @Transactional(readOnly = true)
    public ItemBlockWeekdayListResDto getBlockedWeekdays(Long itemId, Long userId) {
        Item item = getOwnedItem(itemId, userId);

        List<DayOfWeek> blockedWeekdays = itemBlockWeekdayRepository.findAllByItem(item).stream()
                .map(ItemBlockWeekday::getDayOfWeek)
                .sorted()
                .toList();

        return ItemBlockWeekdayListResDto.builder()
                .blockedWeekdays(blockedWeekdays)
                .build();
    }

    /**
     * 주어진 기간에 등록자가 지정한 차단(특정 날짜 또는 매주 반복 요일)이 겹치는지 확인합니다.
     * 대여 신청/승인 검증에서 사용합니다.
     */
    @Transactional(readOnly = true)
    public boolean isPeriodBlockedByOwner(Item item, LocalDate startDate, LocalDate endDate) {
        if (itemBlockedDateRepository.existsByItemAndBlockedDateBetween(item, startDate, endDate)) {
            return true;
        }

        List<DayOfWeek> blockedWeekdays = itemBlockWeekdayRepository.findAllByItem(item).stream()
                .map(ItemBlockWeekday::getDayOfWeek)
                .toList();

        if (blockedWeekdays.isEmpty()) {
            return false;
        }

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (blockedWeekdays.contains(date.getDayOfWeek())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 등록자가 설정한 차단(특정 날짜 + 반복 요일)을 주어진 기간 내의 구체적인 날짜들로 펼쳐서 반환합니다.
     * 반복 요일 차단은 규칙 그 자체로는 캘린더에 표시할 수 없으므로, 여기서 실제 날짜로 변환합니다.
     */
    @Transactional(readOnly = true)
    public List<ItemBlockRangeResDto.BlockRangeItem> getOwnerBlockedRanges(Item item, LocalDate from, LocalDate to) {
        List<ItemBlockRangeResDto.BlockRangeItem> result = new ArrayList<>();

        itemBlockedDateRepository.findAllByItem(item).stream()
                .map(ItemBlockedDate::getBlockedDate)
                .filter(date -> !date.isBefore(from) && !date.isAfter(to))
                .forEach(date -> result.add(ItemBlockRangeResDto.BlockRangeItem.builder()
                        .startDate(date)
                        .endDate(date)
                        .source(BlockSource.OWNER)
                        .build()));

        List<DayOfWeek> blockedWeekdays = itemBlockWeekdayRepository.findAllByItem(item).stream()
                .map(ItemBlockWeekday::getDayOfWeek)
                .toList();

        if (!blockedWeekdays.isEmpty()) {
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                if (blockedWeekdays.contains(date.getDayOfWeek())) {
                    result.add(ItemBlockRangeResDto.BlockRangeItem.builder()
                            .startDate(date)
                            .endDate(date)
                            .source(BlockSource.OWNER)
                            .build());
                }
            }
        }

        return result;
    }
}
