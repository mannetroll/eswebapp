package com.mannetroll.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import com.mannetroll.metrics.helper.AccessMetricServletFilter;
import com.mannetroll.metrics.helper.Constants;
import com.mannetroll.metrics.util.LogKeys;
import com.mannetroll.web.config.Settings;
import com.mannetroll.web.controller.PingController;
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
	private static final Logger LOG = LogManager.getLogger(WebbApplication.class);
	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	private Settings settings;

	static {
		ThreadContext.put(Constants.NANOTIME, String.valueOf(System.nanoTime()));
	}

	@Bean
	public FilterRegistrationBean<AccessMetricServletFilter> accessMetricServletFilter() {
		FilterRegistrationBean<AccessMetricServletFilter> registration = new FilterRegistrationBean<AccessMetricServletFilter>();
		registration.setFilter(new AccessMetricServletFilter());
		registration.addUrlPatterns("/*");
		registration.setName("accessMetricServletFilter");
		LOG.info("### FilterRegistrationBean: AccessMetricServletFilter");
		return registration;
	}

	@Bean
	public FilterRegistrationBean<TimerInfoFilter> itemsTimerInfoFilter() {
		FilterRegistrationBean<TimerInfoFilter> registration = new FilterRegistrationBean<TimerInfoFilter>();
		TimerInfoFilter filter = new TimerInfoFilter();
		registration.setFilter(filter);
		registration.addUrlPatterns("/*");
		registration.setName("timerInfoFilter");
		LOG.info("### FilterRegistrationBean: timerInfoFilter");
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
		LOG.info("eshost: " + settings.getEshost());
		LOG.info("cluster: " + settings.getCluster());
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
			String pickNRandom = pickNRandom(actionList, 1).get(0);
			Map<String, Object> logmap = new HashMap<>();
			long sleep = PingController.nextGaussian();
			logmap.put(LogKeys.DESCRIPTION, "Will sleep: " + sleep);
			logmap.put("pickNRandom", pickNRandom);
			LOG.info(logmap);
			Thread.sleep(sleep);
			//
			restTemplate.getForEntity("http://localhost:8080/process/" + pickNRandom, String.class);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	List<String> actionList = new LinkedList<String>(Arrays.asList("create", "delete", "move", "update"));

	public static List<String> pickNRandom(List<String> lst, int n) {
		List<String> copy = new ArrayList<String>(lst);
		Collections.shuffle(copy);
		return n > copy.size() ? copy.subList(0, copy.size()) : copy.subList(0, n);
	}

	public static void main(String[] args) {
		SpringApplication.run(WebbApplication.class, args);
		LOG.info("Done!");
	}
}
