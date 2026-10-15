package com.localpaymap.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

    private static final int MIN_SECRET_BYTES = 32;

    static SecretKey secretKey(AuthProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("auth.jwt.secret(JWT_SECRET)은 32바이트 이상이어야 합니다.");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(AuthProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(props)));
    }

    @Bean
    public JwtDecoder jwtDecoder(AuthProperties props) {
        return NimbusJwtDecoder.withSecretKey(secretKey(props))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** 키가 설정된 제공자만 등록한다. 키가 없으면 해당 제공자 로그인만 비활성화되고 서버는 정상 기동한다. */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(AuthProperties props) {
        Map<String, ClientRegistration> registrations = new HashMap<>();
        AuthProperties.Kakao kakao = props.kakao();
        if (kakao != null && StringUtils.hasText(kakao.clientId())) {
            registrations.put(
                    "kakao",
                    ClientRegistration.withRegistrationId("kakao")
                            .clientId(kakao.clientId())
                            .clientSecret(kakao.clientSecret())
                            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                            .scope("profile_nickname", "profile_image")
                            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                            .tokenUri("https://kauth.kakao.com/oauth/token")
                            .userInfoUri("https://kapi.kakao.com/v2/user/me")
                            .userNameAttributeName("id")
                            .clientName("Kakao")
                            .build());
        }
        return registrations::get;
    }
}
