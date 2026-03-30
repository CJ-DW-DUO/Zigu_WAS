package com.zigu.ziguwas.domains.trade.controller;

import com.zigu.ziguwas.domains.trade.dto.request.TradeOfferReqDto;
import com.zigu.ziguwas.domains.trade.service.TradeService;
import com.zigu.ziguwas.security.CustomUserDetails;
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
public class TradeController {

    private final TradeService tradeService;

    /**
     * 거래 제안 API
     *
     * @param details 임차예정인 로그인 정보
     * @param dto 매물ID, 대여일수
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
     * 대여 제안 수락 API
     *
     * @param details 임대인 로그인 정보
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

}
