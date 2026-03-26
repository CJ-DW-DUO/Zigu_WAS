package com.zigu.ziguwas.domains.trade.controller;

import com.zigu.ziguwas.domains.trade.dto.request.TradeOfferReqDto;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.service.TradeService;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     * @param details 로그인 정보
     * @param dto 매물ID, 대여일수
     * @return 만들어진 거래 정보
     */
    public ResponseEntity<?> tradeOffer(
            @AuthenticationPrincipal CustomUserDetails details,
            @RequestBody TradeOfferReqDto dto
    ){
        return ResponseEntity.created(URI.create("/api/v1/trades/" +
                tradeService.tradeOffer(details, dto))).build();
    }

}
