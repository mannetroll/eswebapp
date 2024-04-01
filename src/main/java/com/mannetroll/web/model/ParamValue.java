package com.mannetroll.web.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ParamValues")
@ApiModel(value = "ParamValues", description = "Key/Value information about errors")
public class ParamValue {
    private String param;
    private String value;

    public ParamValue() {
    }

    public ParamValue(String param, String value) {
        this.param = param;
        this.value = value;
    }

    @ApiModelProperty(required = true)
    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    @ApiModelProperty(required = true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
