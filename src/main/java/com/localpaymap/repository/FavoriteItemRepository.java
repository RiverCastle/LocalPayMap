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

    List<FavoriteItem> findByGroupIdIn(Collection<Long> groupIds);

    long countByGroupId(Long groupId);

    boolean existsByGroupIdAndStoreId(Long groupId, Long storeId);

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
