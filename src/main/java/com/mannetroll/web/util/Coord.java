package com.mannetroll.web.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Coord {
    public BigDecimal lat = new BigDecimal(0);
    public BigDecimal lon = new BigDecimal(0);
    public BigDecimal TEN = new BigDecimal(10);
    public Coord() {
    }
    public Coord(BigDecimal lat, BigDecimal lon) {
        this.lat = lat;
        this.lon = lon;
    }
    public BigDecimal getLat() {
        return lat;
    }
    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }
    public BigDecimal getLon() {
        return lon;
    }
    public void setLon(BigDecimal lon) {
        this.lon = lon;
    }
    public String getLatString() {
        return lat.setScale(6, RoundingMode.HALF_UP).toPlainString();
    }
    public String getLonString() {
        return lon.setScale(6, RoundingMode.HALF_UP).toPlainString();
    }
    @Override
    public String toString() {
        return lat.floatValue() + "," + lon.floatValue();
    }
    public String getLatEnum() {
        return "LAT" + lat.multiply(TEN).intValue();
    }
    public String getLonEnum() {
        return "LON" + lon.multiply(TEN).intValue();
    }
}