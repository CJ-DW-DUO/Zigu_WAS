package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.trade.dto.response.MyPageReceiveResDto;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeListResDto;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.MyPageTradeRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageTradeService {

    private final MyPageTradeRepository myPageTradeRepository;
    private final UserRepository userRepository;

    /**
     * 내가 빌린 물건 내역을 조회합니다.
     *
     * @param userId 현재 로그인한 사용자의 ID
     * @param tradeStatus 필터링 할 거래 상태
     * @return 대여 중인 거래 정보 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyPageTradeListResDto> findRentingItems(Long userId, TradeStatus tradeStatus) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return myPageTradeRepository.findAllByRenteeAndTradeStatus(user, tradeStatus)
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

    /**
     * 보낸 요청을 조회 합니다.
     *
     * @param userId rentee의 userId
     * @param tradeStatus 필터링 할 문자
     * @param pageable page 정보
     * @return 보낸 요청에 응답할 DTO
     */

    @Transactional(readOnly = true)
    public Page<MyPageTradeListResDto> getSentRequests(Long userId, TradeStatus tradeStatus, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<TradeStatus> statuses = (tradeStatus == null)
                ? List.of(TradeStatus.IN_PROGRESS,TradeStatus.REQUESTED,TradeStatus.REJECTED)
                : List.of(tradeStatus);

        return myPageTradeRepository.findAllByRenteeAndTradeStatusIn(user, statuses, pageable)
                .map(MyPageTradeListResDto::fromEntity);
    }

    /**
     * 받은 대여 요청을 조회합니다.
     *
     * @param userId renter의 userId
     * @param tradeStatus 필터링 할 상태
     * @param pageable 페이지 정보
     * @return 받은 요청 리스트
     */
    @Transactional(readOnly = true)
    public Page<MyPageReceiveResDto> getReceivedRequests(Long userId, TradeStatus tradeStatus, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<TradeStatus> statuses = (tradeStatus == null)
                ? List.of(TradeStatus.IN_PROGRESS, TradeStatus.REQUESTED, TradeStatus.REJECTED)
                : List.of(tradeStatus);

        return myPageTradeRepository.findAllByRenterAndTradeStatusIn(user, statuses, pageable)
                .map(MyPageReceiveResDto::fromEntity);
    }


}
