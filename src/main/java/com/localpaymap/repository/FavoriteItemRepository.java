package com.localpaymap.repository;

import com.localpaymap.domain.FavoriteItem;
import com.localpaymap.domain.Store;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {

    /** 가맹점이 이미 사라진 항목(삭제 시 정리하기 전에 생긴 데이터)은 목록과 개수에서 제외한다. */
    @Query("select i from FavoriteItem i where i.groupId in :groupIds "
            + "and exists (select s.id from Store s where s.id = i.storeId)")
    List<FavoriteItem> findByGroupIdIn(@Param("groupIds") Collection<Long> groupIds);

    @Query("select count(i) from FavoriteItem i where i.groupId = :groupId "
            + "and exists (select s.id from Store s where s.id = i.storeId)")
    long countByGroupId(@Param("groupId") Long groupId);

    boolean existsByGroupIdAndStoreId(Long groupId, Long storeId);

    List<FavoriteItem> findByStoreId(Long storeId);

    @Query("select distinct i.storeId from FavoriteItem i where i.storeId in :storeIds")
    List<Long> findReferencedStoreIds(@Param("storeIds") Collection<Long> storeIds);

    @Modifying
    @Query("delete from FavoriteItem i where i.groupId = :groupId and i.storeId = :storeId")
    int deleteByGroupIdAndStoreId(@Param("groupId") Long groupId, @Param("storeId") Long storeId);

    @Modifying
    @Query("delete from FavoriteItem i where i.groupId = :groupId")
    int deleteByGroupId(@Param("groupId") Long groupId);

    @Query("select s from Store s left join fetch s.currencyType "
            + "where s.id in (select i.storeId from FavoriteItem i where i.groupId = :groupId) order by s.name")
    List<Store> findStoresByGroupId(@Param("groupId") Long groupId);
}
