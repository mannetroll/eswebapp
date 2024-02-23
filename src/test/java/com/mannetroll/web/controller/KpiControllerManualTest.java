package com.mannetroll.web.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.mannetroll.web.controller.KpiController;
import com.mannetroll.web.model.KpiResponse;
import com.mannetroll.web.util.JsonUtil;

public class KpiControllerManualTest {
    private static final Logger LOGGER = LogManager.getLogger(KpiControllerManualTest.class);

    @Test
    public void test() {
        KpiController controller = new KpiController();
        controller.setJestClient(ClientFactory.getJestClient());
        ResponseEntity<KpiResponse> entity = controller.kpi("20337765803SE");
        KpiResponse body = entity.getBody();
        LOGGER.info(JsonUtil.toJson(body));
    }
}
