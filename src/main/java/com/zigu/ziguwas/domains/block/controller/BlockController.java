package com.zigu.ziguwas.domains.block.controller;

import com.zigu.ziguwas.domains.block.dto.response.BlockedUserResDto;
import com.zigu.ziguwas.domains.block.service.BlockService;
import com.zigu.ziguwas.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    /**
     * 사용자를 차단합니다.
     * 차단하면 상대방의 게시물이 목록/상세조회에서 보이지 않고, 서로 채팅을 주고받을 수 없게 됩니다.
     *
     * @param blockedUserId 차단할 사용자 ID
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     */
    @PostMapping("/{blockedUserId}")
    public ResponseEntity<Void> blockUser(
            @PathVariable Long blockedUserId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        blockService.blockUser(customUserDetails.getUserId(), blockedUserId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 사용자 차단을 해제합니다.
     *
     * @param blockedUserId 차단 해제할 사용자 ID
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     */
    @DeleteMapping("/{blockedUserId}")
    public ResponseEntity<Void> unblockUser(
            @PathVariable Long blockedUserId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        blockService.unblockUser(customUserDetails.getUserId(), blockedUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 내가 차단한 사용자 목록을 조회합니다.
     *
     * @param customUserDetails 현재 로그인한 사용자의 시큐리티 인증 정보
     * @return 차단한 사용자 목록
     */
    @GetMapping
    public ResponseEntity<List<BlockedUserResDto>> getBlockedUsers(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        return ResponseEntity.ok(blockService.getBlockedUsers(customUserDetails.getUserId()));
    }
}
