package com.zigu.ziguwas.domains.user;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.dto.auth.response.OngoingTradeResDto;
import com.zigu.ziguwas.domains.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 탈퇴 점검 응답: 일일 가격(price)은 그대로, 총액(totalPrice = 일일 가격 × 대여 일수) 추가. */
class OngoingTradeTotalPriceTest {

    @Test
    void 진행중_거래에_총액이_추가된다() {
        University univ = new University(1L, "서울대학교", "snu.ac.kr");
        User renter = User.builder().id(1L).email("a@snu.ac.kr").nickname("a").univ(univ).build();
        User rentee = User.builder().id(2L).email("b@snu.ac.kr").nickname("b").univ(univ).build();
        Item item = Item.builder().id(10L).user(renter).title("노트북").category(ItemCategory.ELECTRONICS)
                .dayPerPrice(15000L).build();
        Trade trade = Trade.builder().id(5L).item(item).renter(renter).rentee(rentee).dayPerPrice(15000L).period(3L)
                .tradeStatus(TradeStatus.IN_PROGRESS).build();

        OngoingTradeResDto dto = OngoingTradeResDto.from(trade);

        assertEquals(15000L, dto.getPrice());
        assertEquals(45000L, dto.getTotalPrice());
    }
}
