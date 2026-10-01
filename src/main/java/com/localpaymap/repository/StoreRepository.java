package com.localpaymap.repository;

import com.localpaymap.domain.Store;
import com.localpaymap.domain.StoreSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findBySourceAndExternalId(StoreSource source, String externalId);

    long countBySource(StoreSource source);

    @Query("select s.category as category, count(s) as cnt from Store s "
            + "where s.category is not null group by s.category")
    java.util.List<CategoryCountProjection> countByCategory();

    @Query(
            "select s from Store s "
                    + "where s.lat between :swLat and :neLat "
                    + "and s.lng between :swLng and :neLng "
                    + "and (:currencyTypeId is null or s.currencyType.id = :currencyTypeId) "
                    + "and (:categoryCount = 0 or s.category in :categories) "
                    + "and (:keyword is null or s.name like concat('%', :keyword, '%'))")
    java.util.List<Store> searchInBounds(
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng,
            @Param("currencyTypeId") Long currencyTypeId,
            @Param("categoryCount") int categoryCount,
            @Param("categories") java.util.List<String> categories,
            @Param("keyword") String keyword);

    @Query(
            "select count(s) from Store s "
                    + "where s.lat between :swLat and :neLat "
                    + "and s.lng between :swLng and :neLng "
                    + "and (:currencyTypeId is null or s.currencyType.id = :currencyTypeId) "
                    + "and (:categoryCount = 0 or s.category in :categories) "
                    + "and (:keyword is null or s.name like concat('%', :keyword, '%'))")
    long countInBounds(
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng,
            @Param("currencyTypeId") Long currencyTypeId,
            @Param("categoryCount") int categoryCount,
            @Param("categories") java.util.List<String> categories,
            @Param("keyword") String keyword);

    @Query(
            value = "select gx, gy, avg(lat) as avgLat, avg(lng) as avgLng, count(*) as cnt "
                    + "from ("
                    + "  select lat, lng, floor(lat / :cellSize) as gx, floor(lng / :cellSize) as gy "
                    + "  from store "
                    + "  where lat between :swLat and :neLat "
                    + "  and lng between :swLng and :neLng "
                    + "  and (:currencyTypeId is null or currency_type_id = :currencyTypeId) "
                    + "  and (:categoryCount = 0 or category in (:categories)) "
                    + "  and (:keyword is null or name like concat('%', :keyword, '%'))"
                    + ") grid "
                    + "group by gx, gy",
            nativeQuery = true)
    java.util.List<StoreClusterProjection> clusterInBounds(
            @Param("swLat") double swLat,
            @Param("swLng") double swLng,
            @Param("neLat") double neLat,
            @Param("neLng") double neLng,
            @Param("currencyTypeId") Long currencyTypeId,
            @Param("categoryCount") int categoryCount,
            @Param("categories") java.util.List<String> categories,
            @Param("keyword") String keyword,
            @Param("cellSize") double cellSize);
}
