package com.fintech.sre.agent.governance.search;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GovernanceSearchResilienceProperties.class)
public class GovernanceSearchResilienceConfiguration {
}
