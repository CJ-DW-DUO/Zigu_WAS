package com.zigu.ziguwas.domains.item.entity;

import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE item SET is_deleted = true, deleted_at = NOW() WHERE item_id = ?")
@SQLRestriction("is_deleted = false")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ItemCategory category;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "day_per_price", nullable = false)
    private Long dayPerPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    @Builder.Default
    private ItemStatus itemStatus = ItemStatus.REGISTERED; // 물건 대여 상태

    @Column(name = "is_reported", nullable = false)
    private boolean isReported; // 신고처리

    @Builder.Default
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemImage> imageUrl = new ArrayList<>();

    @Builder.Default
    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;


    public void addImage(ItemImage image) {
        this.imageUrl.add(image);
        if (image.getItem() != this) { // 무한 loop 방지
            image.updateItem(this);
        }

    }

    public void updateItemPost(String title, ItemCategory itemCategory, Long dayPerPrice, String description){
        this.title = title;
        this.category = itemCategory;
        this.dayPerPrice = dayPerPrice;
        this.description = description;
    }

    public void updateItemStatus(ItemStatus itemStatus) {
        this.itemStatus = itemStatus;
    }
}