package com.mannetroll.web.controller;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.mannetroll.metrics.LogKeys;
import com.mannetroll.web.config.Settings;
import com.mannetroll.web.model.ApiError;
import com.mannetroll.web.model.ArrivalCompletedResponse;
import com.mannetroll.web.model.Fault;
import com.mannetroll.web.model.PopularTimesResponse;
import com.mannetroll.web.service.ElasticService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/*
 * curl -is http://localhost:8080/populartimes/spid/567566
 * curl -is http://localhost:8080/populartimes/spid/2228128
 * curl -is http://localhost:8080/populartimes/spid/336538
 * curl -is http://localhost:8080/populartimes/spid/691073 
 * curl -is http://localhost:8080/populartimes/spid/691066
 * 
 * curl -is http://localhost:8080/populartimes/spid/1503/cc/DK
 *
 * curl -is http://localhost:8080/populartimes/spid/336538/cc/SE/from/20221001/to/20231001
 * 
 * time curl -is http://localhost:8080/populartimes/spid/336538
 * time curl -is http://localhost:8080/populartimes/spid/336538/cc/SE
 * time curl -is http://localhost:8080/populartimes/spid/1503/cc/DK
 * time curl -is http://localhost:8080/populartimescache
 */

@RestController
@Api(value = "PopularTimes")
public class PopularTimesController {
    private static final Logger LOGGER = LogManager.getLogger(PopularTimesController.class);
    private static final DateTime START = new DateTime();
    private static final String POPULARTIMES = "populartimes";
    private static final String POPULARTIMES_URI = "/populartimes/spid/{spid}";
    private static DateTimeFormatter DAY = DateTimeFormat.forPattern("yyyyMMdd");
    private static final String SE = "SE";
    private static final String POPULARTIMES_CACHE = "populartimescache";
    private static final String POPULARTIMES_CACHE_URI = "/populartimescache";

    private static String toDate() {
        return PopularTimesController.DAY.print(System.currentTimeMillis());
    }

    @Autowired
    private Settings settings;

    @Autowired
    private ElasticService elasticService;

    void setElasticService(ElasticService elasticService) {
        this.elasticService = elasticService;
    }

