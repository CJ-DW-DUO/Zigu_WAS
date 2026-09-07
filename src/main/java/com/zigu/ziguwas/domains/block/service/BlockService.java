package com.zigu.ziguwas.domains.block.service;

import com.zigu.ziguwas.domains.block.dto.response.BlockedUserResDto;
import com.zigu.ziguwas.domains.block.entity.Block;
import com.zigu.ziguwas.domains.block.repository.BlockRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    /**
     * 사용자를 차단합니다.
     *
     * @param blockerId 차단을 실행하는 사용자의 식별자
     * @param blockedId 차단 대상 사용자의 식별자
     * @throws CustomException
     * - SELF_BLOCK_NOT_ALLOWED: 자기 자신을 차단하려는 경우
     * - USER_NOT_FOUND: 차단 대상 사용자가 존재하지 않는 경우
     * - ALREADY_BLOCKED_USER: 이미 차단한 사용자인 경우
     */
    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {

        if (blockerId.equals(blockedId)) {
            throw new CustomException(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new CustomException(ErrorCode.ALREADY_BLOCKED_USER);
        }

        blockRepository.save(Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build());
    }

    /**
     * 사용자 차단을 해제합니다.
     *
     * @param blockerId 차단을 해제하려는 사용자의 식별자
     * @param blockedId 차단 해제 대상 사용자의 식별자
     * @throws CustomException BLOCK_NOT_FOUND: 차단 내역이 존재하지 않을 경우
     */
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new CustomException(ErrorCode.BLOCK_NOT_FOUND));

        blockRepository.delete(block);
    }

    /**
     * 사용자가 차단한 사용자 목록을 조회합니다.
     *
     * @param blockerId 조회를 요청한 사용자의 식별자
     * @return 차단한 사용자 목록 (최근 차단순)
     */
    public List<BlockedUserResDto> getBlockedUsers(Long blockerId) {
        return blockRepository.findAllByBlockerIdOrderByCreatedAtDesc(blockerId).stream()
                .map(BlockedUserResDto::fromEntity)
                .toList();
    }
}
