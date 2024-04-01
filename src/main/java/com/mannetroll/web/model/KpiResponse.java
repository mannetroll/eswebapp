package com.mannetroll.web.model;

import java.util.Collections;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;

@XmlRootElement(name = "KpiResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KpiResponse")
@JsonInclude(Include.NON_NULL)
@ApiModel(value = "KpiResponse", description = "Model containing a KPI")
public class KpiResponse {
    private static final String ONROAD = "onroad_";
    private static final String TIMESTAMP = "@timestamp";
    private String error;
    private ApiError apiError;
    private final Map<String, Object> data;

    public KpiResponse(Map<String, Object> data) {
        this.data = data;
    }

    public KpiResponse(ApiError apiError) {
        this.apiError = apiError;
        this.data = Collections.emptyMap();
    }

    public Map<String, Object> getData() {
        data.remove(TIMESTAMP);
        data.entrySet().removeIf(e -> e.getKey().startsWith(ONROAD));
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