    @ApiOperation(value = "populartimes", notes = "populartimes", response = PopularTimesResponse.class)
    @RequestMapping(value = "/populartimes/spid/{spid}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PopularTimesResponse> populartimes(@ApiParam(value = "ServicePointID")
    @PathVariable("spid") String spid) throws IOException {
        ThreadContext.put(LogKeys.METRICS_NAME, POPULARTIMES_URI);
        ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, POPULARTIMES);
        try {
            PopularTimesResponse data = elasticService.popularTimes(spid, SE);
            data.setDate(toDate());
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            ApiError apiError = new ApiError();
            apiError.addFault(new Fault("001", e.getMessage()));
            PopularTimesResponse body = new PopularTimesResponse(apiError);
            body.setServicePointId(spid);
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "populartimes", notes = "populartimes", response = PopularTimesResponse.class)
    @RequestMapping(value = "/populartimes/spid/{spid}/cc/{cc}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PopularTimesResponse> populartimesCountry(@ApiParam(value = "ServicePointID")
    @PathVariable("spid") String spid,
            @ApiParam(value = "CountryCode")
            @PathVariable("cc") String cc) throws IOException {
        ThreadContext.put(LogKeys.METRICS_NAME, POPULARTIMES_URI);
        ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, POPULARTIMES);
        try {
            PopularTimesResponse data = elasticService.popularTimes(spid, cc);
            data.setDate(toDate());
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            ApiError apiError = new ApiError();
            apiError.addFault(new Fault("001", e.getMessage()));
            PopularTimesResponse body = new PopularTimesResponse(apiError);
            body.setServicePointId(spid);
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /*
     * DO NOT USE DURING PEAK!!
     * 
    @ApiOperation(value = "populartimes", notes = "populartimes", response = PopularTimesResponse.class)
    @RequestMapping(value = "/populartimes/spid/{spid}/cc/{cc}/from/{from}/to/{to}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PopularTimesResponse> populartimesCountryTime(@ApiParam(value = "ServicePointID")
    @PathVariable("spid") String spid,
            @ApiParam(value = "CountryCode")
            @PathVariable("cc") String cc,
            @ApiParam(value = "From")
            @PathVariable("from") Integer from,
            @ApiParam(value = "To")
            @PathVariable("to") Integer to) throws IOException {
        ThreadContext.put(LogKeys.METRICS_NAME, POPULARTIMES_URI);
        ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, POPULARTIMES);
        try {
            DateTime fromDay = DAY.parseDateTime(String.valueOf(from));
            DateTime toDay = DAY.parseDateTime(String.valueOf(to)).plusHours(24);
            if (from < 20220101) {
                ApiError apiError = new ApiError();
                apiError.addFault(new Fault("001", "Only From: 2022-01-01"));
                PopularTimesResponse body = new PopularTimesResponse(apiError);
                body.setServicePointId(spid);
                return new ResponseEntity<>(body, HttpStatus.UNPROCESSABLE_ENTITY);
            }

            if (Days.daysBetween(fromDay, toDay).getDays() > 370) {
                ApiError apiError = new ApiError();
                apiError.addFault(new Fault("002", "Timespan max 1 year"));
                PopularTimesResponse body = new PopularTimesResponse(apiError);
                body.setServicePointId(spid);
                return new ResponseEntity<>(body, HttpStatus.UNPROCESSABLE_ENTITY);
            }
            PopularTimesResponse data = elasticService.popularTimes(spid, cc, fromDay, toDay);
            data.setDate(toDate());
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            ApiError apiError = new ApiError();
            apiError.addFault(new Fault("001", e.getMessage()));
            PopularTimesResponse body = new PopularTimesResponse(apiError);
            body.setServicePointId(spid);
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
     */

    @Autowired
    CacheManager cacheManager;

    @ApiOperation(value = "populartimescache", notes = "notes", response = ArrivalCompletedResponse.class)
    @RequestMapping(value = "/populartimescache", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> arrivalcompletedcache() throws IOException {
        ThreadContext.put(LogKeys.METRICS_NAME, POPULARTIMES_CACHE_URI);
        ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, POPULARTIMES_CACHE);
        Map<String, Object> lmap = new TreeMap<String, Object>();
        DateTime now = new DateTime();
        lmap.put("Date", now);
        lmap.put("HostName", getHostName());
        lmap.put("TimeToLive", settings.getTimetolive());
        lmap.put("Uptime", getUptime(now));
        try {
            Cache cache = cacheManager.getCache(ElasticService.POPULARTIMES);
            Object nativeCache = cache.getNativeCache();
            LOGGER.info("nativeCache: " + nativeCache);
            if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                @SuppressWarnings("rawtypes") com.github.benmanes.caffeine.cache.Cache map = (com.github.benmanes.caffeine.cache.Cache) nativeCache;
                CacheStats body = map.stats();
                lmap.put("hitCount", body.hitCount());
                lmap.put("hitRate", body.hitRate());
                lmap.put("missCount", body.missCount());
                lmap.put("missRate", body.missRate());
                lmap.put("loadSuccessCount", body.loadSuccessCount());
                lmap.put("loadFailureCount", body.loadFailureCount());
                lmap.put("requestCount", body.requestCount());
                lmap.put("totalLoadTime", body.totalLoadTime());
                lmap.put("evictionCount", body.evictionCount());
                lmap.put("evictionWeight", body.evictionWeight());
                lmap.put("estimatedSize", map.estimatedSize());
                LOGGER.info(lmap);
            }
            return new ResponseEntity<>(lmap, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            ApiError apiError = new ApiError();
            apiError.addFault(new Fault("001", e.getMessage()));
            return new ResponseEntity<>(lmap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private int getUptime(DateTime now) {
        return Hours.hoursBetween(START, now).getHours();
    }

    private String getHostName() {
        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Throwable th) {
            host = th.getMessage();
        }
        return host;
    }
}
