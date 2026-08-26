package com.localpaymap.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverGeocodeResponse {

    private String status;
    private List<Address> addresses;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        private String roadAddress;
        private String jibunAddress;
        private String x; // 경도(lng)
        private String y; // 위도(lat)

        public String getRoadAddress() {
            return roadAddress;
        }

        public void setRoadAddress(String roadAddress) {
            this.roadAddress = roadAddress;
        }

        public String getJibunAddress() {
            return jibunAddress;
        }

        public void setJibunAddress(String jibunAddress) {
            this.jibunAddress = jibunAddress;
        }

        public String getX() {
            return x;
        }

        public void setX(String x) {
            this.x = x;
        }

        public String getY() {
            return y;
        }

        public void setY(String y) {
            this.y = y;
        }
    }
}
