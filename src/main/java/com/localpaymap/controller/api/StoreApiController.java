package com.localpaymap.controller.api;

import com.localpaymap.dto.StoreResponse;
import com.localpaymap.dto.StoreSearchResponse;
import com.localpaymap.service.StoreService;
import java.util.Arrays;
import java.util.List;
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
            @RequestParam(required = false) List<String> categoryGroups,
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") int zoom) {
        return storeService.searchInBounds(
                swLat, swLng, neLat, neLng, currencyTypeId, categoryGroups, rawValues(request, "categories"), keyword, zoom);
    }

    /** 업종값에 쉼표가 들어 있어("축산물,정육점") 스프링의 쉼표 분리 변환을 피하려고 반복 파라미터를 원문 그대로 읽는다. */
    private static List<String> rawValues(jakarta.servlet.http.HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? List.of() : Arrays.asList(values);
    }

    @GetMapping("/{id}")
    public StoreResponse getById(@PathVariable Long id) {
        return storeService.getById(id);
    }
}
