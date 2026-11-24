package com.zigu.ziguwas.domains.item.service;

import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.item.dto.response.ItemListResDto;
import com.zigu.ziguwas.domains.item.dto.response.ItemSearchCond;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.PostType;
import com.zigu.ziguwas.domains.item.repository.ItemListRepository;
import com.zigu.ziguwas.domains.item.repository.spec.ItemSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemListService {

    private final ItemListRepository itemListRepository;
    private final BlockRepository blockRepository;

    @Transactional(readOnly = true)
    public Page<ItemListResDto> getItemList(ItemSearchCond cond, Pageable pageable ,
                                            Long userId, Long univId) {

        // 1. 정렬 조건 결정
        Sort sort = Sort.by(Sort.Direction.DESC, "id"); // 기본값: 최신순

        // "인기순"과 "조회수순"은 같은 조회수 정렬이다 (홈 화면 정렬 이름이 "조회수순"으로 바뀌어도 받아준다)
        if ("인기순".equals(cond.getSort()) || "조회수순".equals(cond.getSort())) {
            sort = Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "id"));
        } else if ("낮은가격순".equals(cond.getSort())) {
            sort = Sort.by(Sort.Direction.ASC, "dayPerPrice").and(Sort.by(Sort.Direction.DESC, "id"));
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // 2. 요청자가 차단한 사용자 ID 목록 조회 (차단한 사용자의 글은 목록에서 제외)
        List<Long> blockedUserIds = (userId != null)
                ? blockRepository.findBlockedUserIdsByBlockerId(userId)
                : List.of();

        // 3. 검색 조건 조합 및 조회 : 게시글 유형, 카테고리, 대학ID, 차단 여부 기반으로 조회
        // 유형을 지정하지 않으면 요청글을 모르는 기존 클라이언트를 위해 공급글만 조회한다.
        // allTypes=true 이면 유형 조건 없이 공급글·요청글을 함께 조회한다 (홈 통합 피드용, null = 유형 필터 없음).
        PostType postType = cond.isAllTypes() ? null
                : (cond.getPostType() != null ? cond.getPostType() : PostType.SUPPLY);
        Specification<Item> spec = Specification.allOf(
                ItemSpecs.withPostType(postType),
                ItemSpecs.withCategory(cond.getCategory()),
                ItemSpecs.withUniversity(univId),
                ItemSpecs.excludeBlockedUsers(blockedUserIds)
        );


        // 카테고리조건 + 정렬
        Page<Item> items = itemListRepository.findAll(spec, sortedPageable);

        return items.map(item -> ItemListResDto.fromEntity(item, userId));
    }
}
