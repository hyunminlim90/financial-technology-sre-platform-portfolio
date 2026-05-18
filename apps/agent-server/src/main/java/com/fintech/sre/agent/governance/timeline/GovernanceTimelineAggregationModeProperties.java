package com.fintech.sre.agent.governance.timeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.governance.timeline.aggregation")
public class GovernanceTimelineAggregationModeProperties {

	private GovernanceTimelineAggregationMode mode =
			GovernanceTimelineAggregationMode.RUNTIME_FAN_OUT;

	public GovernanceTimelineAggregationMode getMode() {
		return mode;
	}

	public void setMode(GovernanceTimelineAggregationMode mode) {
		this.mode = mode;
	}
}
