package com.localpaymap.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.localpaymap.domain.AppUser;
import com.localpaymap.domain.BizStatus;
import com.localpaymap.domain.Store;
import com.localpaymap.domain.StoreSource;
import com.localpaymap.repository.AppUserRepository;
import com.localpaymap.repository.StoreReportRepository;
import com.localpaymap.repository.StoreRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class StoreReportApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired AuthTokenService tokenService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired StoreReportRepository reportRepository;

    private Cookie loginAs(String providerId) {
        AppUser user = appUserRepository.save(new AppUser("kakao", providerId, "u-" + providerId, null));
        return new Cookie(TokenCookies.ACCESS, tokenService.issue(user).accessToken());
    }

    /** 다른 테스트의 가맹점이 지도 조회에 섞이지 않도록 테스트마다 다른 좌표를 쓴다. */
    private Store newStore(String name, double lat) {
        return storeRepository.save(new Store(
                name, name + " 도로명", null, lat, 128.0, null, "한식", null, BizStatus.OPEN, StoreSource.MANUAL, null));
    }

    private static MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder builder, Cookie login) {
        return builder.cookie(login, new Cookie("XSRF-TOKEN", "t")).header("X-XSRF-TOKEN", "t");
    }

    private static String url(Store store, String type) {
        return "/api/me/stores/" + store.getId() + "/reports/" + type;
    }

    @Test
    void reportingRequiresLoginAndCsrfToken() throws Exception {
        Store store = newStore("제보 권한 식당", 33.1);

        mockMvc.perform(put(url(store, "PAYMENT_REFUSED"))
                        .cookie(new Cookie("XSRF-TOKEN", "t"))
                        .header("X-XSRF-TOKEN", "t"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(url(store, "PAYMENT_REFUSED")).cookie(loginAs("r-csrf")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/me/reports")).andExpect(status().isUnauthorized());
        assertThat(reportRepository.findByStoreId(store.getId())).isEmpty();
    }

    @Test
    void countsArePerUserAndRepeatedReportsAreIgnored() throws Exception {
        Cookie alice = loginAs("r-alice");
        Cookie bob = loginAs("r-bob");
        Store store = newStore("제보 집계 식당", 33.2);

        mockMvc.perform(write(put(url(store, "PAYMENT_REFUSED")), alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(write(put(url(store, "PAYMENT_REFUSED")), alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(write(put(url(store, "PAYMENT_REFUSED")), bob)).andExpect(status().isOk());
        mockMvc.perform(write(put(url(store, "CLOSED")), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.counts.PAYMENT_REFUSED").value(2))
                .andExpect(jsonPath("$.counts.CLOSED").value(1))
                .andExpect(jsonPath("$.lastReportedAt").isNotEmpty());

        mockMvc.perform(get("/api/me/reports").cookie(alice))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].storeId").value(store.getId()))
                .andExpect(jsonPath("$[0].type").value("PAYMENT_REFUSED"));
    }

    @Test
    void cancelRemovesOnlyMyReport() throws Exception {
        Cookie alice = loginAs("r-cancel-alice");
        Cookie bob = loginAs("r-cancel-bob");
        Store store = newStore("제보 취소 식당", 33.3);
        mockMvc.perform(write(put(url(store, "WRONG_INFO")), alice)).andExpect(status().isOk());
        mockMvc.perform(write(put(url(store, "WRONG_INFO")), bob)).andExpect(status().isOk());

        mockMvc.perform(write(delete(url(store, "WRONG_INFO")), alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(write(delete(url(store, "WRONG_INFO")), alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(get("/api/me/reports").cookie(bob)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void reportCountsArePublicInStoreResponses() throws Exception {
        Store reported = newStore("제보 받은 식당", 33.4);
        mockMvc.perform(write(put(url(reported, "PAYMENT_REFUSED")), loginAs("r-public")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stores?swLat=33.39&swLng=127.9&neLat=33.41&neLng=128.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stores.length()").value(1))
                .andExpect(jsonPath("$.stores[0].reports.total").value(1))
                .andExpect(jsonPath("$.stores[0].reports.counts.PAYMENT_REFUSED").value(1));
        mockMvc.perform(get("/api/stores/" + reported.getId())).andExpect(jsonPath("$.reports.total").value(1));

        Store clean = newStore("제보 없는 식당", 33.5);
        mockMvc.perform(get("/api/stores/" + clean.getId()))
                .andExpect(jsonPath("$.reports.total").value(0))
                .andExpect(jsonPath("$.reports.lastReportedAt").isEmpty());
    }

    @Test
    void unknownStoreAndUnknownTypeAreRejected() throws Exception {
        Cookie login = loginAs("r-bad");
        Store store = newStore("잘못된 제보 식당", 33.6);

        mockMvc.perform(write(put("/api/me/stores/999999/reports/CLOSED"), login))
                .andExpect(status().isNotFound());
        mockMvc.perform(write(put(url(store, "SPAM")), login)).andExpect(status().isBadRequest());
    }
}
