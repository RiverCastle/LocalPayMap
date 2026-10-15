package com.localpaymap.controller.admin;

import com.localpaymap.dto.DeleteAllDuplicatesRequest;
import com.localpaymap.dto.DeleteStoresRequest;
import com.localpaymap.dto.DuplicatePageResponse;
import com.localpaymap.dto.DuplicateSummaryResponse;
import com.localpaymap.dto.StoreResponse;
import com.localpaymap.dto.StoreUpsertRequest;
import com.localpaymap.service.StoreService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/stores")
public class AdminStoreApiController {

    private final StoreService storeService;

    public AdminStoreApiController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<StoreResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return storeService.listAll(PageRequest.of(page, size));
    }

    @GetMapping("/duplicates")
    public DuplicatePageResponse duplicates(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return storeService.listDuplicates(page, size);
    }

    @GetMapping("/duplicates/summary")
    public DuplicateSummaryResponse duplicateSummary() {
        return storeService.summarizeDuplicates();
    }

    @PostMapping("/duplicates/delete-all")
    public Map<String, Integer> deleteAllDuplicates(@Valid @RequestBody DeleteAllDuplicatesRequest request) {
        return Map.of("deletedCount", storeService.deleteAllDuplicates(request.expectedDeleteCount()));
    }

    @PostMapping("/duplicates/delete")
    public Map<String, Integer> deleteDuplicates(@Valid @RequestBody DeleteStoresRequest request) {
        return Map.of("deletedCount", storeService.deleteAll(request.ids()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse create(@Valid @RequestBody StoreUpsertRequest request) {
        return storeService.createManual(request);
    }

    @PutMapping("/{id}")
    public StoreResponse update(@PathVariable Long id, @Valid @RequestBody StoreUpsertRequest request) {
        return storeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        storeService.delete(id);
    }
}
