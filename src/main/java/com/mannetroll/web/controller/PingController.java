package com.mannetroll.web.controller;

import java.util.Date;

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
 * curl -is http://localhost:8080/ping
 */

@RestController
@Api(value = "Ping")
public class PingController {
	@ApiOperation(value = "ping", notes = "ping")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
			@ApiResponse(code = 400, message = "Bad request") })
	@RequestMapping(value = "/ping", method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
	public ResponseEntity<String> ping() {
		ThreadContext.put(Constants.METRICS_NAME, "ping");
		String response = (new Date()).toString();
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}

}
