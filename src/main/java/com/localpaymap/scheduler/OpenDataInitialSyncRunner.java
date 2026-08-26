package com.localpaymap.scheduler;

import com.localpaymap.domain.StoreSource;
import com.localpaymap.repository.StoreRepository;
import com.localpaymap.service.OpenDataSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 서버가 처음 떠서(=OPEN_API로 적재된 가맹점이 하나도 없을 때) 공공데이터를 아직 한 번도 못 받아온 상태라면,
 * 매일 새벽 스케줄({@link OpenDataSyncScheduler})을 기다리지 않고 기동 직후 곧바로 한 번 동기화한다.
 * 이미 데이터가 있으면(재기동/재배포) 다시 부르지 않고 기존 스케줄에 맡긴다.
 * 기동 자체를 막지 않도록 비동기로 실행한다.
 */
@Component
public class OpenDataInitialSyncRunner {

    private static final Logger log = LoggerFactory.getLogger(OpenDataInitialSyncRunner.class);

    private final StoreRepository storeRepository;
    private final OpenDataSyncService openDataSyncService;
    private final boolean syncOnStartup;

    public OpenDataInitialSyncRunner(
            StoreRepository storeRepository,
            OpenDataSyncService openDataSyncService,
            @Value("${open-data.sync-on-startup:true}") boolean syncOnStartup) {
        this.storeRepository = storeRepository;
        this.openDataSyncService = openDataSyncService;
        this.syncOnStartup = syncOnStartup;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnFirstStartup() {
        if (!syncOnStartup) {
            return;
        }
        if (storeRepository.countBySource(StoreSource.OPEN_API) > 0) {
            log.info("공공데이터 가맹점이 이미 존재하여 기동 시 동기화를 건너뜁니다.");
            return;
        }
        log.info("공공데이터 가맹점이 없어 기동 직후 최초 동기화를 시작합니다.");
        openDataSyncService.sync();
    }
}
