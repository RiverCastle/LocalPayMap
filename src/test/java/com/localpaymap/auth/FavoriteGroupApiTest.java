package com.localpaymap.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.localpaymap.domain.AppUser;
import com.localpaymap.domain.BizStatus;
import com.localpaymap.domain.Store;
import com.localpaymap.domain.StoreSource;
import com.localpaymap.repository.AppUserRepository;
import com.localpaymap.repository.StoreRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class FavoriteGroupApiTest {

    private static final String BASE = "/api/me/favorite-groups";

    @Autowired MockMvc mockMvc;
    @Autowired AuthTokenService tokenService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired StoreRepository storeRepository;

    private Cookie loginAs(String providerId) {
        AppUser user = appUserRepository.save(new AppUser("kakao", providerId, "u-" + providerId, null));
        return new Cookie(TokenCookies.ACCESS, tokenService.issue(user).accessToken());
    }

    private Store newStore(String name) {
        return storeRepository.save(new Store(
                name, "도로명 1", null, 37.5, 127.0, null, "한식", null, BizStatus.OPEN, StoreSource.MANUAL, null));
    }

    /** 쓰기 요청: 로그인 쿠키 + 실제 CSRF 쿠키/헤더 */
    private static MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder builder, Cookie login) {
        return builder.cookie(login, new Cookie("XSRF-TOKEN", "t")).header("X-XSRF-TOKEN", "t");
    }

    private String createGroup(Cookie login, String name) throws Exception {
        String body = mockMvc.perform(write(post(BASE), login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return body.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE)
                        .cookie(new Cookie("XSRF-TOKEN", "t"))
                        .header("X-XSRF-TOKEN", "t")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"a\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writesRequireCsrfToken() throws Exception {
        Cookie login = loginAs("g-csrf");
        mockMvc.perform(post(BASE).cookie(login).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"a\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createListRenameAndDeleteGroup() throws Exception {
        Cookie login = loginAs("g-crud");
        String id = createGroup(login, "  데이트   맛집 ");

        mockMvc.perform(get(BASE).cookie(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("데이트 맛집"))
                .andExpect(jsonPath("$[0].color").exists())
                .andExpect(jsonPath("$[0].storeIds.length()").value(0));

        mockMvc.perform(write(patch(BASE + "/" + id), login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"회식\",\"color\":\"#2f9e44\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("회식"))
                .andExpect(jsonPath("$.color").value("#2f9e44"));

        mockMvc.perform(write(delete(BASE + "/" + id), login)).andExpect(status().isNoContent());
        mockMvc.perform(get(BASE).cookie(login)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void duplicateNameInvalidNameAndBadColorAreRejected() throws Exception {
        Cookie login = loginAs("g-dup");
        createGroup(login, "중복");

        mockMvc.perform(write(post(BASE), login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"중복\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(write(post(BASE), login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(write(post(BASE), login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"색\",\"color\":\"red;x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameStoreCanBeInMultipleGroupsAndAddIsIdempotent() throws Exception {
        Cookie login = loginAs("g-store");
        Store store = newStore("여러그룹 식당");
        String g1 = createGroup(login, "A");
        String g2 = createGroup(login, "B");

        for (String g : new String[] {g1, g2, g1}) {
            mockMvc.perform(write(put(BASE + "/" + g + "/stores/" + store.getId()), login))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(get(BASE).cookie(login))
                .andExpect(jsonPath("$[0].storeIds.length()").value(1))
                .andExpect(jsonPath("$[1].storeIds.length()").value(1));
        mockMvc.perform(get(BASE + "/" + g1 + "/stores").cookie(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("여러그룹 식당"));

        mockMvc.perform(write(delete(BASE + "/" + g1 + "/stores/" + store.getId()), login))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/" + g1 + "/stores").cookie(login)).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(BASE + "/" + g2 + "/stores").cookie(login)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unknownStoreIsRejected() throws Exception {
        Cookie login = loginAs("g-nostore");
        String g = createGroup(login, "A");
        mockMvc.perform(write(put(BASE + "/" + g + "/stores/999999"), login)).andExpect(status().isNotFound());
    }

    @Test
    void usersCannotSeeOrModifyEachOthersGroups() throws Exception {
        Cookie alice = loginAs("g-alice");
        Cookie bob = loginAs("g-bob");
        Store store = newStore("앨리스 식당");
        String aliceGroup = createGroup(alice, "앨리스 그룹");

        mockMvc.perform(get(BASE).cookie(bob)).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(BASE + "/" + aliceGroup + "/stores").cookie(bob)).andExpect(status().isNotFound());
        mockMvc.perform(write(put(BASE + "/" + aliceGroup + "/stores/" + store.getId()), bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(write(patch(BASE + "/" + aliceGroup), bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"해킹\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(write(delete(BASE + "/" + aliceGroup), bob)).andExpect(status().isNotFound());

        // 앨리스의 그룹은 그대로
        mockMvc.perform(get(BASE).cookie(alice)).andExpect(jsonPath("$[0].name").value("앨리스 그룹"));
    }

    @Test
    void groupCountIsLimited() throws Exception {
        Cookie login = loginAs("g-limit");
        for (int i = 0; i < 20; i++) {
            createGroup(login, "g" + i);
        }
        mockMvc.perform(write(post(BASE), login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"초과\"}"))
                .andExpect(status().isBadRequest());
    }
}
