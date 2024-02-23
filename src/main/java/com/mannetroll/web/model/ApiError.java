package com.mannetroll.web.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ApiError")
@ApiModel(value = "ApiError", description = "Model for API errors")
@JsonInclude(Include.NON_EMPTY)
public class ApiError {
    private List<Fault> faults = new ArrayList<Fault>();

    public ApiError(){ }
    
    public ApiError(String faultCode, String explanationText) {
        Fault fault = new Fault(faultCode, explanationText);
        faults.add(fault);
    }

    @ApiModelProperty(required = true)
    public List<Fault> getFaults() {
        return faults;
    }

    public void addFault(Fault fault) {
        faults.add(fault);
    }
}
