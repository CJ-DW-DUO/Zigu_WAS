package com.zigu.ziguwas.domains.trade.controller;

import com.zigu.ziguwas.domains.trade.api.MyPageTradeApi;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageReceiveResDto;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeListResDto;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.service.MyPageTradeService;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MyPageTradeController implements MyPageTradeApi {

    private final MyPageTradeService myPageTradeService;

    /**
     * 대여중 / 반납완료인 item을 조회합니다.
     *
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     * @return 대여중인 item List
     */
    @GetMapping("/renting")
    public ResponseEntity<List<MyPageTradeListResDto>> getRentingItems(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam TradeStatus tradeStatus
    ) {

        List<MyPageTradeListResDto> myPageTradeListResDto = myPageTradeService.findRentingItems(customUserDetails.getUserId(), tradeStatus);
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

    /**
     * 보낸 대여 요청을 조회 합니다.
     *
     * @param customUserDetails 인증된 객체 정보
     * @param status 거래상태 -> null 넣으면 전체 조회 (필터링 없이)
     * @param pageable 페이지정보
     * @return 보낸 대여요청 목록 list
     */
    @GetMapping("/requests/sent")
    public ResponseEntity<Page<MyPageTradeListResDto>> getSentRequests(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) TradeStatus status, // null 넣으면 전체 조회 (필터링 없이)
            @PageableDefault(size = 10, sort = "tradeReqdate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MyPageTradeListResDto> response = myPageTradeService.getSentRequests(
                customUserDetails.getUserId(),
                status,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 받은 대여 요청을 조회 합니다.
     *
     * @param customUserDetails 인증된 객체 정보
     * @param status 거래상태 -> null 넣으면 전체 조회 (필터링 없이)
     * @param pageable 페이지정보
     * @return 보낸 대여요청 목록 list
     */
    @GetMapping("/renter/request")
    public ResponseEntity<Page<MyPageReceiveResDto>> getReceivedRequests(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) TradeStatus status, // null 넣으면 전체 조회 (필터링 없이)
            @PageableDefault(size = 10, sort = "tradeReqdate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MyPageReceiveResDto> response = myPageTradeService.getReceivedRequests(
                customUserDetails.getUserId(),
                status,
                pageable
        );
        return ResponseEntity.ok(response);
    }

}
