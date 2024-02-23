package com.mannetroll.web.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mannetroll.web.controller.Checksum;

public class ChecksumTest {
    private static final Logger LOGGER = LogManager.getLogger(ChecksumTest.class);
    private static Checksum checksum = Checksum.instance();

    @Test
    public void test() {
        String itemid1 = "05308114610781M";
        LOGGER.info("itemid1: " + itemid1);
        String itemid2 = deleteChecksum(itemid1);
        LOGGER.info("trimmed: " + itemid2);
    }

    private static final int LENGTH_WITHOUT_CHECKSUM = 14;
    private static final int LENGTH_WITH_CHECKSUM = 15;
    public String deleteChecksum(String id) {
        if (id == null || id.length() != LENGTH_WITH_CHECKSUM) {
            return id;
        }
        try {
            char sign = id.charAt(LENGTH_WITHOUT_CHECKSUM);
            String externalId = id.substring(0, LENGTH_WITHOUT_CHECKSUM);
            char dpdChecksum = checksum.getDPDChecksum(externalId);
            LOGGER.info("dpdChecksum: " + dpdChecksum);
            if (sign == dpdChecksum) {
                return externalId;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return id;
    }

}
