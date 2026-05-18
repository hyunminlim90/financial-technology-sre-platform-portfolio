package com.fintech.sre.agent.governance.timeline;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GovernanceTimelineAggregationModeProperties.class)
public class GovernanceTimelineAggregationModeConfiguration {
}
