package com.mannetroll.web.controller;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mannetroll.web.service.ElasticService;
import com.mannetroll.web.service.impl.ElasticServiceImpl;

public class VolumeControllerManualTest {
    private VolumeController controller;

    @Test
    public void testVolume() throws IOException {
        controller = new VolumeController();
        ElasticService elasticService = new ElasticServiceImpl(ClientFactory.getJestClient());
        controller.setElasticService(elasticService);
        MockHttpServletResponse mock = new MockHttpServletResponse();
        controller.volume(20230213, 20230217, "19", "apotea.se", mock);
    }

    @Test
    public void testCount() {
        controller = new VolumeController();
        ElasticService elasticService = new ElasticServiceImpl(ClientFactory.getJestClient());
        controller.setElasticService(elasticService);
        MockHttpServletResponse mock = new MockHttpServletResponse();
        controller.count(20230213, 20230217, mock);
    }
}
