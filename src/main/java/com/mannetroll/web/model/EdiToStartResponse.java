package com.mannetroll.web.model;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class EdiToStartResponse {
    private String error;
    private ApiError apiError;
    private String consignor;
    private Map<String, Map<String, Object>> weekDay = new TreeMap<String, Map<String, Object>>();

    public EdiToStartResponse() {
    }

    public EdiToStartResponse(ApiError apiError) {
        this.apiError = apiError;
    }

    public Map<String, Map<String, Object>> getWeekDay() {
        return weekDay;
    }

    public String getConsignor() {
        return consignor;
    }

    public void setConsignor(String consignor) {
        this.consignor = consignor;
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

}