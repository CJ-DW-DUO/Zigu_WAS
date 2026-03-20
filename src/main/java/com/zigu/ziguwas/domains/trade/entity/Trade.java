package com.zigu.ziguwas.domains.trade.entity;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemStatus;
import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renter_id", nullable = false)
    private User renter; // 임대인ID (빌려주는 자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rentee_id", nullable = false)
    private User rentee; // 임차인ID

    @Column(name = "period", nullable = false)
    private Long period; // 대여기간

    @Column(name = "is_approved", nullable = false)
    private boolean isApproved;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_status", nullable = false)
    private TradeStatus tradeStatus;

    /**
     * 거래 상태를 변경하고 아이템의 상태도 함께 제어합니다.
     * @param newTradeStatus 변경할 거래 상태
     */
    public void updateStatus(TradeStatus newTradeStatus) {
        this.tradeStatus = newTradeStatus;

        if (newTradeStatus == TradeStatus.IN_PROGRESS) {
            this.item.updateItemStatus(ItemStatus.RENTING);
        } else if (newTradeStatus == TradeStatus.RETURNED) {
            this.item.updateItemStatus(ItemStatus.REGISTERED);
        }
    }

    /**
     * 총 대여 금액을 계산합니다.
     *
     * @return (아이템 하루 가격 * 대여 일수)
     */
    public Long calculateTotalPrice() {
        if (this.item == null || this.period == null) {
            return 0L; // 혹시나 하는 null일때..
        }
        return this.item.getDayPerPrice() * this.period;
    }
}
