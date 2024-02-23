package com.mannetroll.web.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EdiPressureStatusResponse {
    private String error;
    private ApiError apiError;
    private String name;
    private String date;
    private Long numberOfItems;
    private Integer numberOfConsignorNames;

    private final List<Map<String, Object>> ediPressureStatus = new ArrayList<>();

    public EdiPressureStatusResponse() {
    }

    public EdiPressureStatusResponse(ApiError apiError) {
        this.apiError = apiError;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Long getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(Long numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public Integer getNumberOfConsignorNames() {
        return numberOfConsignorNames;
    }

    public void setNumberOfConsignorNames(Integer numberOfConsignorNames) {
        this.numberOfConsignorNames = numberOfConsignorNames;
    }

    public List<Map<String, Object>> getEdiPressureStatus() {
        return ediPressureStatus;
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

}