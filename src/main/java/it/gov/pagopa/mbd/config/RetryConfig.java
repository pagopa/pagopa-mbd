package it.gov.pagopa.mbd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "mbd.retry")
public class RetryConfig {
	private int maxAttempts;
    private long delay;
    private double multiplier;
}
