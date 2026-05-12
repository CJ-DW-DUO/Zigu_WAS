package com.zigu.ziguwas.domains.user.dto.auth.response;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@Builder
@RequiredArgsConstructor
public class WithdrawalCheckResDto {

    private final boolean canWithdraw;
    private final int ongoingCount;
    private final List<OngoingTradeResDto> ongoingTrades;

    public static WithdrawalCheckResDto toEntity(List<OngoingTradeResDto> ongoingTrades) {
        return WithdrawalCheckResDto.builder()
                .canWithdraw(ongoingTrades.isEmpty())
                .ongoingCount(ongoingTrades.size())
                .ongoingTrades(ongoingTrades)
                .build();
    }
}
