package com.mannetroll.servicepoints;

import java.io.Serializable;

public class Address implements Serializable {
    private static final long serialVersionUID = 1L;
    private String countryCode;
    private String city;
    private String streetName;
    private String streetNumber;
    private String postalCode;
    private String additionalDescription;
    public String getCountryCode() {
        return countryCode;
    }
    public String getCity() {
        return city;
    }
    public String getStreetName() {
        return streetName;
    }
    public String getStreetNumber() {
        return streetNumber;
    }
    public String getPostalCode() {
        return postalCode;
    }
    public String getAdditionalDescription() {
        return additionalDescription;
    }
}
