package com.fintech.sre.agent.alert.dedup;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AlertDeduplicationProperties.class)
public class AlertDeduplicationConfiguration {
}
