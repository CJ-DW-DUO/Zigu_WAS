package com.zigu.ziguwas.domains.item.entity;

import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
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

    // 게시글 유형. 기존 데이터는 모두 공급글이므로 DB 기본값을 SUPPLY로 지정해 ddl-auto update 시 기존 행이 채워지도록 한다.
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SUPPLY'")
    @Column(name = "post_type", nullable = false)
    @Builder.Default
    private PostType postType = PostType.SUPPLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    @Builder.Default
    private ItemStatus itemStatus = ItemStatus.REGISTERED; // 물건 대여 상태

    @Column(name = "desired_start_date")
    private LocalDate desiredStartDate; // 요청글(DEMAND) 전용: 희망 대여 시작일

    @Column(name = "desired_end_date")
    private LocalDate desiredEndDate; // 요청글(DEMAND) 전용: 희망 대여 종료일

    @Column(name = "memo", length = 500)
    private String memo; // 요청글(DEMAND) 전용: 거래 희망 장소/시간 등 자유 메모

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

    public void updateDemandInfo(LocalDate desiredStartDate, LocalDate desiredEndDate, String memo) {
        this.desiredStartDate = desiredStartDate;
        this.desiredEndDate = desiredEndDate;
        this.memo = memo;
    }

    public void updateItemStatus(ItemStatus itemStatus) {
        this.itemStatus = itemStatus;
    }
}