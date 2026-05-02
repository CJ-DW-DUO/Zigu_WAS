package com.zigu.ziguwas.domains.search.repository;

import com.zigu.ziguwas.domains.search.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    /**
     * 사용자의 검색 기록을 마지막 검색 시간 기준 내림차순(최신순)으로 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 최신순으로 정렬된 검색 기록 리스트
     */
    List<SearchHistory> findByUserIdOrderByLastSearchedAtDesc(Long userId);

    /**
     * 사용자의 기록 중 가장 오랫동안 검색하지 않은 데이터 하나를 조회합니다. (FIFO 삭제용)
     *
     * @param userId 사용자 ID
     * @return 가장 오래된 검색 기록
     */
    Optional<SearchHistory> findFirstByUserIdOrderByLastSearchedAtAsc(Long userId);

    /**
     * 특정 사용자가 이미 검색했던 단어인지 확인합니다. (중복 체크용)
     */
    List<SearchHistory> findByUserIdAndSearchName(Long userId, String searchName);

    /**
     * 특정 사용자의 검색 기록 개수를 조회합니다.
     */
    long countByUserId(Long userId);

    /**
     * 특정 사용자의 모든 검색 기록을 삭제합니다.
     *
     * @param userId 삭제할 사용자 ID
     */
    void deleteAllByUserId(Long userId);

}
