package com.localpaymap.config;

import com.localpaymap.domain.AdminRole;
import com.localpaymap.domain.AdminUser;
import com.localpaymap.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 최초 실행 시 관리자 계정이 하나도 없으면 기본 계정을 생성한다. 운영 환경에서는 반드시 비밀번호를 변경해야 한다. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultUsername;
    private final String defaultPassword;

    public DataInitializer(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.default-username:admin}") String defaultUsername,
            @Value("${admin.default-password:changeme123!}") String defaultPassword) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
    }

    @Override
    public void run(String... args) {
        if (adminUserRepository.count() == 0) {
            adminUserRepository.save(
                    new AdminUser(defaultUsername, passwordEncoder.encode(defaultPassword), AdminRole.ADMIN));
        }
    }
}
