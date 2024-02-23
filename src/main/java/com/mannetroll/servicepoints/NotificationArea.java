package com.mannetroll.servicepoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NotificationArea implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> postalCodes = new ArrayList<String>();

    public List<String> getPostalCodes() {
        return postalCodes;
    }

}
