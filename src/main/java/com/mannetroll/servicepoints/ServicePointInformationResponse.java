package com.mannetroll.servicepoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("servicePointInformationResponse")
public class ServicePointInformationResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<ServicePoint> servicePoints = new ArrayList<ServicePoint>();

    public List<ServicePoint> getServicePoints() {
        return this.servicePoints;
    }

}
