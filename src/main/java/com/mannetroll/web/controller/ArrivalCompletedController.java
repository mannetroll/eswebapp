package com.mannetroll.web.controller;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Hours;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.mannetroll.metrics.LogKeys;
import com.mannetroll.servicepoints.ServicePoint;
import com.mannetroll.web.config.Settings;
import com.mannetroll.web.model.ApiError;
import com.mannetroll.web.model.ArrivalCompletedResponse;
import com.mannetroll.web.model.ArrivalCompletedStatusResponse;
import com.mannetroll.web.model.Fault;
import com.mannetroll.web.service.ElasticService;
import com.mannetroll.web.util.ServicePointUtil2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/*
 * time curl -is http://localhost:8080/arrivalcompleted/spid/567566
 * time curl -is http://localhost:8080/arrivalcompleted/spid/567566/dayofweek/1
 * time curl -is http://localhost:8080/arrivalcompleted/spid/307869/dayofweek/3
 * time curl -is http://localhost:8080/arrivalcompleted/spid/307869?percentile=50
 * time curl -is http://localhost:8080/arrivalcompleted/spid/307869/percentile/95/date/20220829/weeks/8
 *
 * time curl -is http://localhost:8080/arrivalcompletedcache
 *
 */

@RestController
@Api(value = "ArrivalCompleted")
public class ArrivalCompletedController {
	private static final Logger LOGGER = LogManager.getLogger(ArrivalCompletedController.class);
	private static final String ARRIVALCOMPLETED_URI = "/arrivalcompleted/spid/{spid}";
	private static final String ARRIVALCOMPLETED_DAY_URI = "/arrivalcompleted/spid/{spid}/dayofweek/{dayofweek}";
	private static final String ARRIVALCOMPLETED_STATUS = "arrivalcompletedstatus";
	private static final String ARRIVALCOMPLETED_STATUS_URI = "/arrivalcompletedstatus";
	private static final String ARRIVALCOMPLETED_EXPORT = "arrivalcompletedexport";
	private static final String ARRIVALCOMPLETED_EXPORT_URI = "/arrivalcompletedexport";
	private static final String ARRIVALCOMPLETED_LIST = "arrivalcompletedlist";
	private static final String ARRIVALCOMPLETED_LIST_URI = "/arrivalcompletedlist/{spid}";
	private static final String ARRIVALCOMPLETED_CACHE = "arrivalcompletedcache";
	private static final String ARRIVALCOMPLETED_CACHE_URI = "/arrivalcompletedcache";
	private static final DateTime START = new DateTime();
	private static final String SEP = ";";
	private static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
	private static DateTimeFormatter intdate = DateTimeFormat.forPattern("yyyyMMdd");

	static {
		formatter = formatter.withZone(DateTimeZone.forID("GMT"));
		intdate = intdate.withZone(DateTimeZone.forID("GMT"));
	}

	@Autowired
	private Settings settings;

	@Autowired
	private ElasticService elasticService;

	@Value("${kpis.percentile}")
	private Integer defaultPercentile;

	void setElasticService(ElasticService elasticService) {
		this.elasticService = elasticService;
	}

	public void setDefaultPercentile(Integer defaultPercentile) {
		this.defaultPercentile = defaultPercentile;
	}

	public static String toDate() {
		return ArrivalCompletedController.formatter.print(System.currentTimeMillis());
	}

	public static Integer toDateInteger() {
		return Integer.parseInt(ArrivalCompletedController.intdate.print(System.currentTimeMillis()));
	}

	public static String toDateString(Integer date) {
		return ArrivalCompletedController.formatter.print(toDate(date).getMillis());
	}

	public static DateTime toDate(Integer date) {
		return ArrivalCompletedController.intdate.parseDateTime(String.valueOf(date));
	}

