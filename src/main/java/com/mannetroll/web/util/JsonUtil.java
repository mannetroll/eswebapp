package com.mannetroll.web.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class JsonUtil {
    private final static Logger LOG = LoggerFactory.getLogger(JsonUtil.class);
    private static ObjectMapper mapper;
    private static ObjectMapper pretty;

    static {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.registerModule(new JodaModule());
        //
        pretty = new ObjectMapper();
        pretty.setSerializationInclusion(Include.NON_NULL);
        pretty.enable(SerializationFeature.INDENT_OUTPUT);
        pretty.registerModule(new JodaModule());
    }

    public static String toJson(Object object) {
        try {
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, object);
            return sw.toString();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return "{\"message\": \"toJson failed\"}";
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(String json) {
        Map<String, Object> dto = new TreeMap<String, Object>();
        try {
            dto = mapper.readValue(json, Map.class);
        } catch (IOException e) {
            LOG.info(e.getMessage(), e);
        }
        return dto;
    }

    public static String toPretty(Object object) {
        try {
            StringWriter sw = new StringWriter();
            pretty.writeValue(sw, object);
            return sw.toString();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return "{\"message\": \"toPretty failed: " + e.getMessage() + "\"}";
        }
    }

}
