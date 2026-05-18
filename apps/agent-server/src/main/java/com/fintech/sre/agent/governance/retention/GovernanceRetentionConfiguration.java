package com.fintech.sre.agent.governance.retention;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GovernanceRetentionProperties.class)
public class GovernanceRetentionConfiguration {
}
