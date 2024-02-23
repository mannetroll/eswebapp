package com.mannetroll.web.config;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final Logger LOGGER = LogManager.getLogger(CacheConfig.class);

    @Autowired
    private Settings settings;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        LOGGER.info("cacheManager: " + cacheManager);
        LOGGER.info("timetolive: " + settings.getTimetolive() + " HOURS");
        return cacheManager;
    }

    @Bean
    public Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder().expireAfterWrite(settings.getTimetolive(), TimeUnit.HOURS).maximumSize(10000)
                .recordStats();
    }

}
