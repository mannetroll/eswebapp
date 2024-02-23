package com.mannetroll.web.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PopularTimesResponse {
    private String error;
    private ApiError apiError;
    private String name;
    private String date;
    private DateTime dateFrom;
    private DateTime dateTo;
    private String servicePointId;
    private List<Map<String, Object>> popularTimes = new ArrayList<>();
    private Long weekAverage;
    private String countryCode;

    public PopularTimesResponse() {
    }

    public PopularTimesResponse(ApiError apiError) {
        this.apiError = apiError;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Map<String, Object>> getPopularTimes() {
        return popularTimes;
    }

    public String getServicePointId() {
        return servicePointId;
    }

    public void setServicePointId(String servicePointId) {
        this.servicePointId = servicePointId;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setApiError(ApiError apiError) {
        this.apiError = apiError;
    }

    public ApiError getApiError() {
        return apiError;
    }

    public String getError() {
        return error;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getWeekAverage() {
        return weekAverage;
    }

    public void setWeekAverage(Long weekAverage) {
        this.weekAverage = weekAverage;

    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setDateFrom(DateTime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public DateTime getDateFrom() {
        return dateFrom;
    }

    public void setDateTo(DateTime toDay) {
        this.dateTo = toDay;
    }

    public DateTime getDateTo() {
        return dateTo;
    }
}