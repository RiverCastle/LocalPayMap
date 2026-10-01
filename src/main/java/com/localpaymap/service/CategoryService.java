package com.localpaymap.service;

import com.localpaymap.dto.CategoryResponse;
import com.localpaymap.repository.CategoryCountProjection;
import com.localpaymap.repository.StoreRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB에 저장된 세부 업종값("대분류/소분류" 형태, 165종 내외)을 사용자가 이해하기 쉬운 몇 개의 그룹으로 묶는다.
 * 그룹은 업종명에 포함된 키워드로 판정하며, 선언 순서대로 먼저 맞는 그룹이 우선한다. 어디에도 안 맞으면 "기타".
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    static final String OTHER = "기타";
    private static final long CACHE_MILLIS = 60_000;

    private static final Map<String, List<String>> GROUP_KEYWORDS = new LinkedHashMap<>();

    static {
        GROUP_KEYWORDS.put("반려동물", List.of("애완", "동물"));
        GROUP_KEYWORDS.put("병원·약국", List.of("병원", "의원", "약국", "한약", "한방", "의약", "의료", "산후조리", "과"));
        GROUP_KEYWORDS.put("학원·교육", List.of("학원", "교육", "교습", "교구"));
        GROUP_KEYWORDS.put("자동차", List.of("자동차", "차량", "세차", "타이어", "오토바이", "주유", "주차", "대리운전", "운송기구"));
        GROUP_KEYWORDS.put("카페·디저트", List.of("커피", "제과", "아이스크림"));
        GROUP_KEYWORDS.put("음식점", List.of("음식점", "식전문점", "뷔페", "패스트푸드", "주점", "치킨"));
        GROUP_KEYWORDS.put(
                "마트·식품",
                List.of("편의점", "마트", "식음료", "정육", "청과", "농산물", "수산", "건강보조", "인삼", "체인스토어", "아울렛", "주류", "사료"));
        GROUP_KEYWORDS.put("미용·뷰티", List.of("미용", "피부", "이발", "화장품", "가발", "안마"));
        GROUP_KEYWORDS.put(
                "레저·숙박",
                List.of("헬스", "골프", "당구", "노래방", "오락", "레저", "영화", "숙박", "호텔", "사우나", "스포츠", "자전거", "여행", "취미"));
        GROUP_KEYWORDS.put(
                "쇼핑·패션",
                List.of("의류", "내의", "신발", "가방", "한복", "양복", "시계", "귀금속", "잡화", "안경", "문구", "서적", "가구", "침구", "커튼",
                        "가전", "컴퓨터", "통신기기", "조명", "건축자재", "주방", "화원", "완구", "악기", "상거래", "공예", "중고", "기념",
                        "선물", "유아복", "카메라", "전자", "비디오", "음반", "교복", "예술품", "화방", "상품", "종합소매", "사무", "기계", "보일러", "연료"));
        GROUP_KEYWORDS.put(
                "생활서비스",
                List.of("세탁", "서비스", "수리", "수선", "이사", "부동산", "광고", "판촉", "택배", "창고", "사진", "통신", "인쇄", "웨딩",
                        "점술", "정수기", "인테리어", "기부", "요금", "공공", "장비임대", "대인", "복지", "소비조합"));
    }

    private final StoreRepository storeRepository;

    private volatile Snapshot snapshot;

    public CategoryService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /** 칩(그룹)과 자동완성 후보. 그룹은 가맹점 수 내림차순이며 "기타"는 항상 마지막이다. */
    public CategoryResponse list() {
        Snapshot snap = load();
        return new CategoryResponse(snap.groups(), snap.items());
    }

    /**
     * 선택된 그룹명/세부 업종값을 DB 조회용 정확한 업종값 목록으로 펼친다.
     * 필터가 없으면 빈 리스트를 돌려주며, 이 경우 호출측은 업종 조건을 걸지 않는다.
     */
    public List<String> resolve(List<String> groupNames, List<String> items) {
        Set<String> result = new LinkedHashSet<>();
        if (items != null) {
            items.stream().filter(v -> v != null && !v.isBlank()).forEach(result::add);
        }
        if (groupNames != null && !groupNames.isEmpty()) {
            Set<String> wanted = new LinkedHashSet<>(groupNames);
            for (CategoryResponse.Item item : load().items()) {
                if (wanted.contains(item.group())) {
                    result.add(item.value());
                }
            }
            // 선택한 그룹에 속한 업종이 하나도 없으면 "조건 없음"이 아니라 "결과 없음"이 되도록 존재하지 않는 값을 넣는다.
            if (result.isEmpty()) {
                result.add("__none__");
            }
        }
        return new ArrayList<>(result);
    }

    static String groupOf(String category) {
        String top = category.contains("/") ? category.substring(0, category.indexOf('/')) : category;
        for (Map.Entry<String, List<String>> entry : GROUP_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (matches(keyword, top)) {
                    return entry.getKey();
                }
            }
        }
        return OTHER;
    }

    /** 한 글자 키워드 "과"(진료과)는 오탐이 잦아서 업종명이 "과"로 끝날 때만 인정한다. */
    private static boolean matches(String keyword, String top) {
        if ("과".equals(keyword)) {
            return top.endsWith("과");
        }
        return top.contains(keyword);
    }

    /** "일반음식점/일반음식점"처럼 대·소분류가 같으면 한 번만 보여준다. */
    static String labelOf(String category) {
        int idx = category.indexOf('/');
        if (idx < 0) {
            return category;
        }
        String top = category.substring(0, idx);
        String sub = category.substring(idx + 1);
        return top.equals(sub) ? top : top + " · " + sub;
    }

    private Snapshot load() {
        Snapshot current = snapshot;
        long now = System.currentTimeMillis();
        if (current != null && now - current.loadedAt() < CACHE_MILLIS) {
            return current;
        }
        List<CategoryResponse.Item> items = new ArrayList<>();
        Map<String, Long> groupCounts = new LinkedHashMap<>();
        for (CategoryCountProjection row : storeRepository.countByCategory()) {
            String group = groupOf(row.getCategory());
            items.add(new CategoryResponse.Item(row.getCategory(), labelOf(row.getCategory()), group, row.getCnt()));
            groupCounts.merge(group, row.getCnt(), Long::sum);
        }
        items.sort(Comparator.comparingLong(CategoryResponse.Item::count).reversed());

        List<CategoryResponse.Group> groups = new ArrayList<>();
        groupCounts.entrySet().stream()
                .filter(e -> !OTHER.equals(e.getKey()))
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> groups.add(new CategoryResponse.Group(e.getKey(), e.getValue())));
        if (groupCounts.containsKey(OTHER)) {
            groups.add(new CategoryResponse.Group(OTHER, groupCounts.get(OTHER)));
        }

        Snapshot fresh = new Snapshot(now, groups, items);
        // 데이터가 아직 없을 때(최초 가져오기 전)는 캐시하지 않아 가져오기 직후 바로 반영되게 한다.
        if (!items.isEmpty()) {
            snapshot = fresh;
        }
        return fresh;
    }

    private record Snapshot(long loadedAt, List<CategoryResponse.Group> groups, List<CategoryResponse.Item> items) {}
}
