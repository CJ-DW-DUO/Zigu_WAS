package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.trade.dto.response.ItemBlockRangeResDto;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** W-08: block-ranges 조회는 아이템 등록자와 같은 대학 사용자만 가능하다. */
class BlockRangesUniversityTest {

    TradeRepository tradeRepository = mock(TradeRepository.class);
    ItemRepository itemRepository = mock(ItemRepository.class);
    ItemBlockService itemBlockService = mock(ItemBlockService.class);
    TradeService service = new TradeService(tradeRepository, mock(UserRepository.class), itemRepository,
            mock(BlockRepository.class), itemBlockService, mock(ApplicationEventPublisher.class));

    private Item itemOfUniv(Long univId) {
        University univ = mock(University.class);
        when(univ.getUnivId()).thenReturn(univId);
        User owner = mock(User.class);
        when(owner.getUniv()).thenReturn(univ);
        Item item = mock(Item.class);
        when(item.getUser()).thenReturn(owner);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        return item;
    }

    @Test
    void 다른_대학_사용자는_차단_기간을_조회할_수_없다() {
        itemOfUniv(1L);

        CustomException e = assertThrows(CustomException.class,
                () -> service.getBlockRanges(10L, 2L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10)));

        assertEquals(ErrorCode.DIFFERENT_UNIVERSITY_ACCESS, e.getErrorCode());
        verifyNoInteractions(itemBlockService);
    }

    @Test
    void 같은_대학_사용자는_정상_조회() {
        Item item = itemOfUniv(1L);
        when(tradeRepository.findOverlappingTrades(any(), any(), any(), any())).thenReturn(List.of());
        when(itemBlockService.getOwnerBlockedRanges(any(), any(), any())).thenReturn(List.of());

        ItemBlockRangeResDto res = service.getBlockRanges(10L, 1L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10));

        assertNotNull(res);
    }
}
