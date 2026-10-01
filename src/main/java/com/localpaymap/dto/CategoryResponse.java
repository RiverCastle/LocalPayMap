package com.localpaymap.dto;

import java.util.List;

/** 업종 칩(그룹)과 자동완성용 세부 업종 목록. */
public record CategoryResponse(List<Group> groups, List<Item> items) {

    public record Group(String name, long count) {}

    /** value는 DB에 저장된 원본 업종값(검색 파라미터로 그대로 사용), label은 화면 표시용. */
    public record Item(String value, String label, String group, long count) {}
}
