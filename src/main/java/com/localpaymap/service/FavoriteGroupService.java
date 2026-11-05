package com.localpaymap.service;

import com.localpaymap.controller.api.ApiException;
import com.localpaymap.domain.FavoriteGroup;
import com.localpaymap.domain.FavoriteItem;
import com.localpaymap.dto.FavoriteGroupRequest;
import com.localpaymap.dto.FavoriteGroupResponse;
import com.localpaymap.dto.StoreResponse;
import com.localpaymap.repository.FavoriteGroupRepository;
import com.localpaymap.repository.FavoriteItemRepository;
import com.localpaymap.repository.StoreRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 모든 조회/변경은 userId로 소유권을 확인한다. 남의 그룹은 "없는 그룹"(404)으로 취급해 존재 여부도 드러내지 않는다. */
@Service
@Transactional(readOnly = true)
public class FavoriteGroupService {

    static final int MAX_GROUPS_PER_USER = 20;
    static final int MAX_STORES_PER_GROUP = 500;
    private static final List<String> PALETTE =
            List.of("#e8590c", "#2f9e44", "#7048e8", "#d6336c", "#1098ad", "#f08c00", "#495057", "#c92a2a");

    private final FavoriteGroupRepository groupRepository;
    private final FavoriteItemRepository itemRepository;
    private final StoreRepository storeRepository;

    public FavoriteGroupService(
            FavoriteGroupRepository groupRepository,
            FavoriteItemRepository itemRepository,
            StoreRepository storeRepository) {
        this.groupRepository = groupRepository;
        this.itemRepository = itemRepository;
        this.storeRepository = storeRepository;
    }

    public List<FavoriteGroupResponse> list(Long userId) {
        List<FavoriteGroup> groups = groupRepository.findByUserIdOrderByIdAsc(userId);
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Long>> storeIdsByGroup = new HashMap<>();
        itemRepository
                .findByGroupIdIn(groups.stream().map(FavoriteGroup::getId).toList())
                .forEach(i -> storeIdsByGroup
                        .computeIfAbsent(i.getGroupId(), k -> new ArrayList<>())
                        .add(i.getStoreId()));
        return groups.stream()
                .map(g -> FavoriteGroupResponse.of(g, storeIdsByGroup.getOrDefault(g.getId(), List.of())))
                .toList();
    }

    public List<StoreResponse> listStores(Long userId, Long groupId) {
        FavoriteGroup group = ownedGroup(userId, groupId);
        return itemRepository.findStoresByGroupId(group.getId()).stream()
                .map(StoreResponse::from)
                .toList();
    }

    @Transactional
    public FavoriteGroupResponse create(Long userId, FavoriteGroupRequest request) {
        String name = normalize(request.name());
        long existing = groupRepository.countByUserId(userId);
        if (existing >= MAX_GROUPS_PER_USER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "그룹은 최대 " + MAX_GROUPS_PER_USER + "개까지 만들 수 있습니다.");
        }
        if (groupRepository.existsByUserIdAndName(userId, name)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 같은 이름의 그룹이 있습니다.");
        }
        String color = request.color() != null
                ? request.color().toLowerCase()
                : PALETTE.get((int) (existing % PALETTE.size()));
        try {
            FavoriteGroup saved = groupRepository.saveAndFlush(new FavoriteGroup(userId, name, color));
            return FavoriteGroupResponse.of(saved, List.of());
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 같은 이름의 그룹이 있습니다.");
        }
    }

    @Transactional
    public FavoriteGroupResponse update(Long userId, Long groupId, FavoriteGroupRequest request) {
        FavoriteGroup group = ownedGroup(userId, groupId);
        String name = normalize(request.name());
        if (!name.equals(group.getName()) && groupRepository.existsByUserIdAndName(userId, name)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 같은 이름의 그룹이 있습니다.");
        }
        group.setName(name);
        if (request.color() != null) {
            group.setColor(request.color().toLowerCase());
        }
        List<Long> storeIds = itemRepository.findByGroupIdIn(List.of(groupId)).stream()
                .map(FavoriteItem::getStoreId)
                .toList();
        return FavoriteGroupResponse.of(group, storeIds);
    }

    @Transactional
    public void delete(Long userId, Long groupId) {
        FavoriteGroup group = ownedGroup(userId, groupId);
        itemRepository.deleteByGroupId(group.getId());
        groupRepository.delete(group);
    }

    /** 이미 담긴 가맹점을 다시 담아도 오류 없이 성공(멱등)한다. */
    @Transactional
    public void addStore(Long userId, Long groupId, Long storeId) {
        FavoriteGroup group = ownedGroup(userId, groupId);
        if (!storeRepository.existsById(storeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 가맹점입니다.");
        }
        if (itemRepository.existsByGroupIdAndStoreId(group.getId(), storeId)) {
            return;
        }
        if (itemRepository.countByGroupId(group.getId()) >= MAX_STORES_PER_GROUP) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "한 그룹에는 최대 " + MAX_STORES_PER_GROUP + "곳까지 담을 수 있습니다.");
        }
        try {
            itemRepository.saveAndFlush(new FavoriteItem(group.getId(), storeId));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 요청이 들어와 유니크 제약에 걸린 경우도 이미 담긴 것이므로 성공으로 본다.
        }
    }

    @Transactional
    public void removeStore(Long userId, Long groupId, Long storeId) {
        FavoriteGroup group = ownedGroup(userId, groupId);
        itemRepository.deleteByGroupIdAndStoreId(group.getId(), storeId);
    }

    private FavoriteGroup ownedGroup(Long userId, Long groupId) {
        return groupRepository
                .findByIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 그룹입니다."));
    }

    private static String normalize(String name) {
        // 제어문자와 연속 공백을 정리해 이름만 미세하게 다른 중복 그룹이 생기는 것을 막는다.
        String cleaned = name.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "그룹 이름을 입력해 주세요.");
        }
        return cleaned;
    }
}
