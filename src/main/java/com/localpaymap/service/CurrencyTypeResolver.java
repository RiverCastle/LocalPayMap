package com.localpaymap.service;

import com.localpaymap.domain.CurrencyType;
import com.localpaymap.repository.CurrencyTypeRepository;
import org.springframework.stereotype.Component;

/** 지역명+지역화폐명으로 CurrencyType을 찾고, 없으면 새로 만든다. */
@Component
public class CurrencyTypeResolver {

    private final CurrencyTypeRepository currencyTypeRepository;

    public CurrencyTypeResolver(CurrencyTypeRepository currencyTypeRepository) {
        this.currencyTypeRepository = currencyTypeRepository;
    }

    public CurrencyType resolve(String regionName, String currencyName) {
        String region = regionName == null ? "" : regionName.trim();
        String currency = (currencyName == null || currencyName.isBlank()) ? "미분류 지역화폐" : currencyName.trim();
        String code = (region + "_" + currency).replaceAll("\\s+", "_");

        return currencyTypeRepository
                .findByCode(code)
                .orElseGet(() -> currencyTypeRepository.save(new CurrencyType(region, currency, code)));
    }
}
