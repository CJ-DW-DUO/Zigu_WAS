package com.zigu.ziguwas.domains.user;

import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.item.event.ItemImagesDeletedEvent;
import com.zigu.ziguwas.domains.item.repository.ItemListRepository;
import com.zigu.ziguwas.domains.notification.repository.PushTokenRepository;
import com.zigu.ziguwas.domains.search.repository.SearchHistoryRepository;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.domains.user.service.AuthService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.redis.RedisService;
import com.zigu.ziguwas.security.JwtUtil;
import com.zigu.ziguwas.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 탈퇴 시 본인 물건·검색 기록·차단 정리 (거래·채팅 정리는 포함하지 않는다). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WithdrawCleanupTest {

    @Mock UserRepository userRepository;
    @Mock TradeRepository tradeRepository;
    @Mock PushTokenRepository pushTokenRepository;
    @Mock TokenService tokenService;
    @Mock JwtUtil jwtUtil;
    @Mock RedisService redisService;
    @Mock ItemListRepository itemListRepository;
    @Mock SearchHistoryRepository searchHistoryRepository;
    @Mock BlockRepository blockRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AuthService authService;

    User user = User.builder().id(1L).email("a@snu.ac.kr").nickname("홍길동").password("pw")
            .univ(new University(1L, "서울대학교", "snu.ac.kr")).build();

    private Item item(Long id, String imageUrl) {
        Item item = Item.builder().id(id).user(user).title("t" + id).category(ItemCategory.ELECTRONICS).dayPerPrice(1000L).build();
        if (imageUrl != null) {
            item.getImageUrl().add(new ItemImage(imageUrl, true));
        }
        return item;
    }

    @BeforeEach
    void setUp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tradeRepository.findAllByUserIdInvolvedAndStatus(1L, TradeStatus.IN_PROGRESS)).thenReturn(List.of());
        when(jwtUtil.getExpiration(anyString())).thenReturn(1000L);
    }

    // 물건 2개 중 대기 중(REQUESTED) 거래가 걸린 것은 조회 단계에서 빠지므로 불러오지 않고 남기고, 나머지는 삭제하며 이미지 삭제 이벤트는 한 번만 낸다.
    @Test
    void 거래가_걸린_물건은_남기고_나머지_물건_검색기록_차단을_정리한다() {
        Item free = item(10L, "http://s3/free.png");
        when(itemListRepository.findAllWithoutActiveTradeByUserId(eq(1L), eq(List.of(TradeStatus.REQUESTED, TradeStatus.IN_PROGRESS))))
                .thenReturn(List.of(free));
        when(itemListRepository.countByUserId(1L)).thenReturn(2L);

        authService.withdraw(1L, "token", "사유");

        verify(itemListRepository).delete(free);
        verify(itemListRepository, times(1)).delete(any(Item.class));
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(List.of("http://s3/free.png"), ((ItemImagesDeletedEvent) event.getValue()).imageUrls());
        verify(searchHistoryRepository).deleteAllByUserId(1L);
        verify(blockRepository).deleteAllInvolving(1L);
    }

    // 사용자 삭제는 정리가 모두 끝난 뒤 마지막에 한다.
    @Test
    void 사용자_삭제는_마지막에_한다() {
        Item free = item(10L, null);
        when(itemListRepository.findAllWithoutActiveTradeByUserId(eq(1L), anyList())).thenReturn(List.of(free));
        when(itemListRepository.countByUserId(1L)).thenReturn(1L);

        authService.withdraw(1L, "token", "사유");

        InOrder order = inOrder(itemListRepository, searchHistoryRepository, blockRepository, userRepository);
        order.verify(itemListRepository).delete(free);
        order.verify(searchHistoryRepository).deleteAllByUserId(1L);
        order.verify(blockRepository).deleteAllInvolving(1L);
        order.verify(userRepository).delete(user);
        // 이미지가 없는 물건만 지웠으면 이미지 삭제 이벤트는 내지 않는다.
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    // 진행 중(IN_PROGRESS) 거래가 있으면 기존처럼 탈퇴 자체가 막히고 아무것도 정리하지 않는다.
    @Test
    void 진행중_거래가_있으면_탈퇴가_막히고_정리도_하지_않는다() {
        when(tradeRepository.findAllByUserIdInvolvedAndStatus(1L, TradeStatus.IN_PROGRESS)).thenReturn(List.of(new Trade()));

        CustomException e = assertThrows(CustomException.class, () -> authService.withdraw(1L, "token", "사유"));

        assertEquals(ErrorCode.WITHDRAW_ACTIVE_TRADE_EXISTS, e.getErrorCode());
        verify(itemListRepository, never()).findAllWithoutActiveTradeByUserId(anyLong(), anyList());
        verify(searchHistoryRepository, never()).deleteAllByUserId(anyLong());
        verify(blockRepository, never()).deleteAllInvolving(anyLong());
        verify(userRepository, never()).delete(any(User.class));
    }
}