	@SuppressWarnings("deprecation")
	@ApiOperation(value = "arrivalcompleteddayofweek", notes = "notes", response = ArrivalCompletedResponse.class)
	@RequestMapping(value = "/arrivalcompleted/spid/{spid}/dayofweek/{dayofweek}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<ArrivalCompletedResponse> arrivalCompletedDayOfWeek(
			@ApiParam(value = "ServicePointID") @PathVariable("spid") String spid,
			@ApiParam(value = "DayOfWeek") @PathVariable("dayofweek") Integer dayofweek,
			@ApiParam(value = "Percentile") @RequestParam(value = "percentile", required = false) Integer percentile)
			throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, ARRIVALCOMPLETED_DAY_URI);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, ElasticService.ARRIVALCOMPLETED);
		try {
			if (percentile == null || percentile < 0 || percentile > 100) {
				percentile = defaultPercentile;
			}
			//
			// ArrivalCompletedResponse data = elasticService.arrivalCompleted(spid,
			// dayofweek, percentile);
			// Cached method for all dayofweek
			//
			ArrivalCompletedResponse tmp = elasticService.arrivalCompleted(spid, percentile);
			//
			// One dayofweek
			//
			ArrivalCompletedResponse data = new ArrivalCompletedResponse();
			data.setServicePointId(tmp.getServicePointId());
			data.setName(tmp.getName());
			data.setPercentile(tmp.getPercentile());
			data.setWeeks(tmp.getWeeks());
			List<Map<String, Object>> allDays = tmp.getArrivalCompleted();
			for (Map<String, Object> map : allDays) {
				if (dayofweek == Integer.valueOf((int) map.get("dayOfWeek"))) {
					List<Map<String, Object>> arrivalCompleted = data.getArrivalCompleted();
					// copy 4
					Map<String, Object> copy = new TreeMap<String, Object>();
					copy.put("day", map.get("day"));
					copy.put("dayOfWeek", map.get("dayOfWeek"));
					copy.put("hour", map.get("hour"));
					copy.put("hourOfDay", map.get("hourOfDay"));
					arrivalCompleted.add(copy);
					break;
				}
			}
			return new ResponseEntity<>(data, HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			ApiError apiError = new ApiError();
			apiError.addFault(new Fault("001", e.getMessage()));
			ArrivalCompletedResponse body = new ArrivalCompletedResponse(apiError);
			body.setServicePointId(spid);
			return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@SuppressWarnings("deprecation")
	@ApiOperation(value = "arrivalcompleted", notes = "notes", response = ArrivalCompletedResponse.class)
	@RequestMapping(value = "/arrivalcompleted/spid/{spid}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<ArrivalCompletedResponse> arrivalCompleted(
			@ApiParam(value = "ServicePointID") @PathVariable("spid") String spid,
			@ApiParam(value = "Percentile") @RequestParam(value = "percentile", required = false) Integer percentile)
			throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, ARRIVALCOMPLETED_URI);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, ElasticService.ARRIVALCOMPLETED);
		try {
			if (percentile == null || percentile < 0 || percentile > 100) {
				percentile = defaultPercentile;
			}
			ArrivalCompletedResponse data = elasticService.arrivalCompleted(spid, percentile);
			return new ResponseEntity<>(data, HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			ApiError apiError = new ApiError();
			apiError.addFault(new Fault("001", e.getMessage()));
			ArrivalCompletedResponse body = new ArrivalCompletedResponse(apiError);
			body.setServicePointId(spid);
			return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@SuppressWarnings("deprecation")
	@ApiOperation(value = "arrivalcompletedparam", notes = "notes", response = ArrivalCompletedResponse.class)
	@RequestMapping(value = "/arrivalcompleted/spid/{spid}/percentile/{percentile}/date/{date}/weeks/{weeks}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<ArrivalCompletedResponse> arrivalCompletedParam(
			@ApiParam(value = "ServicePointID") @PathVariable("spid") String spid,
			@ApiParam(value = "percentile") @PathVariable("percentile") Integer percentile,
			@ApiParam(value = "date") @PathVariable("date") Integer date,
			@ApiParam(value = "weeks") @PathVariable("weeks") Integer weeks) throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, ARRIVALCOMPLETED_URI);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, ElasticService.ARRIVALCOMPLETED);
		try {
			if (weeks < 1 || weeks > 52) {
				weeks = 8;
			}
			if (percentile < 1 || percentile > 100) {
				percentile = 95;
			}
			Integer dateInteger = toDateInteger();
			if (date < 20210501 || date > dateInteger) {
				date = dateInteger;
			}
			ArrivalCompletedResponse data = elasticService.arrivalCompleted(spid, percentile, toDate(date), weeks);
			return new ResponseEntity<>(data, HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			ApiError apiError = new ApiError();
			apiError.addFault(new Fault("001", e.getMessage()));
			ArrivalCompletedResponse body = new ArrivalCompletedResponse(apiError);
			body.setServicePointId(spid);
			return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/*
	 ******************************************* HOUR
	 */

	@SuppressWarnings("deprecation")
	@ApiOperation(value = "arrivalcompleteddayofweek2", notes = "notes", response = ArrivalCompletedResponse.class)
	@RequestMapping(value = "/arrivalcompleted2/spid/{spid}/dayofweek/{dayofweek}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<ArrivalCompletedResponse> arrivalCompletedDayOfWeek2(
			@ApiParam(value = "ServicePointID") @PathVariable("spid") String spid,
			@ApiParam(value = "DayOfWeek") @PathVariable("dayofweek") Integer dayofweek) throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, ARRIVALCOMPLETED_URI);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, ElasticService.ARRIVALCOMPLETED + "2");
		try {
			ArrivalCompletedResponse data = elasticService.arrivalCompletedHour(spid, dayofweek);
			return new ResponseEntity<>(data, HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			ApiError apiError = new ApiError();
			apiError.addFault(new Fault("001", e.getMessage()));
			ArrivalCompletedResponse body = new ArrivalCompletedResponse(apiError);
			body.setServicePointId(spid);
			return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@SuppressWarnings("deprecation")
	@ApiOperation(value = "arrivalcompleted2", notes = "notes", response = ArrivalCompletedResponse.class)
	@RequestMapping(value = "/arrivalcompleted2/spid/{spid}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<ArrivalCompletedResponse> arrivalCompleted2(
			@ApiParam(value = "ServicePointID") @PathVariable("spid") String spid) throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, ARRIVALCOMPLETED_URI);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, ElasticService.ARRIVALCOMPLETED + "2");
		try {
			ArrivalCompletedResponse data = elasticService.arrivalCompletedHour(spid);
			return new ResponseEntity<>(data, HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			ApiError apiError = new ApiError();
			apiError.addFault(new Fault("001", e.getMessage()));
			ArrivalCompletedResponse body = new ArrivalCompletedResponse(apiError);
			body.setServicePointId(spid);
			return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@SuppressWarnings("deprecation")
	@ApiOperation(value = "arrivalcompletedstatus", notes = "notes", response = ArrivalCompletedResponse.class)
	@RequestMapping(value = "/arrivalcompletedstatus", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<ArrivalCompletedStatusResponse> arrivalCompletedStatus() throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, ARRIVALCOMPLETED_STATUS_URI);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, ARRIVALCOMPLETED_STATUS);
		try {
			ArrivalCompletedStatusResponse data = elasticService.arrivalCompletedStatus();
			return new ResponseEntity<>(data, HttpStatus.OK);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			ApiError apiError = new ApiError();
			apiError.addFault(new Fault("001", e.getMessage()));
			ArrivalCompletedStatusResponse body = new ArrivalCompletedStatusResponse(apiError);
			return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String[] weekdays = { "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY" };

	private String getValue(String string) {
		if (string != null) {
			return string;
		}
		return "";
	}

	@ApiOperation(value = "arrivalcompletedexport", notes = "notes", response = ArrivalCompletedResponse.class)
	@RequestMapping(value = "/arrivalcompletedexport", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public void arrivalCompletedExport(@RequestParam(value = "size", required = false) Integer size,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, ARRIVALCOMPLETED_EXPORT_URI);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, ARRIVALCOMPLETED_EXPORT);
		if (size == null) {
			size = 100;
		}
		final Instant start = Instant.now();
		response.setContentType(MediaType.TEXT_PLAIN_VALUE);
		ServletOutputStream out = response.getOutputStream();
		Long loop = 1L;
		// columns
		out.print("N;date;SSID;name;");
		for (int i = 0; i <= 6; i++) {
			String day = weekdays[i];
			out.print(day + "_hour" + SEP);
			out.print(day + "_count" + SEP);
			out.print(day + "_avg" + SEP);
			out.print(day + "_std" + SEP);
		}
		out.println();
		Map<String, ServicePoint> servicePoints = new TreeMap<>(ServicePointUtil2.getServicePoints());
		String date = toDate();
		for (Entry<String, ServicePoint> entry : servicePoints.entrySet()) {
			final ServicePoint sp = entry.getValue();
			if ("SE".equals(sp.getDeliveryAddress().getCountryCode())) {
				if (sp.getDeliveryAddress().getPostalCode().startsWith("41")) { // Gothenburg
					final String servicePointId = "" + sp.getServicePointId();
					loop = servicePoint2CSV(out, loop, servicePointId, date);
				}
			}
			if (loop > size) {
				break;
			}
			if ((loop % 100) == 0) {
				LOGGER.info("loop: " + loop);
			}
		}
		long elapsed = Duration.between(start, Instant.now()).toMillis();
		Map<String, Object> lmap = new HashMap<String, Object>();
		lmap.put(LogKeys.MESSAGE, "arrivalCompletedStatus: elapsed: " + elapsed);
		lmap.put("elapsed", elapsed);
		LOGGER.info(lmap);
	}

	@ApiOperation(value = "arrivalcompletedlist", notes = "notes", response = ArrivalCompletedResponse.class)
	@RequestMapping(value = "/arrivalcompletedlist/{spid}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public void arrivalCompletedIds(@ApiParam(value = "ServicePointID") @PathVariable("spid") String[] spid,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, ARRIVALCOMPLETED_LIST_URI);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, ARRIVALCOMPLETED_LIST);
		final Instant start = Instant.now();
		ServletOutputStream out = response.getOutputStream();
		Long loop = 1L;
		// columns
		out.print("N;date;SSID;name;");
		for (int i = 0; i <= 6; i++) {
			String day = weekdays[i];
			out.print(day + "_hour" + SEP);
			out.print(day + "_count" + SEP);
			out.print(day + "_avg" + SEP);
			out.print(day + "_std" + SEP);
		}
		out.println();

		String date = toDate();
		List<String> idList = Arrays.asList(spid);
		for (String servicePointId : idList) {
			loop = servicePoint2CSV(out, loop, servicePointId, date);
		}

		long elapsed = Duration.between(start, Instant.now()).toMillis();
		Map<String, Object> lmap = new HashMap<String, Object>();
		lmap.put(LogKeys.MESSAGE, "arrivalcompletedids: elapsed: " + elapsed);
		lmap.put("elapsed", elapsed);
		LOGGER.info(lmap);
	}

	private Long servicePoint2CSV(ServletOutputStream out, Long loop, final String servicePointId, String date)
			throws IOException {
		out.print(String.valueOf(loop++) + SEP);
		out.print(date + SEP);
		ArrivalCompletedResponse acr = elasticService.arrivalCompleted(servicePointId, defaultPercentile);
		out.print(String.valueOf(acr.getServicePointId()) + SEP);
		out.print(String.valueOf(acr.getName()) + SEP);
		List<Map<String, Object>> list = acr.getArrivalCompleted();
		Map<String, String> week = new LinkedHashMap<String, String>();
		for (Map<String, Object> map : list) {
			String day = (String) map.get("day");
			week.put(day + "_hour", (String) map.get("hour"));
			week.put(day + "_count", "" + (Long) map.get("count"));
			week.put(day + "_avg", "" + (Float) map.get("avg"));
			week.put(day + "_std", "" + (Float) map.get("std"));
		}
		for (int i = 0; i < 6; i++) {
			String day = weekdays[i];
			out.print(getValue(week.get(day + "_hour")) + SEP);
			out.print(getValue(week.get(day + "_count")) + SEP);
			out.print(getValue(week.get(day + "_avg")) + SEP);
			out.print(getValue(week.get(day + "_std")) + SEP);
		}
		String day = weekdays[6];
		out.print(getValue(week.get(day + "_hour")) + SEP);
		out.print(getValue(week.get(day + "_count")));
		out.print(getValue(week.get(day + "_avg")));
		out.print(getValue(week.get(day + "_std")));
		out.println();
		return loop;
	}

	/*
	 ******************************************* HOUR
	 */

	@Autowired
	CacheManager cacheManager;

	@ApiOperation(value = "arrivalcompletedcache", notes = "notes", response = ArrivalCompletedResponse.class)
	@RequestMapping(value = "/arrivalcompletedcache", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> arrivalcompletedcache() throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, ARRIVALCOMPLETED_CACHE_URI);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, ARRIVALCOMPLETED_CACHE);
		Map<String, Object> lmap = new TreeMap<String, Object>();
		DateTime now = new DateTime();
		lmap.put("Date", now);
		lmap.put("HostName", getHostName());
		lmap.put("TimeToLive", settings.getTimetolive());
		lmap.put("Uptime", getUptime(now));
		try {
			Cache cache = cacheManager.getCache(ElasticService.ARRIVALCOMPLETED);
			Object nativeCache = cache.getNativeCache();
			LOGGER.info("nativeCache: " + nativeCache);
			if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
				@SuppressWarnings("rawtypes")
				com.github.benmanes.caffeine.cache.Cache map = (com.github.benmanes.caffeine.cache.Cache) nativeCache;
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
