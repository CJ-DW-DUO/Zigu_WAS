package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 거래 목록·상세 응답에 넣을 "거래 상대와의 채팅방 ID" 를 찾는다.
 *
 * Mongo 조회가 거래 수만큼 늘어나지 않도록, 거래가 몇 건이든 조회 3번(내 참여 목록 / 채팅방 / 참여자)으로 끝낸다.
 * 채팅방을 못 찾거나 Mongo 조회가 실패해도 거래 응답은 그대로 내려가고 chatRoomId 만 null 이 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeChatRoomFinder {

    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * @param userId 조회하는 사용자 (나가지 않은 채팅방만 대상)
     * @param trades 거래 목록 (item·renter·rentee 가 로딩되어 있어야 함)
     * @return 거래ID → 채팅방ID. 채팅방이 없는 거래는 키가 없다.
     */
    public Map<Long, String> find(Long userId, List<Trade> trades) {
        Map<Long, String> result = new HashMap<>();
        if (trades.isEmpty()) {
            return result;
        }
        try {
            Set<String> myRoomIds = chatParticipantRepository.findAllByUserIdAndLeftAtIsNull(userId).stream()
                    .map(ChatParticipant::getChatRoomId)
                    .collect(Collectors.toSet());
            if (myRoomIds.isEmpty()) {
                return result;
            }

            Set<Long> itemIds = trades.stream()
                    .filter(trade -> trade.getItem() != null)
                    .map(trade -> trade.getItem().getId())
                    .collect(Collectors.toSet());
            // 내 방 중 이 거래들의 매물에 걸린 방만 남긴다 (방ID → 매물ID)
            Map<String, Long> itemIdByRoomId = new HashMap<>();
            for (ChatRoom room : chatRoomRepository.findAllById(myRoomIds)) {
                if (itemIds.contains(room.getItemId())) {
                    itemIdByRoomId.put(room.getId(), room.getItemId());
                }
            }
            if (itemIdByRoomId.isEmpty()) {
                return result;
            }

            // 방ID → 참여자 userId 들
            Map<String, Set<Long>> membersByRoomId = chatParticipantRepository
                    .findAllByChatRoomIdIn(itemIdByRoomId.keySet()).stream()
                    .collect(Collectors.groupingBy(ChatParticipant::getChatRoomId,
                            Collectors.mapping(ChatParticipant::getUserId, Collectors.toSet())));

            for (Trade trade : trades) {
                if (trade.getItem() == null) {
                    continue;
                }
                Long counterpartId = counterpartId(trade, userId);
                itemIdByRoomId.forEach((roomId, itemId) -> {
                    if (itemId.equals(trade.getItem().getId())
                            && membersByRoomId.getOrDefault(roomId, Set.of()).contains(counterpartId)) {
                        result.putIfAbsent(trade.getId(), roomId);
                    }
                });
            }
        } catch (RuntimeException e) {
            // 채팅 저장소(Mongo) 문제로 거래 목록 전체가 실패하지 않도록 chatRoomId 만 비운다.
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
