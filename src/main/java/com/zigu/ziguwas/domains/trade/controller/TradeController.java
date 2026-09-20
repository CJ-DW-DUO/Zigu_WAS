package com.zigu.ziguwas.domains.trade.controller;

import com.zigu.ziguwas.domains.trade.api.TradeApi;
import com.zigu.ziguwas.domains.trade.dto.request.TradeOfferReqDto;
import com.zigu.ziguwas.domains.trade.dto.request.TradeProposeReqDto;
import com.zigu.ziguwas.domains.trade.service.TradeService;
import com.zigu.ziguwas.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trades")
public class TradeController implements TradeApi {

    private final TradeService tradeService;

    /**
     * 거래 제안 API
     *
     * @param details 임차예정인 로그인 정보
     * @param dto 매물ID, 대여시작일, 대여 종료일
     * @return 만들어진 거래 정보
     */
    @PostMapping
    public ResponseEntity<?> tradeOffer(
            @AuthenticationPrincipal CustomUserDetails details,
            @RequestBody TradeOfferReqDto dto
    ){
        return ResponseEntity.created(URI.create("/api/v1/trades/" +
                tradeService.tradeOffer(details, dto))).build();
    }

    /**
     * 요청글 대여 제안 API
     *
     * @param details 물건을 빌려주려는 공급자 로그인 정보
     * @param dto 요청글(아이템)ID
     * @return 만들어진 거래 정보
     */
    @PostMapping("/proposals")
    public ResponseEntity<?> proposeToDemand(
            @AuthenticationPrincipal CustomUserDetails details,
            @RequestBody @Valid TradeProposeReqDto dto
    ){
        return ResponseEntity.created(URI.create("/api/v1/trades/" +
                tradeService.proposeToDemand(details, dto))).build();
    }

    /**
     * 대여 제안 수락 API
     *
     * 공급글은 임대인이, 요청글은 요청글 작성자(임차인)가 수락한다.
     *
     * @param details 수락 권한자 로그인 정보
     * @param tradeId 거래ID
     * @return 성공여부
     */
    @PostMapping("/{tradeId}/accept")
    public ResponseEntity<?> approveTrade(
            @AuthenticationPrincipal CustomUserDetails details,
            @PathVariable Long tradeId
    ){
        // 거래 승인 true 매개변수 전달
        tradeService.offerResponse(details, tradeId, true);
        return ResponseEntity.ok().build();
    }

    /**
     * 대여 제안 거절 API
     *
     * @param details 임대인 로그인 정보
     * @param tradeId 거래ID
     * @return 성공여부
     */
    @PostMapping("/{tradeId}/reject")
    public ResponseEntity<?> rejectTrade(
            @AuthenticationPrincipal CustomUserDetails details,
            @PathVariable Long tradeId
    ){
        // 거래 승인 false(거절) 매개변수 전달
        tradeService.offerResponse(details, tradeId, false);
        return ResponseEntity.ok().build();
    }


    /**
     * 대여 반납 확인 API
     *
     * 임대인이 물건을 받는 즉시 반납 확인
     *
     * @param details 임대인 로그인 정보
     * @param tradeId 거래ID
     * @return 성공여부
     */
    @PostMapping("/{tradeId}/return")
    public ResponseEntity<?> returnTradeCheck(
            @AuthenticationPrincipal CustomUserDetails details,
            @PathVariable Long tradeId
    ){
        tradeService.returnTradeCheck(details, tradeId);
        return ResponseEntity.ok().build();
    }

}
