package com.zigu.ziguwas.domains.item.repository.spec;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import org.springframework.data.jpa.domain.Specification;

public class ItemSpecs {
    /**
     * 카테고리 필터 조건 생성
     */
    public static Specification<Item> withCategory(ItemCategory category) {
        return (root, query, builder) -> {
            if (category == null) return null;
            return builder.equal(root.get("category"), category);
        };
    }

    /**
     * 대학 ID 필터 조건 생성
     * @param univId 대학ID
     * @return 필터링된 Item
     */
    public static Specification<Item> withUniversity(Long univId) {
        return (root, query, builder) -> {
            if (univId == null) return null;
            // get으로 이어지는 구문이 테이블들이 모두 조인되는 현상임.
            return builder.equal(root.get("user").get("univ").get("univId"), univId);
        };
    }


    public static Specification<Item> withTitleContains(String keyword) {
        return (root, query, builder) -> {
            if (keyword == null || keyword.isBlank())
                return builder.disjunction(); // 0건 처리
            // 둘다 소문자로 만들어서 대소문자를 사실상 같게 변경
            return builder.like(builder.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
        };
    }
}
