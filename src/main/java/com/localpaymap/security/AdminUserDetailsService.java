package com.localpaymap.security;

import com.localpaymap.domain.AdminUser;
import com.localpaymap.repository.AdminUserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    public AdminUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 관리자입니다: " + username));

        return new User(
                adminUser.getUsername(),
                adminUser.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + adminUser.getRole().name())));
    }
}
