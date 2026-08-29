package com.zigu.ziguwas.config;


import com.zigu.ziguwas.exception.CustomAccessDeniedHandler;
import com.zigu.ziguwas.exception.CustomAuthenticationEntryPoint;
import com.zigu.ziguwas.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/user/login").permitAll()
                        .requestMatchers("/api/v1/user/signup/**").permitAll()
                        .requestMatchers("/api/v1/universities/**").permitAll()
                        // /api/v1/auth/** 는 기본적으로 permitAll이지만, 아래 4개는 실제로는
                        // @AuthenticationPrincipal(인증 필요)을 사용하므로 명시적으로 인증을 요구한다.
                        // 로그인 하지 않은 유저가 로그아웃, 비밀번호변경, 회원탈퇴 등을 시도할 경우의 오류 방지를 위함.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/delcheck").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/auth/password").authenticated()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/email/**").permitAll()
                        .requestMatchers("/api/v1/verify/**").permitAll()
                        .requestMatchers("/api/v1/openai/**").permitAll()
                        .requestMatchers("/ws-stomp/**").permitAll() // 웹소켓 핸드쉐이크 허용
                        .requestMatchers("/swagger-resources/**", "/swagger-ui/**", "/v3/api-docs/**",
                                "/webjars/**", "/error").permitAll()
                        .requestMatchers("/view/**").permitAll()
                        .requestMatchers("/css/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
