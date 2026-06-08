package com.qtai.domain.ai.client.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qtai.ai.client")
public class AiClientProperties {

    private Mode mode = Mode.MOCK;
    private String serviceToken = "";
    private int timeoutMs = 3000;
    private Service qt = new Service();
    private Service bible = new Service();
    private Service study = new Service();
    private Service audit = new Service();
    private Service adminAuth = new Service();

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.MOCK : mode;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Service getQt() {
        return qt;
    }

    public void setQt(Service qt) {
        this.qt = qt == null ? new Service() : qt;
    }

    public Service getBible() {
        return bible;
    }

    public void setBible(Service bible) {
        this.bible = bible == null ? new Service() : bible;
    }

    public Service getStudy() {
        return study;
    }

    public void setStudy(Service study) {
        this.study = study == null ? new Service() : study;
    }

    public Service getAudit() {
        return audit;
    }

    public void setAudit(Service audit) {
        this.audit = audit == null ? new Service() : audit;
    }

    public Service getAdminAuth() {
        return adminAuth;
    }

    public void setAdminAuth(Service adminAuth) {
        this.adminAuth = adminAuth == null ? new Service() : adminAuth;
    }

    public int timeoutMsFor(Service service) {
        if (service != null && service.getTimeoutMs() != null && service.getTimeoutMs() > 0) {
            return service.getTimeoutMs();
        }
        return timeoutMs;
    }

    public enum Mode {
        MOCK,
        HTTP
    }

    public static class Service {

        private String baseUrl = "";
        private Integer timeoutMs;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Integer getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(Integer timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
