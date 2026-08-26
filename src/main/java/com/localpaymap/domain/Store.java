package com.localpaymap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "store",
        indexes = {
            @Index(name = "idx_store_lat_lng", columnList = "lat,lng"),
            @Index(name = "idx_store_source_external_id", columnList = "source,externalId")
        })
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String roadAddress;

    @Column(length = 300)
    private String jibunAddress;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_type_id")
    private CurrencyType currencyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BizStatus bizStatus = BizStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreSource source;

    @Column(length = 100)
    private String externalId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Store(
            String name,
            String roadAddress,
            String jibunAddress,
            Double lat,
            Double lng,
            String phone,
            String category,
            CurrencyType currencyType,
            BizStatus bizStatus,
            StoreSource source,
            String externalId) {
        this.name = name;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.lat = lat;
        this.lng = lng;
        this.phone = phone;
        this.category = category;
        this.currencyType = currencyType;
        this.bizStatus = bizStatus;
        this.source = source;
        this.externalId = externalId;
    }

    public void updateFrom(Store other) {
        this.name = other.name;
        this.roadAddress = other.roadAddress;
        this.jibunAddress = other.jibunAddress;
        this.lat = other.lat;
        this.lng = other.lng;
        this.phone = other.phone;
        this.category = other.category;
        this.currencyType = other.currencyType;
        this.bizStatus = other.bizStatus;
    }
}
