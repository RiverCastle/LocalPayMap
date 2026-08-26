package com.localpaymap.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 공공데이터포털 "전국지역화폐가맹점표준데이터"(한국조폐공사_통합_가맹점기본정보) 응답 항목.
 * 실제 필드명은 활용신청 승인 후 상세 명세서를 받아 검증/수정해야 한다. (data.go.kr 15100062)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenDataStoreItem {

    private String bizplcNm; // 가맹점명
    private String rdnwhlAddr; // 도로명주소
    private String lnmAddr; // 지번주소
    private String telNo; // 전화번호
    private String lat; // 위도
    private String lot; // 경도
    private String induty; // 업종
    private String bizrno; // 사업자등록번호 (external id 대용)
    private String trdStateNm; // 영업상태명
    private String billNm; // 지역화폐(상품권)명
    private String ctpvNm; // 시도명
    private String sggNm; // 시군구명

    public String getBizplcNm() {
        return bizplcNm;
    }

    public void setBizplcNm(String bizplcNm) {
        this.bizplcNm = bizplcNm;
    }

    public String getRdnwhlAddr() {
        return rdnwhlAddr;
    }

    public void setRdnwhlAddr(String rdnwhlAddr) {
        this.rdnwhlAddr = rdnwhlAddr;
    }

    public String getLnmAddr() {
        return lnmAddr;
    }

    public void setLnmAddr(String lnmAddr) {
        this.lnmAddr = lnmAddr;
    }

    public String getTelNo() {
        return telNo;
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }

    public String getInduty() {
        return induty;
    }

    public void setInduty(String induty) {
        this.induty = induty;
    }

    public String getBizrno() {
        return bizrno;
    }

    public void setBizrno(String bizrno) {
        this.bizrno = bizrno;
    }

    public String getTrdStateNm() {
        return trdStateNm;
    }

    public void setTrdStateNm(String trdStateNm) {
        this.trdStateNm = trdStateNm;
    }

    public String getBillNm() {
        return billNm;
    }

    public void setBillNm(String billNm) {
        this.billNm = billNm;
    }

    public String getCtpvNm() {
        return ctpvNm;
    }

    public void setCtpvNm(String ctpvNm) {
        this.ctpvNm = ctpvNm;
    }

    public String getSggNm() {
        return sggNm;
    }

    public void setSggNm(String sggNm) {
        this.sggNm = sggNm;
    }
}
