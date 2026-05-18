package com.fintech.sre.agent.governance.query;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GovernanceQueryResilienceProperties.class)
public class GovernanceQueryResilienceConfiguration {
}
