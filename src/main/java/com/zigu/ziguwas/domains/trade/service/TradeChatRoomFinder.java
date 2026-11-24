package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 거래 목록·상세 응답에 넣을 "거래 상대와의 채팅방 ID" 를 찾는다.
 *
 * 거래가 몇 건이든 조회 1번(내 방 + 같은 방의 상대방을 조인)으로 끝낸다.
 * 채팅방을 못 찾거나 조회가 실패해도 거래 응답은 그대로 내려가고 chatRoomId 만 null 이 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeChatRoomFinder {

    private final ChatRoomRepository chatRoomRepository;

    /**
     * @param userId 조회하는 사용자 (나가지 않은 채팅방만 대상)
     * @param trades 거래 목록 (item·renter·rentee 가 로딩되어 있어야 함)
     * @return 거래ID → 채팅방ID. 채팅방이 없는 거래는 키가 없다.
     */
    public Map<Long, String> find(Long userId, List<Trade> trades) {
        Map<Long, String> result = new HashMap<>();
        Set<Long> itemIds = trades.stream()
                .filter(trade -> trade.getItem() != null)
                .map(trade -> trade.getItem().getId())
                .collect(Collectors.toSet());
        if (itemIds.isEmpty()) {
            return result;
        }
        try {
            // [채팅방 ID, 물품 ID, 상대방 userId]
            List<Object[]> rooms = chatRoomRepository.findMyRoomsWithCounterparts(userId, itemIds);
            for (Trade trade : trades) {
                if (trade.getItem() == null) {
                    continue;
                }
                Long itemId = trade.getItem().getId();
                Long counterpartId = counterpartId(trade, userId);
                for (Object[] room : rooms) {
                    if (itemId.equals(room[1]) && counterpartId.equals(room[2])) {
                        result.putIfAbsent(trade.getId(), (String) room[0]);
                    }
                }
            }
        } catch (DataAccessException e) {
            // 채팅 저장소 조회 문제로 거래 목록 전체가 실패하지 않도록 chatRoomId 만 비운다.
            log.warn("거래 응답용 채팅방 조회 실패: {}", e.toString());
            return new HashMap<>();
        }
        return result;
    }

    /** 거래에서 조회자의 상대방 userId */
    public static Long counterpartId(Trade trade, Long userId) {
        return trade.getRentee().getId().equals(userId)
                ? trade.getRenter().getId()
                : trade.getRentee().getId();
    }
}
