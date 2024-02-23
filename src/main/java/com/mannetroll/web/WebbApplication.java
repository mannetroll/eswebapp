package com.mannetroll.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import com.mannetroll.metrics.helper.AccessMetricServletFilter;
import com.mannetroll.metrics.helper.Constants;
import com.mannetroll.web.config.Settings;
import com.mannetroll.web.filter.HttpEtagFilter;
import com.mannetroll.web.filter.LastModifiedHeaderFilter;
import com.mannetroll.web.filter.TimerInfoFilter;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.client.config.HttpClientConfig.Builder;
import io.searchbox.client.http.JestHttpClient;

@SpringBootApplication
public class WebbApplication {
	private final static Logger LOGGER = LoggerFactory.getLogger(WebbApplication.class);
	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	private Settings settings;

	static {
		MDC.put(Constants.NANOTIME, String.valueOf(System.nanoTime()));
	}

	@Bean
	public FilterRegistrationBean<AccessMetricServletFilter> accessMetricServletFilter() {
		FilterRegistrationBean<AccessMetricServletFilter> registration = new FilterRegistrationBean<AccessMetricServletFilter>();
		registration.setFilter(new AccessMetricServletFilter());
		registration.addUrlPatterns("/*");
		registration.setName("accessMetricServletFilter");
		LOGGER.info("### FilterRegistrationBean: AccessMetricServletFilter");
		return registration;
	}

	@Bean
	public FilterRegistrationBean<TimerInfoFilter> itemsTimerInfoFilter() {
		FilterRegistrationBean<TimerInfoFilter> registration = new FilterRegistrationBean<TimerInfoFilter>();
		TimerInfoFilter filter = new TimerInfoFilter();
		registration.setFilter(filter);
		registration.addUrlPatterns("/*");
		registration.setName("timerInfoFilter");
		LOGGER.info("### FilterRegistrationBean: timerInfoFilter");
		return registration;
	}

	@Bean
	public Filter httpEtagFilter() {
		return new HttpEtagFilter();
	}

	@Bean
	public Filter lastModifiedHeaderFilter() {
		return new LastModifiedHeaderFilter();
	}

	@Bean
	JestClient jestClient() {
		LOGGER.info("eshost: " + settings.getEshost());
		LOGGER.info("cluster: " + settings.getCluster());
		List<Header> headers = new ArrayList<Header>();
		headers.add(new BasicHeader("X-Found-Cluster", settings.getCluster()));
		String basic = new String(Base64.encodeBase64(settings.getShield().getBytes()));
		headers.add(new BasicHeader("Authorization", "Basic " + basic));

		JestClientFactory factory = new JestClientFactory() {
			@Override
			protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder) {
				return builder.setDefaultHeaders(headers);
			}
		};

		Builder builder = new HttpClientConfig.Builder(settings.getEshost()).multiThreaded(true).discoveryEnabled(false)
				.connTimeout(1000).readTimeout(settings.getTimeout());
		factory.setHttpClientConfig(builder.build());
		return (JestHttpClient) factory.getObject();
	}

	@Scheduled(initialDelay = 3 * 1000, fixedRate = 2000)
	public void ping() {
		try {
			LOGGER.info("ping");
			restTemplate.getForEntity("http://localhost:8080/ping", String.class);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(WebbApplication.class, args);
		LOGGER.info("Done!");
	}
}
