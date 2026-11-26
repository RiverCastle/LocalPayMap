package com.localpaymap.repository;

import com.localpaymap.domain.ReportType;
import com.localpaymap.domain.StoreReport;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreReportRepository extends JpaRepository<StoreReport, Long> {

    boolean existsByUserIdAndStoreIdAndType(Long userId, Long storeId, ReportType type);

    List<StoreReport> findByStoreId(Long storeId);

    /** 내 제보 목록. 가맹점이 이미 사라진 제보는 제외한다. */
    @Query("select r from StoreReport r where r.userId = :userId "
            + "and exists (select s.id from Store s where s.id = r.storeId) order by r.id")
    List<StoreReport> findMine(@Param("userId") Long userId);

    @Modifying
    @Query("delete from StoreReport r where r.userId = :userId and r.storeId = :storeId and r.type = :type")
    int deleteMine(@Param("userId") Long userId, @Param("storeId") Long storeId, @Param("type") ReportType type);

    @Query("select r.storeId as storeId, r.type as type, count(r) as cnt, max(r.createdAt) as lastAt "
            + "from StoreReport r where r.storeId in :storeIds group by r.storeId, r.type")
    List<ReportCountProjection> countByStoreIds(@Param("storeIds") Collection<Long> storeIds);

    @Query("select distinct r.storeId from StoreReport r where r.storeId in :storeIds")
    List<Long> findReferencedStoreIds(@Param("storeIds") Collection<Long> storeIds);
}
