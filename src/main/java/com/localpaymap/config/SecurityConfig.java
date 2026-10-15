package com.localpaymap.config;

import com.localpaymap.auth.CookieJwtAuthenticationFilter;
import com.localpaymap.auth.CookieOAuth2AuthorizationRequestRepository;
import com.localpaymap.auth.OAuth2LoginSuccessHandler;
import com.localpaymap.security.CsrfCookieFilter;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 관리자: 기존과 동일한 세션 + 폼 로그인. */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth.requestMatchers("/admin/login")
                        .permitAll()
                        .anyRequest()
                        .hasRole("ADMIN"))
                .formLogin(form -> form.loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin/stores", true)
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/admin/logout").logoutSuccessUrl("/").permitAll())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
        return http.build();
    }

    /** 일반 사용자: 소셜 로그인 후 JWT(HttpOnly 쿠키 또는 Bearer 헤더)로 인증하는 stateless 체인. */
    @Bean
    @Order(2)
    public SecurityFilterChain userFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            OAuth2LoginSuccessHandler successHandler,
            CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository)
            throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authorities);

        RequestMatcher bearerHeader = request -> request.getHeader("Authorization") != null;

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/me")
                        .permitAll()
                        // 로그인이 필요한 개인화 기능(추후 추가될 즐겨찾기/후기/제보 등)은 /api/me/** 아래에 둔다.
                        .requestMatchers("/api/me/**")
                        .authenticated()
                        .requestMatchers("/", "/index.html", "/error", "/css/**", "/js/**", "/images/**", "/api/**")
                        .permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/favicon.ico")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(o -> o.authorizationEndpoint(
                                a -> a.authorizationRequestRepository(authorizationRequestRepository))
                        .successHandler(successHandler)
                        .failureUrl("/?loginError"))
                .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtConverter)))
                .addFilterBefore(
                        new CookieJwtAuthenticationFilter(jwtDecoder, jwtConverter), BearerTokenAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // 쿠키 인증(CookieJwtAuthenticationFilter)은 CSRF 방어가 필요하다. Authorization 헤더 요청은 브라우저가 자동 첨부하지 않으므로 제외.
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(bearerHeader))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
        return http.build();
    }
}
