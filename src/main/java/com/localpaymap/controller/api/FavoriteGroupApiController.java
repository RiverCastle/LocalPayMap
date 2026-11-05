package com.localpaymap.controller.api;

import com.localpaymap.dto.FavoriteGroupRequest;
import com.localpaymap.dto.FavoriteGroupResponse;
import com.localpaymap.dto.StoreResponse;
import com.localpaymap.service.FavoriteGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 사용자 본인의 즐겨찾기 그룹만 다룬다. (/api/me/** 는 보안 설정에서 인증을 요구한다) */
@RestController
@RequestMapping("/api/me/favorite-groups")
public class FavoriteGroupApiController {

    private final FavoriteGroupService service;

    public FavoriteGroupApiController(FavoriteGroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<FavoriteGroupResponse> list(JwtAuthenticationToken auth) {
        return service.list(userId(auth));
    }

    @PostMapping
    public ResponseEntity<FavoriteGroupResponse> create(
            JwtAuthenticationToken auth, @Valid @RequestBody FavoriteGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId(auth), request));
    }

    @PatchMapping("/{groupId}")
    public FavoriteGroupResponse update(
            JwtAuthenticationToken auth, @PathVariable Long groupId, @Valid @RequestBody FavoriteGroupRequest request) {
        return service.update(userId(auth), groupId, request);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(JwtAuthenticationToken auth, @PathVariable Long groupId) {
        service.delete(userId(auth), groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/stores")
    public List<StoreResponse> stores(JwtAuthenticationToken auth, @PathVariable Long groupId) {
        return service.listStores(userId(auth), groupId);
    }

    @PutMapping("/{groupId}/stores/{storeId}")
    public ResponseEntity<Void> addStore(
            JwtAuthenticationToken auth, @PathVariable Long groupId, @PathVariable Long storeId) {
        service.addStore(userId(auth), groupId, storeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}/stores/{storeId}")
    public ResponseEntity<Void> removeStore(
            JwtAuthenticationToken auth, @PathVariable Long groupId, @PathVariable Long storeId) {
        service.removeStore(userId(auth), groupId, storeId);
        return ResponseEntity.noContent().build();
    }

    private static Long userId(JwtAuthenticationToken auth) {
        return Long.valueOf(auth.getToken().getSubject());
    }
}
