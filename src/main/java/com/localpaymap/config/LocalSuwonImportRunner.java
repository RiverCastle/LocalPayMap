package com.localpaymap.config;

import com.localpaymap.domain.StoreSource;
import com.localpaymap.repository.StoreRepository;
import com.localpaymap.service.StoreImportService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * local 프로파일 개발 편의용: H2는 재기동마다 초기화되므로, 서버가 뜰 때마다
 * 로컬에 있는 수원 가맹점 raw JSON 파일을 관리자 파일 가져오기와 동일한 경로로 자동 적재한다.
 * 파일이 없으면(다른 개발자 PC 등) 조용히 건너뛴다. 기동을 막지 않도록 비동기로 실행.
 */
@Profile("local")
@Component
public class LocalSuwonImportRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSuwonImportRunner.class);

    private final StoreRepository storeRepository;
    private final StoreImportService storeImportService;
    private final String filePath;

    public LocalSuwonImportRunner(
            StoreRepository storeRepository,
            StoreImportService storeImportService,
            @Value("${import.suwon-file-path:}") String filePath) {
        this.storeRepository = storeRepository;
        this.storeImportService = storeImportService;
        this.filePath = filePath;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void importOnStartup() {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        if (storeRepository.countBySource(StoreSource.IMPORTED) > 0) {
            log.info("가져오기 데이터가 이미 존재하여 수원 가맹점 자동 적재를 건너뜁니다.");
            return;
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            log.warn("수원 가맹점 파일을 찾을 수 없어 자동 적재를 건너뜁니다: {}", filePath);
            return;
        }

        try {
            log.info("수원 가맹점 파일 자동 적재를 시작합니다: {}", filePath);
            String json = Files.readString(path, StandardCharsets.UTF_8);
            storeImportService.importJson(json, "경기도 수원시", "수원페이");
            log.info("수원 가맹점 파일 자동 적재를 완료했습니다.");
        } catch (IOException e) {
            log.error("수원 가맹점 파일 읽기 실패: {}", filePath, e);
        }
    }
}
