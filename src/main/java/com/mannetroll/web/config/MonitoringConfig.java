package com.mannetroll.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;
import com.mannetroll.metrics.codahale.AppenderMetricsManager;

@Configuration
public class MonitoringConfig {
	private final static Logger logger = LoggerFactory.getLogger(MonitoringConfig.class);

	@Autowired
	private MetricRegistry registry;

	@Bean
	public MetricRegistry metricRegistry() {		
		registry = new MetricRegistry();
		try {
			AppenderMetricsManager.setMetricRegistry(registry);
			logger.info("MetricRegistry: " + registry);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return registry;
	}

}