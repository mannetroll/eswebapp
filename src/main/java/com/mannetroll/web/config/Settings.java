package com.mannetroll.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author drtobbe
 */
@Component
@ConfigurationProperties(prefix = "kpis")
public class Settings {
    private String eshost;
    private String cluster;
    private String shield;
    private Integer timeout;
    private Integer timetolive;

    public String getEshost() {
        return eshost;
    }
    public void setEshost(String eshost) {
        this.eshost = eshost;
    }
    public String getCluster() {
        return cluster;
    }
    public void setCluster(String cluster) {
        this.cluster = cluster;
    }
    public String getShield() {
        return shield;
    }
    public void setShield(String shield) {
        this.shield = shield;
    }
    public Integer getTimeout() {
        return timeout;
    }
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    public Integer getTimetolive() {
        return timetolive;
    }
    public void setTimetolive(Integer timetolive) {
        this.timetolive = timetolive;
    }
}
