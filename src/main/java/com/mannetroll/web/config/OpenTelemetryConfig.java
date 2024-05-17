package com.mannetroll.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.semconv.SemanticAttributes;

@SuppressWarnings("deprecation")
@Configuration
public class OpenTelemetryConfig {

	@Bean
	public AutoConfigurationCustomizerProvider otelCustomizer() {
		return p -> p.addSamplerCustomizer((fallback, config) -> 
		RuleBasedRoutingSampler
		  .builder(SpanKind.SERVER, fallback)
		  .drop(SemanticAttributes.URL_PATH, "^/actuator")
		  .build());
	}
}