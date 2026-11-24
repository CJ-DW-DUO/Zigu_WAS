package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.chat.entity.ChatParticipant;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.chat.repository.ChatRoomRepository;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.PostType;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageReceiveResDto;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeDetailResDto;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeListResDto;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** W-23: 거래 응답의 상대방·채팅방 필드 — 순수 단위 테스트(DB·Mongo 없음). */
class TradeResponseTest {

    ChatParticipantRepository participantRepository = mock(ChatParticipantRepository.class);
    ChatRoomRepository roomRepository = mock(ChatRoomRepository.class);
    TradeChatRoomFinder finder = new TradeChatRoomFinder(participantRepository, roomRepository);

    User rentee = user(1L, "빌리는사람");
    User renter = user(2L, "빌려주는사람");
    Item item = mock(Item.class);
    Trade trade;

    static User user(Long id, String nickname) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getNickname()).thenReturn(nickname);
        return u;
    }

    @BeforeEach
    void setUp() {
        when(item.getId()).thenReturn(10L);
        when(item.getPostType()).thenReturn(PostType.SUPPLY);
        when(item.getTitle()).thenReturn("카메라");
        trade = Trade.builder().id(5L).item(item).renter(renter).rentee(rentee)
                .tradeStdate(LocalDate.of(2026, 12, 1)).tradeEndate(LocalDate.of(2026, 12, 3)).period(3L)
                .tradeStatus(TradeStatus.REQUESTED).tradeReqdate(LocalDate.of(2026, 11, 30)).build();
    }

    @Test
    void 상대방은_조회자_반대편이고_날짜와_채팅방이_채워진다() {
        MyPageTradeListResDto asRentee = MyPageTradeListResDto.fromEntity(trade, 1L, "room1");
        assertEquals(2L, asRentee.getCounterpartId());
        assertEquals("빌려주는사람", asRentee.getCounterpartNickname());
        assertEquals(LocalDate.of(2026, 12, 1), asRentee.getTradeStdate());
        assertEquals("room1", asRentee.getChatRoomId());

        MyPageTradeListResDto asRenter = MyPageTradeListResDto.fromEntity(trade, 2L, null);
        assertEquals(1L, asRenter.getCounterpartId());
        assertNull(asRenter.getChatRoomId());
    }

    @Test
    void 상세는_거래ID_아이템ID_postType_상태코드를_준다() {
        MyPageTradeDetailResDto dto = MyPageTradeDetailResDto.fromEntity(trade, 2L, "room1");
        assertEquals(5L, dto.getTradeId());
        assertEquals(10L, dto.getItemId());
        assertEquals("SUPPLY", dto.getPostType());
        assertEquals("REQUESTED", dto.getTradeStatusCode());
        assertEquals(1L, dto.getCounterpartId());
        assertEquals("room1", dto.getChatRoomId());
    }

    @Test
    void 받은요청의_상대방은_요청을_보낸_사람() {
        assertEquals(1L, MyPageReceiveResDto.fromEntity(trade, null).getCounterpartId());
    }

    @Test
    void 채팅방은_같은_매물이고_상대가_참여한_방만_찾는다() {
        when(participantRepository.findAllByUserIdAndLeftAtIsNull(2L)).thenReturn(List.of(
                ChatParticipant.builder().chatRoomId("other-item").userId(2L).build(),
                ChatParticipant.builder().chatRoomId("room1").userId(2L).build()));
        when(roomRepository.findAllById(any())).thenReturn(List.of(
                ChatRoom.builder().id("other-item").itemId(99L).build(),
                ChatRoom.builder().id("room1").itemId(10L).build()));
        when(participantRepository.findAllByChatRoomIdIn(any())).thenReturn(List.of(
                ChatParticipant.builder().chatRoomId("room1").userId(2L).build(),
                ChatParticipant.builder().chatRoomId("room1").userId(1L).build()));

        assertEquals(Map.of(5L, "room1"), finder.find(2L, List.of(trade)));
    }

    @Test
    void 내가_참여한_채팅방이_없으면_Mongo_추가_조회_없이_빈_결과() {
        when(participantRepository.findAllByUserIdAndLeftAtIsNull(2L)).thenReturn(List.of());
        assertTrue(finder.find(2L, List.of(trade)).isEmpty());
        verify(roomRepository, never()).findAllById(any());
    }

    @Test
    void Mongo_조회가_실패해도_거래_응답은_막지_않는다() {
        when(participantRepository.findAllByUserIdAndLeftAtIsNull(any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("mongo down"));
        assertTrue(finder.find(2L, List.of(trade)).isEmpty());
    }
}
