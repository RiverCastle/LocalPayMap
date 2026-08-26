package com.localpaymap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "currency_type", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class CurrencyType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String regionName;

    @Column(nullable = false, length = 100)
    private String currencyName;

    @Column(nullable = false, length = 50)
    private String code;

    public CurrencyType(String regionName, String currencyName, String code) {
        this.regionName = regionName;
        this.currencyName = currencyName;
        this.code = code;
    }
}
