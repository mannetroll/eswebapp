package com.mannetroll.web.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ArrivalCompletedStatusResponse {
    private String error;
    private ApiError apiError;
    private Long numberOfItems;
    private Integer numberOfServicePoints;
    private final List<Map<String, Object>> arrivalCompletedStatus = new ArrayList<>();

    public ArrivalCompletedStatusResponse() {
    }

    public Long getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(Long numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public Integer getNumberOfServicePoints() {
        return numberOfServicePoints;
    }

    public void setNumberOfServicePoints(Integer numberOfServicePoints) {
        this.numberOfServicePoints = numberOfServicePoints;
    }

    public List<Map<String, Object>> getArrivalCompletedStatus() {
        return arrivalCompletedStatus;
    }

    public ArrivalCompletedStatusResponse(ApiError apiError) {
        this.apiError = apiError;
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