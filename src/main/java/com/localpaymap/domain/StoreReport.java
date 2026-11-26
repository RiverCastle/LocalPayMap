package com.localpaymap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 가맹점 제보. 한 사용자는 한 가맹점에 같은 종류의 제보를 한 번만 남길 수 있다. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "store_report",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "storeId", "type"}),
        indexes = @Index(name = "idx_store_report_store", columnList = "storeId"))
public class StoreReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Long userId;

    // MySQL ENUM 컬럼이 되면 종류를 추가할 때 ddl-auto=update가 반영하지 못하므로 VARCHAR로 고정한다.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ReportType type;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public StoreReport(Long storeId, Long userId, ReportType type) {
        this.storeId = storeId;
        this.userId = userId;
        this.type = type;
    }

    /** 중복 가맹점을 정리할 때 남는 가맹점으로 옮긴다. */
    public void moveTo(Long storeId) {
        this.storeId = storeId;
    }
}
