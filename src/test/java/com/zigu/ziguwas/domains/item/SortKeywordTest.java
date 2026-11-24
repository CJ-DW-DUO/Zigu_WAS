package com.zigu.ziguwas.domains.item;

import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.item.dto.response.ItemSearchCond;
import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemListRepository;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.item.service.ItemListService;
import com.zigu.ziguwas.domains.search.dto.request.ItemSearchReqDto;
import com.zigu.ziguwas.domains.search.repository.SearchHistoryRepository;
import com.zigu.ziguwas.domains.search.service.SearchService;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 정렬 값 "조회수순"은 "인기순"과 같은 조회수 정렬로 인식하고, 모르는 값은 기존처럼 최신순이다. */
class SortKeywordTest {

    private static final Sort VIEW_COUNT = Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "id"));
    private static final Sort LATEST = Sort.by(Sort.Direction.DESC, "id");

    private Sort listSort(String keyword) {
        ItemListRepository repo = mock(ItemListRepository.class);
        when(repo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        ItemSearchCond cond = new ItemSearchCond();
        cond.setSort(keyword);
        new ItemListService(repo, mock(BlockRepository.class)).getItemList(cond, PageRequest.of(0, 12), 1L, 1L);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findAll(any(Specification.class), captor.capture());
        return captor.getValue().getSort();
    }

    private Sort searchSort(String keyword) {
        ItemRepository repo = mock(ItemRepository.class);
        when(repo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<Item>(List.of()));
        BlockRepository blockRepository = mock(BlockRepository.class);
        ItemSearchReqDto req = new ItemSearchReqDto();
        req.setSort(keyword);
        new SearchService(repo, mock(SearchHistoryRepository.class), mock(UserRepository.class), blockRepository)
                .searchItems(1L, 1L, req, PageRequest.of(0, 12));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findAll(any(Specification.class), captor.capture());
        return captor.getValue().getSort();
    }

    @Test
    void 목록_인기순과_조회수순은_같은_조회수_정렬() {
        assertEquals(VIEW_COUNT, listSort("인기순"));
        assertEquals(VIEW_COUNT, listSort("조회수순"));
    }

    @Test
    void 목록_모르는_값과_없는_값은_최신순() {
        assertEquals(LATEST, listSort("최신순"));
        assertEquals(LATEST, listSort("엉뚱한값"));
        assertEquals(LATEST, listSort(null));
    }

    @Test
    void 검색_인기순과_조회수순은_같은_조회수_정렬() {
        assertEquals(VIEW_COUNT, searchSort("인기순"));
        assertEquals(VIEW_COUNT, searchSort("조회수순"));
    }

    @Test
    void 검색_모르는_값과_없는_값은_최신순() {
        assertEquals(LATEST, searchSort("엉뚱한값"));
        assertEquals(LATEST, searchSort(null));
    }
}
