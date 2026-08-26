package com.localpaymap.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantImportItem {

    private Long seq;
    private String simpleNm;
    private String addr;
    private String telephone;
    private String telNo;
    private String bizTypeNm;
    private Double latitude;
    private Double longitude;

    public Long getSeq() {
        return seq;
    }

    public void setSeq(Long seq) {
        this.seq = seq;
    }

    public String getSimpleNm() {
        return simpleNm;
    }

    public void setSimpleNm(String simpleNm) {
        this.simpleNm = simpleNm;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getTelNo() {
        return telNo;
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public String getBizTypeNm() {
        return bizTypeNm;
    }

    public void setBizTypeNm(String bizTypeNm) {
        this.bizTypeNm = bizTypeNm;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String phoneOrNull() {
        if (telNo != null && !telNo.isBlank()) {
            return telNo;
        }
        return (telephone != null && !telephone.isBlank()) ? telephone : null;
    }
}
