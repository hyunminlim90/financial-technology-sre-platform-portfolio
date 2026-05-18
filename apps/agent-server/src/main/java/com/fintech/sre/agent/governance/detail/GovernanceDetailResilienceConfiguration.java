package com.fintech.sre.agent.governance.detail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GovernanceDetailResilienceProperties.class)
public class GovernanceDetailResilienceConfiguration {
}
