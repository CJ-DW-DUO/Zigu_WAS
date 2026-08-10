package com.zigu.ziguwas.domains.notification.service;

import com.zigu.ziguwas.domains.notification.dto.request.PushTokenRegisterReqDto;
import com.zigu.ziguwas.domains.notification.entity.PushToken;
import com.zigu.ziguwas.domains.notification.repository.PushTokenRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;

    /**
     * 푸시 토큰을 등록합니다.
     * 이미 등록된 토큰이면(같은 기기 재로그인 등) 새로 만들지 않고 소유자/플랫폼만 갱신합니다.
     *
     * @param userId 로그인 사용자 ID
     * @param dto 등록할 토큰 정보
     */
    @Transactional
    public void register(Long userId, PushTokenRegisterReqDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        pushTokenRepository.findByToken(dto.getToken())
                .ifPresentOrElse(
                        existing -> existing.reassign(user, dto.getPlatform()),
                        () -> pushTokenRepository.save(PushToken.create(user, dto.getToken(), dto.getPlatform()))
                );
    }

    /**
     * 로그아웃/토큰 무효화 시 본인 소유 토큰을 삭제합니다.
     * 이미 삭제되었거나 존재하지 않는 토큰이어도 에러 없이 무시합니다(멱등 처리).
     *
     * @param userId 로그인 사용자 ID
     * @param token 삭제할 토큰
     */
    @Transactional
    public void delete(Long userId, String token) {
        pushTokenRepository.deleteByTokenAndUserId(token, userId);
    }
}
