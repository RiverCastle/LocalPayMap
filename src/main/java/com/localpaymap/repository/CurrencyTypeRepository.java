package com.localpaymap.repository;

import com.localpaymap.domain.CurrencyType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyTypeRepository extends JpaRepository<CurrencyType, Long> {
    java.util.Optional<CurrencyType> findByCode(String code);
}
