package com.zigu.ziguwas.domains.trade.controller;

import com.zigu.ziguwas.domains.trade.api.MyPageTradeApi;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeListResDto;
import com.zigu.ziguwas.domains.trade.service.MyPageTradeService;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MyPageTradeController implements MyPageTradeApi {

    private final MyPageTradeService myPageTradeService;

    /**
     * 대여중인 item을 조회합니다.
     *
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     * @return 대여중인 item List
     */
    @GetMapping("/renting")
    public ResponseEntity<List<MyPageTradeListResDto>> getRentingItems(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

        List<MyPageTradeListResDto> myPageTradeListResDto = myPageTradeService.findRentingItems(customUserDetails.getUserId());
        return ResponseEntity.ok(myPageTradeListResDto);
    }

    /**
     * 대여해준 item을 조회합니다.
     *
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     * @return 대여중인 item List
     */
    @GetMapping("/renter")
    public ResponseEntity<List<MyPageTradeListResDto>> getRenterItems(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

        List<MyPageTradeListResDto> myPageTradeListResDto =  myPageTradeService.findRenterItems(customUserDetails.getUserId());
        return ResponseEntity.ok(myPageTradeListResDto);
    }
}
