package com.zigu.ziguwas.domains.block.repository;

import com.zigu.ziguwas.domains.block.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<Block> findAllByBlockerIdOrderByCreatedAtDesc(Long blockerId);

    /**
     * 특정 사용자가 차단한 상대방들의 ID 목록을 조회합니다.
     * 게시물 목록에서 차단한 사용자의 글을 제외할 때 사용합니다.
     *
     * @param blockerId 차단을 실행한 사용자 ID
     * @return 차단당한 사용자 ID 목록
     */
    @Query("select b.blocked.id from Block b where b.blocker.id = :blockerId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);

    /**
     * 두 사용자 사이에 어느 한쪽이라도 상대를 차단했는지 확인합니다.
     * 채팅방 생성/메시지 전송 시 양방향 차단 여부를 검증할 때 사용합니다.
     *
     * @param userId1 사용자1 ID
     * @param userId2 사용자2 ID
     * @return 둘 중 한쪽이라도 차단 관계가 존재하면 true
     */
    @Query("select case when count(b) > 0 then true else false end from Block b " +
            "where (b.blocker.id = :userId1 and b.blocked.id = :userId2) " +
            "or (b.blocker.id = :userId2 and b.blocked.id = :userId1)")
    boolean existsBlockBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
