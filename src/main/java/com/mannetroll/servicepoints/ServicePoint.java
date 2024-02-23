package com.mannetroll.servicepoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServicePoint implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String servicePointId;
    private Address visitingAddress;
    private Address deliveryAddress;
    private NotificationArea notificationArea;
    private List<Coordinate> coordinates = new ArrayList<Coordinate>();

    public String getName() {
        return name;
    }
    public String getServicePointId() {
        return servicePointId;
    }
    public Address getVisitingAddress() {
        return visitingAddress;
    }
    public Address getDeliveryAddress() {
        return deliveryAddress;
    }
    public NotificationArea getNotificationArea() {
        return notificationArea;
    }
    public List<Coordinate> getCoordinates() {
        return coordinates;
    }
}
