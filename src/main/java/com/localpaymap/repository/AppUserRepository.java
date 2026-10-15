package com.localpaymap.repository;

import com.localpaymap.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByProviderAndProviderId(String provider, String providerId);
}
