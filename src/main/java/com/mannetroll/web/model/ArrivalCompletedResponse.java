package com.mannetroll.web.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ArrivalCompletedResponse {
    private String error;
    private ApiError apiError;
    private String name;
    private String servicePointId;
    private final List<Map<String, Object>> arrivalCompleted = new ArrayList<>();
    private Integer percentile;
    private Integer weeks;

    public ArrivalCompletedResponse() {
    }

    public ArrivalCompletedResponse(ApiError apiError) {
        this.apiError = apiError;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Map<String, Object>> getArrivalCompleted() {
        return arrivalCompleted;
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

    public Integer getPercentile() {
        return percentile;
    }

    public void setPercentile(Integer percentile) {
        this.percentile = percentile;
    }

    public Integer getWeeks() {
        return weeks;
    }

    public void setWeeks(Integer weeks) {
        this.weeks = weeks;
    }

}