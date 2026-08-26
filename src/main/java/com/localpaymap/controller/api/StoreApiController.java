package com.localpaymap.controller.api;

import com.localpaymap.dto.StoreResponse;
import com.localpaymap.dto.StoreSearchResponse;
import com.localpaymap.service.StoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
public class StoreApiController {

    private final StoreService storeService;

    public StoreApiController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public StoreSearchResponse search(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam(required = false) Long currencyTypeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") int zoom) {
        return storeService.searchInBounds(swLat, swLng, neLat, neLng, currencyTypeId, category, keyword, zoom);
    }

    @GetMapping("/{id}")
    public StoreResponse getById(@PathVariable Long id) {
        return storeService.getById(id);
    }
}
