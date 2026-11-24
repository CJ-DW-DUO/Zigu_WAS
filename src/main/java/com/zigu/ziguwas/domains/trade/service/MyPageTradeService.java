package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.trade.dto.response.MyPageReceiveResDto;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeListResDto;
import com.zigu.ziguwas.domains.trade.entity.Trade;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyPageTradeService {

    private final MyPageTradeRepository myPageTradeRepository;
    private final UserRepository userRepository;
    private final TradeChatRoomFinder tradeChatRoomFinder;

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

        return toListDtos(userId, myPageTradeRepository.findAllByRenteeAndTradeStatus(user, tradeStatus));
    }

    /**
     * 내가 빌려준 물건 내역을 조회합니다.
     *
     * @param userId 현재 로그인한 사용자의 ID
     * @return 대여 해준 거래 정보 DTO 리스트 (대여중 + 반납완료 — FE 가 상태로 탭을 거른다)
     */
    @Transactional(readOnly = true)
    public List<MyPageTradeListResDto> findRenterItems(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return toListDtos(userId, myPageTradeRepository.findAllByRenterAndTradeStatusIn(user, List.of(TradeStatus.IN_PROGRESS, TradeStatus.RETURNED)));
    }

    /**
     * 보낸 요청을 조회 합니다. (공급글은 임차인이 보낸 요청, 요청글은 임대인이 보낸 제안)
     *
     * @param userId 요청을 보낸 사용자의 userId
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

        Page<Trade> trades = myPageTradeRepository.findSentRequests(user, statuses, pageable);
        Map<Long, String> chatRoomIds = tradeChatRoomFinder.find(userId, trades.getContent());
        return trades.map(trade -> MyPageTradeListResDto.fromEntity(trade, userId, chatRoomIds.get(trade.getId())));
    }

    /**
     * 받은 대여 요청을 조회합니다. (공급글은 임대인이 받은 요청, 요청글은 임차인이 받은 제안)
     *
     * @param userId 요청을 받은 사용자의 userId
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

        Page<Trade> trades = myPageTradeRepository.findReceivedRequests(user, statuses, pageable);
        Map<Long, String> chatRoomIds = tradeChatRoomFinder.find(userId, trades.getContent());
        return trades.map(trade -> MyPageReceiveResDto.fromEntity(trade, chatRoomIds.get(trade.getId())));
    }

    /**
     * 거래 목록을 응답 DTO 로 바꿉니다. 상대방과의 채팅방 ID 는 목록 전체를 한 번에 조회해서 채웁니다.
     */
    private List<MyPageTradeListResDto> toListDtos(Long userId, List<Trade> trades) {
        Map<Long, String> chatRoomIds = tradeChatRoomFinder.find(userId, trades);
        return trades.stream()
                .map(trade -> MyPageTradeListResDto.fromEntity(trade, userId, chatRoomIds.get(trade.getId())))
                .toList();
    }

}
