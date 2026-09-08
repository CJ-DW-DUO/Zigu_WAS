package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.trade.dto.response.ItemBlockedDateListResDto;
import com.zigu.ziguwas.domains.trade.entity.ItemBlockedDate;
import com.zigu.ziguwas.domains.trade.repository.ItemBlockedDateRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 등록자가 직접 설정하는 아이템 대여 불가 차단(특정 날짜 / 반복 요일)을 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class ItemBlockService {

    private final ItemRepository itemRepository;
    private final ItemBlockedDateRepository itemBlockedDateRepository;

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
}
