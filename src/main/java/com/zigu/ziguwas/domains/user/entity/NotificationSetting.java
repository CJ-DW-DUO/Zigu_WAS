package com.zigu.ziguwas.domains.user.entity;

import com.zigu.ziguwas.domains.notification.entity.NotificationCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSetting {

    @Builder.Default
    @Column(name = "chat_noti_enabled", nullable = false)
    private boolean chatNotiEnabled = true;

    @Builder.Default
    @Column(name = "trade_noti_enabled", nullable = false)
    private boolean tradeNotiEnabled = true;

    @Builder.Default
    @Column(name = "marketing_noti_enabled", nullable = false)
    private boolean marketingNotiEnabled = false;

    @Column(name = "marketing_consent_at")
    private LocalDateTime marketingConsentAt = null;

    public boolean isAllowed(NotificationCategory category) {
        return switch (category) {
            case CHAT -> chatNotiEnabled;
            case TRADE -> tradeNotiEnabled;
            case MARKETING -> marketingNotiEnabled;
        };
    }

}
