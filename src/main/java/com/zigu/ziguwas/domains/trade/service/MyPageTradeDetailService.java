package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeDetailResDto;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.repository.MyPageTradeDetailRepository;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageTradeDetailService {

    private final MyPageTradeDetailRepository myPageTradeDetailRepository;
    private final UserRepository userRepository;

    /**
     * 거래 상세 내역을 조회합니다. (거래 당사자인 임대인/임차인 모두 조회 가능)
     *
     * @param tradeId 거래 Id
     * @param userId 현재 로그인한 사용자의 ID
     * @return 거래 상세 내역 DTO
     * @throws CustomException 거래를 찾을 수 없거나 거래 당사자가 아닐 때 발생
     */
    @Transactional(readOnly = true)
    public MyPageTradeDetailResDto findDetailItem(Long tradeId, Long userId) {
        return findDetailForParty(tradeId, userId);
    }

    /**
     * findDetailItem 메소드와 동일한 조회입니다.
     * 알림 창에서 즉시 접속하는 용도로, 요청글 제안 알림은 임차인에게 가므로 임대인/임차인 모두 조회할 수 있습니다.
     *
     * @param tradeId 거래ID
     * @param userId 로그인한 사용자 ID
     * @return 거래 상세 내역 DTO
     */
    @Transactional(readOnly = true)
    public MyPageTradeDetailResDto findDetailItemForRenter(Long tradeId, Long userId) {
        return findDetailForParty(tradeId, userId);
    }

    private MyPageTradeDetailResDto findDetailForParty(Long tradeId, Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Trade trade = myPageTradeDetailRepository.findWithPartiesById(tradeId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRADE_NOT_FOUND));

        // 거래 당사자가 아니면 거래 존재 여부를 노출하지 않도록 동일하게 TRADE_NOT_FOUND 처리
        boolean isParty = trade.getRenter().getId().equals(userId) || trade.getRentee().getId().equals(userId);
        if (!isParty) {
            throw new CustomException(ErrorCode.TRADE_NOT_FOUND);
        }

        return MyPageTradeDetailResDto.fromEntity(trade, userId);
    }
}
