package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeDetailResDto;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.repository.MyPageTradeDetailRepository;
import com.zigu.ziguwas.domains.user.entity.User;
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
     * 현재 대여 중/ 인 물건의 상세 내역을 조회합니다.
     *
     * @param tradeId 거래 Id
     * @param userId 현재 로그인한 사용자의 ID
     * @return 대여 중인 상세 내역 DTO
     * @throws CustomException 거래를 찾을 수 없거나 권한이 없을 때 발생
     */
    @Transactional(readOnly = true)
    public MyPageTradeDetailResDto findDetailItem(Long tradeId, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Trade trade = myPageTradeDetailRepository.findByIdAndRentee(tradeId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.TRADE_NOT_FOUND));

        if (!trade.getRentee().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        return MyPageTradeDetailResDto.fromEntity(trade,userId);
    }

    /**
     * findDetailItem 메소드와 동일하나, 임대인 전용입니다.
     * 알림 창에서 즉시 접속하는 용도
     *
     * @param tradeId 거래ID
     * @param userId 임대인ID
     * @return 임대인용 상세 내역 DTO
     */
    @Transactional(readOnly = true)
    public MyPageTradeDetailResDto findDetailItemForRenter(Long tradeId, Long userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Trade trade = myPageTradeDetailRepository.findByIdAndRenter(tradeId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.TRADE_NOT_FOUND));

        // 임대인인지 검증하는 로직 포함
        if(!trade.getRenter().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        return MyPageTradeDetailResDto.fromEntity(trade, userId);
    }
}
