package com.localpaymap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 소셜 로그인으로 가입한 일반 사용자. 관리자(AdminUser)와는 별도 테이블/체인으로 분리한다. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerId"}))
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false, length = 100)
    private String providerId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(length = 500)
    private String profileImage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    public AppUser(String provider, String providerId, String nickname, String profileImage) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }
}
