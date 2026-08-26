package com.localpaymap.controller.api;

import com.localpaymap.dto.CurrencyTypeResponse;
import com.localpaymap.repository.CurrencyTypeRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/currency-types")
public class CurrencyTypeApiController {

    private final CurrencyTypeRepository currencyTypeRepository;

    public CurrencyTypeApiController(CurrencyTypeRepository currencyTypeRepository) {
        this.currencyTypeRepository = currencyTypeRepository;
    }

    @GetMapping
    public List<CurrencyTypeResponse> list() {
        return currencyTypeRepository.findAll().stream().map(CurrencyTypeResponse::from).toList();
    }
}
