package com.zigu.ziguwas.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    //    @Value("${jwt.secret-key}")
    private final SecretKey secretKey;

    private final long accessValid = 1000L * 60 * 60; // 1시간
    @Getter
    private final long refreshValid = 1000L * 60 * 60 * 24 * 7; // 7일

    public JwtUtil(@Value("${jwt.secret-key}") String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    // 1. Access 토큰 발급
    public String createAccessToken(String email) {
        return generateToken(email, accessValid, ACCESS_TOKEN_TYPE);
    }
    // 2. Refresh 토큰 발급
    public String createRefreshToken(String email) {
        return generateToken(email, refreshValid, REFRESH_TOKEN_TYPE);
    }

    // 토큰 발급 종합
    private String generateToken(String email, long validTime, String type) {
        return Jwts.builder()
                .subject(email)
                .claim(TOKEN_TYPE_CLAIM, type) // 토큰 페이로드에 토큰 타입을 추가
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + validTime))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    // 리프레시 토큰인지 확인 (재발급 엔드포인트에서 access 토큰이 잘못 들어오는 것을 방지)
    public boolean isRefreshToken(String token) {
        String type = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get(TOKEN_TYPE_CLAIM, String.class);
        return REFRESH_TOKEN_TYPE.equals(type);
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }
        return null;
    }

    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public long getExpiration(String token) {
        // 토큰의 전체 페이로드에서 만료 시간(Expiration)을 가져옴
        Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        // 현재 시간과의 차이를 계산 (밀리초 단위)
        long now = new Date().getTime();
        long diff = expiration.getTime() - now;
        return diff > 0 ? diff : 0L; // 0보다 작으면 그냥 0 반환
    }


}
