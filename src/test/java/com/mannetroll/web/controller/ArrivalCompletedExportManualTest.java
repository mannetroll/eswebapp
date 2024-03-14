package com.mannetroll.web.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mannetroll.web.service.ElasticService;
import com.mannetroll.web.service.impl.ElasticServiceImpl;

public class ArrivalCompletedExportManualTest {
    private static final Logger LOGGER = LogManager.getLogger(ArrivalCompletedExportManualTest.class);
    private static DateTimeFormatter DAY = DateTimeFormat.forPattern("yyyyMMdd");

    //
    // EXPORT
    //
    @Test
    public void testArrivalCompletedExport() throws IOException {
        ArrivalCompletedController controller = new ArrivalCompletedController();
        ElasticService elasticService = new ElasticServiceImpl(ClientFactory.getJestClient());
        controller.setElasticService(elasticService);
        controller.setDefaultPercentile(95);

        HttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.arrivalCompletedExport(10000, request, response);
        LOGGER.info("body: \r\n" + response.getContentAsString());
        
        PrintWriter writer = new PrintWriter(
                "arrivalcompleted/ArrivalCompletedExport-" + DAY.print(new DateTime()) + ".csv");
        writer.write(response.getContentAsString());
        writer.flush();
        writer.close();

        LOGGER.info("Done!");
    }

}
