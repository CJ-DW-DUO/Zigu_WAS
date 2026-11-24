package com.zigu.ziguwas.domains.user;

import com.zigu.ziguwas.domains.notification.repository.PushTokenRepository;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.domains.user.service.AuthService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/** 탈퇴 시 진행 중 거래가 있으면 거래용 ACTIVE_TRADE_EXISTS 가 아니라 탈퇴 전용 코드로 막는다(FE 문구 매핑 분리). */
@ExtendWith(MockitoExtension.class)
class WithdrawActiveTradeTest {

    @Mock UserRepository userRepository;
    @Mock TradeRepository tradeRepository;
    @Mock PushTokenRepository pushTokenRepository;
    @InjectMocks AuthService authService;

    @Test
    void 진행중_거래가_있으면_탈퇴_전용_코드로_막고_아무것도_지우지_않는다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(tradeRepository.findAllByUserIdInvolvedAndStatus(1L, TradeStatus.IN_PROGRESS))
                .thenReturn(List.of(mock(Trade.class)));

        CustomException e = assertThrows(CustomException.class, () -> authService.withdraw(1L, "token", "사유"));

        assertEquals(ErrorCode.WITHDRAW_ACTIVE_TRADE_EXISTS, e.getErrorCode());
        verifyNoInteractions(pushTokenRepository);
    }
}
