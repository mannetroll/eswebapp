package com.mannetroll.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.client.config.HttpClientConfig.Builder;
import io.searchbox.client.http.JestHttpClient;

public class ClientFactory {

	public static JestClient getJestClient() {
		String eshost = "https://proxy.elastic.se";
		String cluster = "00000000000000000000000000000000";
		String shield = "elastic:elastic";
		List<Header> headers = new ArrayList<Header>();
		headers.add(new BasicHeader("X-Found-Cluster", cluster));
		String basic = new String(Base64.encodeBase64(shield.getBytes()));
		headers.add(new BasicHeader("Authorization", "Basic " + basic));

		JestClientFactory factory = new JestClientFactory() {
			@Override
			protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder) {
				return builder.setDefaultHeaders(headers);
			}
		};
		Builder httpbuilder = new HttpClientConfig.Builder(eshost).multiThreaded(true).discoveryEnabled(false)
				.connTimeout(1000).readTimeout(10000);
		factory.setHttpClientConfig(httpbuilder.build());
		JestClient client = (JestHttpClient) factory.getObject();
		return client;
	}
}
