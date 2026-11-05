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

/** 사용자가 만든 즐겨찾기 그룹. 같은 사용자 안에서 이름은 유일하다. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "favorite_group", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "name"}))
public class FavoriteGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    /** 지도 마커 색상 (#RRGGBB) */
    @Column(nullable = false, length = 7)
    private String color;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FavoriteGroup(Long userId, String name, String color) {
        this.userId = userId;
        this.name = name;
        this.color = color;
    }
}
