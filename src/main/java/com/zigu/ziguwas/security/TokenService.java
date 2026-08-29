package com.zigu.ziguwas.security;

import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // Redis 저장 시 키 앞에 붙일 접두사 (구분용)
    private static final String RT_PREFIX = "REFTOKEN:";

    /**
     * 리프레시 토큰을 Redis에 저장 (TTL은 7일)
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        // JwtUtil의 refreshValid와 동일하게 7일
        redisService.setDataExpire(RT_PREFIX + userId, refreshToken, jwtUtil.getRefreshValid());
    }

    /**
     * 회원 탈퇴/로그아웃 시 Redis에서 리프레시 토큰 삭제
     */
    public void deleteRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        redisService.deleteData(RT_PREFIX + userId);
    }

    /**
     * 재발급 시 쿠키로 들어온 리프레시 토큰과 대조하기 위해 Redis에 저장된 값을 조회
     * (로그아웃/탈퇴로 이미 삭제됐다면 null이 반환되어 재발급이 거부됨)
     */
    public String getRefreshToken(Long userId) {
        return redisService.getData(RT_PREFIX + userId);
    }
}
