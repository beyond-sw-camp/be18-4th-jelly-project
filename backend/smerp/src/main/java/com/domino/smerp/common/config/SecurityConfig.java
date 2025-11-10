package com.domino.smerp.common.config;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .cors(customizer -> customizer.configurationSource(getCorsConfigurationSource()))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/api/v1/auth/**") // 로그인/로그아웃/me는 모두 허용 (인증 전에도 접근 가능)
                                .permitAll()
                                // User Controller에 대한 인가 규칙 추가
                                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/**") // 사용자 수정
                                .hasAnyRole("ADMIN", "MANAGER") // ADMIN 또는 MANAGER만 허용
                                .requestMatchers(HttpMethod.POST, "/api/v1/users") // 사용자 생성
                                .hasAnyRole("ADMIN", "MANAGER") // ADMIN 또는 MANAGER만 허용
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**") // 사용자 삭제
                                .hasRole("ADMIN") // ADMIN만 허용
                                .anyRequest()
                                .authenticated() // 그 외의 모든 요청은 인증된 사용자에게만 허용
                        )
                .anonymous(anonymous -> anonymous.principal("SYSTEM"))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(sessionManagement -> sessionManagement.maximumSessions(1));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

    private static CorsConfigurationSource getCorsConfigurationSource() {

        return (request) -> {
            CorsConfiguration corsConfiguration = new CorsConfiguration();

            corsConfiguration.setAllowedOriginPatterns(List.of("*"));

            corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "DELETE", "OPTIONS"));

            corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

            corsConfiguration.setExposedHeaders(List.of("Authorization"));

            corsConfiguration.setAllowCredentials(true);

            corsConfiguration.setMaxAge(3600L);

            return corsConfiguration;
        };
    }
}
