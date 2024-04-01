package com.mannetroll.web.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Fault")
@JsonInclude(Include.NON_EMPTY)
@ApiModel(value = "Fault", description = "Model for API Fault")
public class Fault {
    private String faultCode;
    private String explanationText;
    private List<ParamValue> paramValues = new ArrayList<ParamValue>();

    public Fault() {
    }

    public Fault(String faultCode, String explanationText) {
        this.faultCode = faultCode;
        this.explanationText = explanationText;
    }

    @ApiModelProperty(required = true)
    public String getFaultCode() {
        return faultCode;
    }

    public void setFaultCode(String faultCode) {
        this.faultCode = faultCode;
    }

    @ApiModelProperty(required = true)
    public String getExplanationText() {
        return explanationText;
    }

    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
    }

    @ApiModelProperty(required = false)
    public List<ParamValue> getParamValues() {
        return paramValues;
    }

    public void setParamValues(List<ParamValue> paramValues) {
        this.paramValues = paramValues;
    }

    public void addParamValue(ParamValue parameter) {
        this.paramValues.add(parameter);
    }
}
