package com.zigu.ziguwas.domains.item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "is_main_image_url", nullable = false)
    private boolean isMainImageUrl;

    public ItemImage(String imageUrl, boolean isMainImageUrl) {
        this.imageUrl = imageUrl;
        this.isMainImageUrl = isMainImageUrl;
    }
    public void updateItem(Item item) {
        this.item = item;
    }
    public void updateMain(boolean isMainImageUrl) {this.isMainImageUrl = isMainImageUrl;}
}
