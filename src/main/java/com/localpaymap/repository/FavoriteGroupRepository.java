package com.localpaymap.repository;

import com.localpaymap.domain.FavoriteGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteGroupRepository extends JpaRepository<FavoriteGroup, Long> {

    List<FavoriteGroup> findByUserIdOrderByIdAsc(Long userId);

    Optional<FavoriteGroup> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
