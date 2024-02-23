package com.mannetroll.web.controller;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.mannetroll.web.controller.PopularTimesController;
import com.mannetroll.web.model.PopularTimesResponse;
import com.mannetroll.web.service.ElasticService;
import com.mannetroll.web.service.impl.ElasticServiceImpl;
import com.mannetroll.web.util.JsonUtil;

public class PopularTimesControllerManualTest {
    private static final Logger LOGGER = LogManager.getLogger(PopularTimesControllerManualTest.class);

    @Test
    public void testVolumeSWE() throws IOException {
        PopularTimesController controller = new PopularTimesController();
        ElasticService elasticService = new ElasticServiceImpl(ClientFactory.getJestClient());
        controller.setElasticService(elasticService);
        ResponseEntity<PopularTimesResponse> responseEntity = controller.populartimes("336538");
        PopularTimesResponse body = responseEntity.getBody();
        LOGGER.info("body: " + JsonUtil.toPretty(body));
    }

    @Test
    public void testVolumeDNK() throws IOException {
        PopularTimesController controller = new PopularTimesController();
        ElasticService elasticService = new ElasticServiceImpl(ClientFactory.getJestClient());
        controller.setElasticService(elasticService);
        ResponseEntity<PopularTimesResponse> responseEntity = controller.populartimesCountry("1503", "DK");
        PopularTimesResponse body = responseEntity.getBody();
        LOGGER.info("body: " + JsonUtil.toPretty(body));
    }
}
