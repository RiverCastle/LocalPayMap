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

    @Query("select s.name as name, s.roadAddress as roadAddress, c.id as currencyTypeId, count(s) as cnt "
            + "from Store s left join s.currencyType c "
            + "where s.roadAddress is not null "
            + "group by s.name, s.roadAddress, c.id "
            + "having count(s) > 1 "
            + "order by count(s) desc, s.name, s.roadAddress")
    java.util.List<DuplicateGroupProjection> findDuplicateGroups(org.springframework.data.domain.Pageable pageable);

    @Query(
            value = "select count(*) from (select 1 from store where road_address is not null "
                    + "group by name, road_address, currency_type_id having count(*) > 1) t",
            nativeQuery = true)
    long countDuplicateGroups();

    @Query(
            value = "select count(*) as groupCount, coalesce(sum(cnt), 0) as duplicateStoreCount from ("
                    + "select count(*) as cnt from store where road_address is not null "
                    + "group by name, road_address, currency_type_id having count(*) > 1) t",
            nativeQuery = true)
    DuplicateSummaryProjection summarizeDuplicates();

    /** 각 중복 그룹에서 가장 먼저 등록된(id 최소) 1건을 제외한 나머지 가맹점 id. */
    @Query(
            value = "select s.id from store s join ("
                    + "select name, road_address, currency_type_id, min(id) as keep_id from store "
                    + "where road_address is not null "
                    + "group by name, road_address, currency_type_id having count(*) > 1) d "
                    + "on s.name = d.name and s.road_address = d.road_address "
                    + "and (s.currency_type_id = d.currency_type_id "
                    + "or (s.currency_type_id is null and d.currency_type_id is null)) "
                    + "where s.id <> d.keep_id",
            nativeQuery = true)
    java.util.List<Long> findRedundantDuplicateIds();

    @Query("select s from Store s left join s.currencyType c "
            + "where s.name = :name and s.roadAddress = :roadAddress "
            + "and ((:currencyTypeId is null and c is null) or c.id = :currencyTypeId) "
            + "order by s.id")
    java.util.List<Store> findDuplicateMembers(
            @Param("name") String name,
            @Param("roadAddress") String roadAddress,
            @Param("currencyTypeId") Long currencyTypeId);

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
