package com.localpaymap.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** 지자체 지역화폐 앱(예: 수원페이) API를 그대로 덤프한 JSON을 관리자가 업로드할 때 파싱하는 구조. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantImportPayload {

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private int totalCount;
        private List<MerchantImportItem> merchants;

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public List<MerchantImportItem> getMerchants() {
            return merchants;
        }

        public void setMerchants(List<MerchantImportItem> merchants) {
            this.merchants = merchants;
        }
    }
}
