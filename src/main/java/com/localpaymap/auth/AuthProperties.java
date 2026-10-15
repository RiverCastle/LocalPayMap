package com.localpaymap.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(boolean cookieSecure, Jwt jwt, Kakao kakao) {

    public record Jwt(String secret, Duration accessTtl, Duration refreshTtl) {}

    public record Kakao(String clientId, String clientSecret) {}
}
