package com.qtai.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qtai.gateway.ai-service")
public class AiServiceRouteProperties {

    private boolean routeEnabled;
    private String uri;
    private String healthPath = "/actuator/health";
    private int routeOrder = -10;

    public boolean isRouteEnabled() {
        return routeEnabled;
    }

    public void setRouteEnabled(boolean routeEnabled) {
        this.routeEnabled = routeEnabled;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }

    public int getRouteOrder() {
        return routeOrder;
    }

    public void setRouteOrder(int routeOrder) {
        this.routeOrder = routeOrder;
    }
}
