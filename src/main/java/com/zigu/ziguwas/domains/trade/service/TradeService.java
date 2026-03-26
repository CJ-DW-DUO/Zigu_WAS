package com.zigu.ziguwas.domains.trade.service;

import com.zigu.ziguwas.domains.item.entity.Item;
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
     * 거래 요청 서비스
     *
     * @param details 로그인 정보
     * @param dto 아이템ID, 매물 대여기간
     */
    @Transactional
    public void tradeOffer(
            CustomUserDetails details,
            TradeOfferReqDto dto
    ){
        // 1. 임차인 조회(로그인 정보 검증)
        User rentee = userRepository.findByEmail(details.getUsername()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );


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

        tradeRepository.save(trade);
    }
}
