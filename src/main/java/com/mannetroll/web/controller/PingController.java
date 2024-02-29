package com.mannetroll.web.controller;

import java.util.Date;
import java.util.Random;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mannetroll.metrics.helper.Constants;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/*
 * curl -is http://localhost:8080/process
 */

@RestController
@Api(value = "Process")
public class PingController {
	private static final String PROCESS = "/process";
	private static Random random = new Random();

	@ApiOperation(value = "process", notes = "process")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 400, message = "Bad request") })
	@RequestMapping(value = "/process/{action}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)

	public ResponseEntity<String> ping() throws InterruptedException {
		ThreadContext.put(Constants.METRICS_NAME, PROCESS);
		String response = (new Date()).toString();
		long sleep = PingController.nextGaussian();
		Thread.sleep(sleep);
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}

	public static Long nextGaussian() {
		double val = random.nextGaussian() * 500 + 1000;
		return Math.abs((long) Math.round(val));
	}

}