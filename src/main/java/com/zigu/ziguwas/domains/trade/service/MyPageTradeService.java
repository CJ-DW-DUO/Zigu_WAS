package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeListResDto;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.MyPageTradeRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageTradeService {

    private final MyPageTradeRepository myPageTradeRepository;
    private final UserRepository userRepository;

    /**
     * 내가 빌린 물건 중 현재 대여 중인 내역을 조회합니다.
     *
     * @param userId 현재 로그인한 사용자의 ID
     * @return 대여 중인 거래 정보 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyPageTradeListResDto> findRentingItems(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return myPageTradeRepository.findAllByRenteeAndTradeStatus(user, TradeStatus.IN_PROGRESS)
                .stream().map(MyPageTradeListResDto::fromEntity).toList();
    }

    /**
     * 내가 빌려준 물건 내역을 조회합니다.
     *
     * @param userId 현재 로그인한 사용자의 ID
     * @return 대여 해준 거래 정보 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyPageTradeListResDto> findRenterItems(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return myPageTradeRepository.findAllByRenterAndTradeStatus(user,TradeStatus.IN_PROGRESS)
                .stream().map(MyPageTradeListResDto::fromEntity).toList();
    }


}
