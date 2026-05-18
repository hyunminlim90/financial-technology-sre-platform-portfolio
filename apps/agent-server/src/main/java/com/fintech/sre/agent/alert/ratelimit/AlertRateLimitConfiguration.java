package com.fintech.sre.agent.alert.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AlertRateLimitProperties.class)
public class AlertRateLimitConfiguration {
}
