package com.localpaymap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 그룹에 담긴 가맹점. 한 가맹점은 여러 그룹에 담길 수 있지만 같은 그룹에는 한 번만 담긴다. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "favorite_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"groupId", "storeId"}),
        indexes = @Index(name = "idx_favorite_item_store", columnList = "storeId"))
public class FavoriteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private Long storeId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FavoriteItem(Long groupId, Long storeId) {
        this.groupId = groupId;
        this.storeId = storeId;
    }
}
