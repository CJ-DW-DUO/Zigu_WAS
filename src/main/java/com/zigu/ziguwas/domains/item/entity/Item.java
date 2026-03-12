package com.zigu.ziguwas.domains.item.entity;

import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 소유자 ID

    @Column(name = "title", nullable = false)
    private String title;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "item_category", joinColumns = @JoinColumn(name = "item_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private List<ItemCategory> category;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "day_per_price", nullable = false)
    private Long dayPerPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    @Builder.Default
    private ItemStatus itemStatus = ItemStatus.RESISTED; // 물건 대여 상태

    @Column(name = "is_reported", nullable = false)
    private boolean isReported; // 신고처리

}