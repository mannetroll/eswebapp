package com.mannetroll.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mannetroll.web.controller.ArrivalCompletedController;
import com.mannetroll.web.model.ArrivalCompletedResponse;
import com.mannetroll.web.model.ArrivalCompletedStatusResponse;
import com.mannetroll.web.service.ElasticService;
import com.mannetroll.web.service.impl.ElasticServiceImpl;
import com.mannetroll.web.util.JsonUtil;

public class ArrivalCompletedControllerManualTest {
    private static final Logger LOGGER = LogManager.getLogger(ArrivalCompletedControllerManualTest.class);

    private ArrivalCompletedController controller;

    @BeforeEach
    public void before() {
        controller = new ArrivalCompletedController();
        ElasticService elasticService = new ElasticServiceImpl(ClientFactory.getJestClient());
        controller.setElasticService(elasticService);
        controller.setDefaultPercentile(95);
    }

    @Test
    public void testArrivalCompleted() throws IOException {
        ResponseEntity<ArrivalCompletedResponse> responseEntity = controller.arrivalCompleted("776172", 95);
        ArrivalCompletedResponse body = responseEntity.getBody();
        LOGGER.info("body: " + JsonUtil.toPretty(body));
    }

    @Test
    public void testArrivalCompletedStatus() throws IOException {
        ResponseEntity<ArrivalCompletedStatusResponse> responseEntity = controller.arrivalCompletedStatus();
        ArrivalCompletedStatusResponse body = responseEntity.getBody();
        LOGGER.info("body: " + JsonUtil.toPretty(body));
    }

    @Test
    public void testArrivalCompletedIds() throws IOException {
        HttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] spid = { "306185", "306187" };
        controller.arrivalCompletedIds(spid, request, response);
        LOGGER.info("body: \r\n" + response.getContentAsString());
    }

    @Test
    public void testArrivalCompletedDayOfWeek() throws IOException {
        ResponseEntity<ArrivalCompletedResponse> acdow = controller.arrivalCompletedDayOfWeek("306185", 1, 95);
        LOGGER.info("body: " + JsonUtil.toPretty(acdow.getBody()));
    }

}
