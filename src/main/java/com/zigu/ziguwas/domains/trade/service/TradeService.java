package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemStatus;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.trade.dto.request.TradeOfferReqDto;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;


    /**
     * 사용자 불러오기
     *
     * @param email 사용자 이메일
     * @return 사용자 엔티티
     */
    private User getCurrentUser(String email){
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );
    }

    /**
     * 거래 불러오기
     *
     * @param tradeId 거래ID
     * @return 거래 엔티티
     */
    private Trade getCurrentTrade(Long tradeId){
        return tradeRepository.findById(tradeId).orElseThrow(
                () -> new CustomException(ErrorCode.TRADE_NOT_FOUND)
        );
    }


    /**
     * 대여 요청 서비스
     *
     * @param details 임차예정인 로그인 정보
     * @param dto 아이템ID, 매물 대여기간
     */
    @Transactional
    public Long tradeOffer(
            CustomUserDetails details,
            TradeOfferReqDto dto
    ){
        // 1. 임차인 조회(로그인 정보 검증)
        User rentee = getCurrentUser(details.getUsername());

        // 2. 매물 조회
        Item item = itemRepository.findById(dto.getItemId()).orElseThrow(
                () -> new CustomException(ErrorCode.ITEM_NOT_FOUND)
        );

        // 3. 임대인 조회
        User renter = item.getUser();

        // 4. 요청 전송, 거래 시작, 수락일은 요청 당시에 존재하지 않으므로 미기입
        Trade trade = Trade.builder()
                .item(item)
                .renter(renter)
                .rentee(rentee)
                .period(dto.getPeriod())
                .tradeStatus(TradeStatus.REQUESTED)
                .tradeReqdate(LocalDate.now())
                .build();

        Trade saved = tradeRepository.save(trade);

        return saved.getId();
    }


    /**
     * 대여 제안 응답 서비스 -> 승인 혹은 거절
     *
     * @param details 임대인 로그인 정보
     * @param tradeId 거래ID
     * @param isApproved 대여 승인/거절 여부
     */
    @Transactional
    public void offerResponse(
            CustomUserDetails details,
            Long tradeId,
            boolean isApproved
    ){
        // 1. 임대인 조회
        User renter = getCurrentUser(details.getUsername());

        // 2. 거래 조회
        Trade trade = getCurrentTrade(tradeId);

        // 3. 매물 조회
        Item item = trade.getItem();
        if(item == null){
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }

        // 4. 임대인 일치 확인
        if(renter.getId().equals(trade.getRenter().getId())) {
            throw new CustomException(ErrorCode.RENTER_NOT_MATCHED);
        }

        // 5. 거래 요청 상태 확인(대여 오류 방지)
        if(!trade.getTradeStatus().equals(TradeStatus.REQUESTED)) {
            throw new CustomException(ErrorCode.TRADE_STATUS_NOT_REQUESTED);
        }

        // 6. 매물 대여 가능 확인
        if(item.getItemStatus().equals(ItemStatus.RENTING)){
            throw new CustomException(ErrorCode.ITEM_ALREADY_RENTING);
        }

        // 7. 대여 승인 혹은 거절 분기
        if(isApproved){
            // 대여 승인

            // 7A - 1. 거래 상태 변경
            trade.updateStatus(TradeStatus.IN_PROGRESS);

            // 7A - 2. 거래 시작일, 마감일, 수락일 설정
            // 거래 시작일과 수락일에 대한 분리가 필요함, 수정 필수
            trade.setDates(LocalDate.now(), LocalDate.now(),
                    LocalDate.now().plusDays(trade.getPeriod()));

            // 7A - 3. 매물 상태 변경
            item.updateItemStatus(ItemStatus.RENTING);

            // 7A - 4. 매물 변경상태 저장
            itemRepository.save(item);
        } else {
            // 대여 거절

            // 7B. 거래 상태 변경
            trade.updateStatus(TradeStatus.REJECTED);
        }

        // 8. 거래 변경 상태 저장
        tradeRepository.save(trade);
    }

    /**
     * 반납 확인 서비스
     *
     * 임대인과 임차인이 한 대여에 대해서 상호로 반납여부를 확인한다.
     *
     * @param details 임대인 / 임차인 로그인 여부
     * @param tradeId 거래ID
     */
    public void returnTradeCheck(
            CustomUserDetails details,
            Long tradeId
    ){
        // 1. 사용자 조회
        User user = getCurrentUser(details.getUsername());

        // 2. 거래조회
        Trade trade = getCurrentTrade(tradeId);

        // 3. 매물 조회
        Item item = trade.getItem();
        if(item == null){
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }

        // 4. 임대인 일치 확인
        if(!user.getId().equals(trade.getRenter().getId())){
            throw new CustomException(ErrorCode.RENTER_NOT_MATCHED);
        }

        // 5. 아이템 상태 확인
        if(!item.getItemStatus().equals(ItemStatus.RENTING)){
            throw new CustomException(ErrorCode.ITEM_NOT_RENTING);
        }

        // 6. 거래 상태 확인
        if(!trade.getTradeStatus().equals(TradeStatus.IN_PROGRESS)){
            throw new CustomException(ErrorCode.TRADE_STATUS_NOT_REQUESTED);
        }

        // 7. 상태변경
        item.updateItemStatus(ItemStatus.REGISTERED);
        trade.updateStatus(TradeStatus.RETURNED);

        // 8. 변경 반영
        itemRepository.save(item);
        tradeRepository.save(trade);
    }
}
