package com.zigu.ziguwas.domains.search.service;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.repository.ItemRepository;
import com.zigu.ziguwas.domains.search.dto.request.ItemSearchReqDto;
import com.zigu.ziguwas.domains.search.dto.response.ItemSearchResDto;
import com.zigu.ziguwas.domains.search.dto.response.SearchHistoryResDto;
import com.zigu.ziguwas.domains.search.entity.SearchHistory;
import com.zigu.ziguwas.domains.search.repository.SearchHistoryRepository;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Builder
@Transactional(readOnly = true)
public class SearchService {

    private final ItemRepository itemRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    /**
     * 키워드, 카테고리, 정렬 조건을 결합하여 아이템을 페이징 검색합니다.
     *
     * @param userId 사용자 식별자
     * @param reqDto 검색 요청 데이터
     * @param pageable 페이징 정보
     * @return 페이징 및 정렬된 검색 결과
     */
    @Transactional
    public Page<ItemSearchResDto> searchItems(Long userId, ItemSearchReqDto reqDto, Pageable pageable) {
        // 1. 검색 히스토리 저장
        if (reqDto.getKeyword() != null && !reqDto.getKeyword().isBlank()) {
            saveSearchHistory(userId, reqDto.getKeyword().trim());
        }

        // 2. 정렬 조건 결정
        Sort sort = getSortOrder(reqDto.getSort());

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // 3. 검색 수행
        Page<Item> items;
        if (reqDto.getCategory() == null) {
            items = itemRepository.findByTitleContainingIgnoreCase(reqDto.getKeyword(), sortedPageable);
        } else {
            items = itemRepository.findByTitleContainingIgnoreCaseAndCategory(
                    reqDto.getKeyword(),
                    reqDto.getCategory(),
                    sortedPageable
            );
        }

        return items.map(ItemSearchResDto::fromEntity);
    }

    /**
     * 문자열 정렬 조건을 JPA Sort 객체로 변환합니다.
     */
    private Sort getSortOrder(String sortType) {
        if (sortType == null) return Sort.by(Sort.Direction.DESC, "id");

        return switch (sortType) {
            // 인기순으로 정렬하되 같다면 id 순으로 보여줌
            case "인기순" -> Sort.by(Sort.Direction.DESC, "viewCount")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
            case "낮은가격순" -> Sort.by(Sort.Direction.ASC, "dayPerPrice")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
            case "최신순" -> Sort.by(Sort.Direction.DESC, "id");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }

    /**
     * 사용자의 검색 기록 목록을 최신 활동순으로 조회합니다.
     */
    public List<SearchHistoryResDto> getSearchHistory(Long userId) {
        return searchHistoryRepository.findByUserIdOrderByLastSearchedAtDesc(userId).stream()
                .map(SearchHistoryResDto::fromEntity)
                .toList();
    }

    /**
     * 검색 키워드를 저장하거나 갱신합니다.
     *
     */
    private void saveSearchHistory(Long userId, String keyword) {
        // 1. 해당 유저의 특정 키워드 기록을 모두 가져옴
        List<SearchHistory> historyList = searchHistoryRepository.findByUserIdAndSearchName(userId, keyword);

        if (!historyList.isEmpty()) {
            // 2. 검색어가 존재한다면 모든 항목의 시간을 업데이트
            historyList.forEach(SearchHistory::updateTime);
            return;
        }

        // 3. 기록이 20개 넘으면 오래된 것 삭제
        long count = searchHistoryRepository.countByUserId(userId);
        if (count >= 20) {
            searchHistoryRepository.findFirstByUserIdOrderByLastSearchedAtAsc(userId)
                    .ifPresent(searchHistoryRepository::delete);
        }

        // 4. 새 검색어 저장
        searchHistoryRepository.save(SearchHistory.builder()
                .user(userRepository.getReferenceById(userId))
                .searchName(keyword)
                .build());
    }

    /**
     * 특정 검색 기록 하나를 삭제합니다.
     *
     * @param userId 현재 사용자 식별자
     * @param searchId 삭제할 검색 기록의 ID
     */
    @Transactional
    public void deleteSearchHistoryById(Long userId, Long searchId) {
        // 1. 먼저 기록을 찾음
        SearchHistory history = searchHistoryRepository.findById(searchId)
                .orElseThrow(() -> new CustomException(ErrorCode.SEARCH_HISTORY_NOT_FOUND));

        // 2. 권한(소유권) 체크
        if (!history.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 3. 삭제 수행
        searchHistoryRepository.delete(history);
    }

    /**
     * 사용자의 모든 검색 기록을 삭제합니다.
     */
    @Transactional
    public void clearSearchHistory(Long userId) {
        searchHistoryRepository.deleteAllByUserId(userId);
    }
}