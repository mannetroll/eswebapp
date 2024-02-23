package com.mannetroll.servicepoints;

import java.io.Serializable;
import java.math.BigDecimal;

public class Coordinate implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String countryCode;
    protected String srId;
    protected BigDecimal northing;
    protected BigDecimal easting;

    public String getCountryCode() {
        return countryCode;
    }
    public String getSrId() {
        return srId;
    }
    public BigDecimal getNorthing() {
        return northing;
    }
    public BigDecimal getEasting() {
        return easting;
    }

    @Override
    public String toString() {
        return northing + "," + easting;
    }

}
