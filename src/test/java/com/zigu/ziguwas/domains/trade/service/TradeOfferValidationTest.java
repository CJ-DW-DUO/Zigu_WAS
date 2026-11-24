package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.config.Kst;
import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.PostType;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.trade.dto.request.TradeOfferReqDto;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** W-04: 거래 신청(tradeOffer) 검증 — 순수 단위 테스트(DB·Spring 없음). */
class TradeOfferValidationTest {

    TradeRepository tradeRepository = mock(TradeRepository.class);
    UserRepository userRepository = mock(UserRepository.class);
    ItemRepository itemRepository = mock(ItemRepository.class);
    BlockRepository blockRepository = mock(BlockRepository.class);
    ItemBlockService itemBlockService = mock(ItemBlockService.class);
    TradeService service = new TradeService(tradeRepository, userRepository, itemRepository,
            blockRepository, itemBlockService, mock(ApplicationEventPublisher.class));

    CustomUserDetails details = mock(CustomUserDetails.class);
    User rentee = mock(User.class);
    User renter = mock(User.class);
    Item item = mock(Item.class);

    @BeforeEach
    void setUp() {
        University univ = mock(University.class);
        when(univ.getUnivId()).thenReturn(1L);
        when(details.getUsername()).thenReturn("rentee@x.ac.kr");
        when(userRepository.findByEmail("rentee@x.ac.kr")).thenReturn(Optional.of(rentee));
        when(rentee.getId()).thenReturn(1L);
        when(rentee.getUniv()).thenReturn(univ);
        when(renter.getId()).thenReturn(2L);
        when(renter.getUniv()).thenReturn(univ);
        when(item.getPostType()).thenReturn(PostType.SUPPLY);
        when(item.getUser()).thenReturn(renter);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(tradeRepository.save(any(Trade.class))).thenReturn(Trade.builder().id(99L).build());
    }

    private ErrorCode errorOf(LocalDate start, LocalDate end) {
        TradeOfferReqDto dto = new TradeOfferReqDto();
        dto.setItemId(10L);
        dto.setStartDate(start);
        dto.setEndDate(end);
        return assertThrows(CustomException.class, () -> service.tradeOffer(details, dto)).getErrorCode();
    }

    @Test
    void 종료일이_시작일보다_빠르면_거절() {
        LocalDate today = Kst.today();
        assertEquals(ErrorCode.INVALID_DATE_RANGE, errorOf(today.plusDays(3), today.plusDays(1)));
        verify(tradeRepository, never()).save(any());
    }

    @Test
    void 지난_날짜로_시작하면_거절() {
        LocalDate today = Kst.today();
        assertEquals(ErrorCode.TRADE_PAST_DATE, errorOf(today.minusDays(1), today.plusDays(1)));
    }

    @Test
    void 차단_관계면_거절() {
        when(blockRepository.existsBlockBetween(1L, 2L)).thenReturn(true);
        LocalDate today = Kst.today();
        assertEquals(ErrorCode.BLOCKED_USER_ACCESS, errorOf(today, today.plusDays(1)));
    }

    @Test
    void 같은_매물에_요청중인_신청이_있으면_거절() {
        when(tradeRepository.existsByItemAndRenteeAndTradeStatusIn(item, rentee, List.of(TradeStatus.REQUESTED)))
                .thenReturn(true);
        LocalDate today = Kst.today();
        assertEquals(ErrorCode.ACTIVE_TRADE_EXISTS, errorOf(today, today.plusDays(1)));
    }

    @Test
    void 정상_신청은_시작일_종료일_포함_일수와_KST_요청일로_저장() {
        LocalDate today = Kst.today();
        TradeOfferReqDto dto = new TradeOfferReqDto();
        dto.setItemId(10L);
        dto.setStartDate(today);          // 오늘 시작은 허용
        dto.setEndDate(today.plusDays(2)); // 3일(2박 3일)
        service.tradeOffer(details, dto);
        ArgumentCaptor<Trade> cap = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(cap.capture());
        assertEquals(3L, cap.getValue().getPeriod());
        assertEquals(today, cap.getValue().getTradeReqdate());
        assertEquals(TradeStatus.REQUESTED, cap.getValue().getTradeStatus());
    }
}
