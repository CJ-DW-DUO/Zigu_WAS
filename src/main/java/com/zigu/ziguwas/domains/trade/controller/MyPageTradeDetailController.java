package com.zigu.ziguwas.domains.trade.controller;

import com.zigu.ziguwas.domains.trade.api.MyPageTradeDetailApi;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeDetailResDto;
import com.zigu.ziguwas.domains.trade.service.MyPageTradeDetailService;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MyPageTradeDetailController implements MyPageTradeDetailApi {

    private final MyPageTradeDetailService myPageTradeDetailService;

    /**
     * mypage에서 거래 물건을 상세 조회합니다.
     * @param tradeId 거래ID
     * @param customUserDetails 인증된 객체 정보
     * @return 빌린 물건 상세
     */
    @GetMapping("/trade/{tradeId}")
    public ResponseEntity<MyPageTradeDetailResDto> getDetailItem(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        MyPageTradeDetailResDto myPageTradeDetailResDto = myPageTradeDetailService.findDetailItem(tradeId, customUserDetails.getUserId());
        return ResponseEntity.ok(myPageTradeDetailResDto);
    }


    /**
     * 알림에서 거래 물건에 대한 상세 조회를 위한 API
     *
     * @param tradeId 거래ID
     * @param customUserDetails 인증된 객체 정보
     * @return 빌려준 물건 상세
     */
    @GetMapping("/renter/trade/{tradeId}")
    public ResponseEntity<MyPageTradeDetailResDto> getDetailItemForRenter(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        MyPageTradeDetailResDto myPageTradeDetailResDto = myPageTradeDetailService.findDetailItemForRenter(tradeId, customUserDetails.getUserId());
        return ResponseEntity.ok(myPageTradeDetailResDto);
    }
}
