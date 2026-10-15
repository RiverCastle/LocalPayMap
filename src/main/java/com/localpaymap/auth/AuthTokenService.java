package com.localpaymap.auth;

import com.localpaymap.domain.AppUser;
import com.localpaymap.domain.RefreshToken;
import com.localpaymap.repository.AppUserRepository;
import com.localpaymap.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final AuthProperties props;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository appUserRepository;

    public AuthTokenService(
            JwtEncoder jwtEncoder,
            AuthProperties props,
            RefreshTokenRepository refreshTokenRepository,
            AppUserRepository appUserRepository) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.refreshTokenRepository = refreshTokenRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public TokenPair issue(AppUser user) {
        return new TokenPair(createAccessToken(user), createRefreshToken(user.getId()));
    }

    /**
     * refresh 토큰을 검증하고 새 토큰 쌍으로 교체한다. 이미 폐기된 토큰이 다시 제시되면 탈취로 간주해 해당 사용자의 모든
     * refresh 토큰을 폐기한다. (예외로 롤백되지 않도록 Optional로 실패를 알린다.)
     */
    @Transactional
    public Optional<TokenPair> rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return Optional.empty();
        }
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken stored = found.get();
        LocalDateTime now = LocalDateTime.now();
        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(stored.getUserId(), now);
            return Optional.empty();
        }
        if (stored.getExpiresAt().isBefore(now)) {
            return Optional.empty();
        }
        Optional<AppUser> user = appUserRepository.findById(stored.getUserId());
        if (user.isEmpty()) {
            return Optional.empty();
        }
        stored.setRevokedAt(now);
        return Optional.of(issue(user.get()));
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(t -> {
            if (!t.isRevoked()) {
                t.setRevokedAt(LocalDateTime.now());
            }
        });
    }

    private String createAccessToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(props.jwt().accessTtl()))
                .claim("nickname", user.getNickname())
                .claim("roles", List.of("ROLE_USER"))
                .build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String createRefreshToken(Long userId) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime expiresAt = LocalDateTime.now().plus(props.jwt().refreshTtl());
        refreshTokenRepository.save(new RefreshToken(userId, hash(raw), expiresAt));
        return raw;
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
