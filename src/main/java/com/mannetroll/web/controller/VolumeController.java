package com.mannetroll.web.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mannetroll.metrics.LogKeys;
import com.mannetroll.web.model.ApiError;
import com.mannetroll.web.model.Fault;
import com.mannetroll.web.model.VolumeResponse;
import com.mannetroll.web.service.ElasticService;
import com.mannetroll.web.util.JsonUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/*
 * curl -is http://localhost:8080/volume/20230213/20230217/service/19/consignor/apotea.se
 *
 * curl -is http://localhost:8080/count/20230213/20230217
 *
 */

@RestController
@Api(value = "Volume")
public class VolumeController {
	private static final Logger LOGGER = LogManager.getLogger(VolumeController.class);
	private static final String VOLUME = "volume";
	private static final String MARKET_VOLUME = "/volume/{from}/{to}/service/{service}/consignor/{consignor}";
	private static DateTimeFormatter DAY = DateTimeFormat.forPattern("yyyyMMdd");
	private static final String C = ",";

	static {
		DAY = DAY.withZone(DateTimeZone.forID("CET"));
	}

	@Autowired
	private ElasticService elasticService;

	void setElasticService(ElasticService elasticService) {
		this.elasticService = elasticService;
	}

	@ApiOperation(value = "volume", notes = "volume", response = String.class)
	@RequestMapping(value = "/volume/{from}/{to}/service/{service}/consignor/{consignor}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public void volume(@PathVariable("from") Integer from, @PathVariable("to") Integer to,
			@PathVariable("service") String service, @PathVariable("consignor") String consignor,
			HttpServletResponse response) throws IOException {
		ThreadContext.put(LogKeys.METRICS_NAME, MARKET_VOLUME);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, VOLUME);
		ServletOutputStream outputStream = response.getOutputStream();
		if (to < from) {
			outputStream.println(JsonUtil.toJson(createError("to < from", "001")));
			return;
		}
		DateTime fromDay = DAY.parseDateTime(String.valueOf(from));
		DateTime toDay = DAY.parseDateTime(String.valueOf(to)).plusHours(24);
		if (Days.daysBetween(fromDay, toDay).getDays() > 123) {
			outputStream.println(JsonUtil.toJson(createError("to - from > 123", "002")));
			return;
		}
		try {
			List<String[]> data = elasticService.volume(fromDay, toDay, service, consignor);
			csv(data, outputStream);
			response.setStatus(200);
			return;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			outputStream.println(JsonUtil.toJson(createError(e.getMessage(), "003")));
			response.setStatus(500);
			return;
		}
	}

	private void csv(List<String[]> data, ServletOutputStream out) throws IOException {
		long loop = 0;
		for (String[] row : data) {
			if (loop == 0) {
				loop++;
				out.print("N" + C);
			} else {
				out.print(String.valueOf(loop++) + C);
			}
			for (int i = 0; i < row.length; i++) {
				out.print(row[i]);
				if (i == (row.length - 1)) {
					out.println();
				} else {
					out.print(C);
				}
			}
		}
	}

	@ApiOperation(value = "count", notes = "count", response = String.class)
	@RequestMapping(value = "/count/{from}/{to}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String count(@PathVariable("from") Integer from, @PathVariable("to") Integer to,
			HttpServletResponse response) {
		ThreadContext.put(LogKeys.METRICS_NAME, MARKET_VOLUME);
		ThreadContext.put(LogKeys.METRICS_JAVA_METHOD, VOLUME);
		if (to < from) {
			return "to < from";
		}
		try {
			DateTime fromDay = DAY.parseDateTime(String.valueOf(from));
			DateTime toDay = DAY.parseDateTime(String.valueOf(to)).plusHours(24);
			Long count = elasticService.countItems(fromDay.getMillis(), toDay.getMillis());
			LOGGER.info("count: " + from + " -> " + to + ": " + count);
			response.setStatus(200);
			return String.valueOf(count);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			response.setStatus(500);
			return e.getMessage();
		}
	}

	protected ResponseEntity<VolumeResponse> createError(String explanationText, String faultCode) {
		ApiError apiError = new ApiError();
		apiError.addFault(new Fault(faultCode, explanationText));
		VolumeResponse response = new VolumeResponse(apiError);
		LOGGER.info("createError: " + explanationText + ", " + faultCode);
		return new ResponseEntity<VolumeResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
