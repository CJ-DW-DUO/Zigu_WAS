package com.zigu.ziguwas.domains.trade.api;

import com.zigu.ziguwas.domains.trade.dto.request.TradeOfferReqDto;
import com.zigu.ziguwas.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Trade API", description = "거래(대여) 관련 API 입니다.")
public interface TradeApi {

    @Operation(summary = "거래 제안", description = "임차인이 특정 매물에 대해 대여 거래를 제안합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "거래 제안 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 정보")
    })
    ResponseEntity<?> tradeOffer(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails details,
            @RequestBody TradeOfferReqDto dto
    );

    @Operation(summary = "대여 제안 수락", description = "임대인이 들어온 대여 제안을 수락합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제안 수락 성공"),
            @ApiResponse(responseCode = "404", description = "거래 정보를 찾을 수 없음")
    })
    ResponseEntity<?> approveTrade(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails details,
            @PathVariable Long tradeId
    );

    @Operation(summary = "대여 제안 거절", description = "임대인이 들어온 대여 제안을 거절합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제안 거절 성공"),
            @ApiResponse(responseCode = "404", description = "거래 정보를 찾을 수 없음")
    })
    ResponseEntity<?> rejectTrade(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails details,
            @PathVariable Long tradeId
    );

    @Operation(summary = "대여 반납 확인", description = "임대인이 물건을 반납받았음을 확인하여 거래를 종료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "반납 확인 성공"),
            @ApiResponse(responseCode = "404", description = "거래 정보를 찾을 수 없음")
    })
    ResponseEntity<?> returnTradeCheck(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails details,
            @PathVariable Long tradeId
    );
}
