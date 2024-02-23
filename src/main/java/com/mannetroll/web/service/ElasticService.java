package com.mannetroll.web.service;

import java.util.List;

import org.joda.time.DateTime;

import com.mannetroll.web.model.ArrivalCompletedResponse;
import com.mannetroll.web.model.ArrivalCompletedStatusResponse;
import com.mannetroll.web.model.EdiPressureStatusResponse;
import com.mannetroll.web.model.EdiToStartResponse;
import com.mannetroll.web.model.PopularTimesResponse;

public interface ElasticService {

    public static final String ARRIVALCOMPLETED = "arrivalcompleted";

    public static final String POPULARTIMES = "populartimes";

    public Long countItems(long gte, long lte);

    public List<String[]> nokey(DateTime fromDay);

    /*
     * MARKET
     */

    public List<String[]> volume(DateTime fromDay, DateTime toDay, String service, String consignor);

    public EdiToStartResponse edi2Start(String consignor);

    public ArrivalCompletedResponse arrivalCompleted(String spid, Integer percentile);

    public ArrivalCompletedResponse arrivalCompletedHour(String spid);

    public ArrivalCompletedResponse arrivalCompleted(String spid, Integer dayofweek, Integer percentile);

    public ArrivalCompletedResponse arrivalCompletedHour(String spid, Integer dayofweek);

    public PopularTimesResponse popularTimes(String spid, String cc);

    public ArrivalCompletedStatusResponse arrivalCompletedStatus();

    public EdiPressureStatusResponse ediPressureStatus();

    public ArrivalCompletedResponse arrivalCompleted(String spid, Integer percentile, DateTime date, Integer weeks);

    public PopularTimesResponse popularTimes(String spid, String cc, DateTime fromDay, DateTime toDay);
}
