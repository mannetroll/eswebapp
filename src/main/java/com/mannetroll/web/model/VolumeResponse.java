package com.mannetroll.web.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class VolumeResponse {
    private String error;
    private ApiError apiError;
    private List<String[]> data = new ArrayList<String[]>();

    public VolumeResponse(List<String[]> data) {
        this.data = data;
    }

    public VolumeResponse(ApiError apiError) {
        this.apiError = apiError;
        this.data = Collections.emptyList();
    }

    public List<String[]> getData() {
        return data;
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