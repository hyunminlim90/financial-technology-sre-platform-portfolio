package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GovernanceTimelineAggregationModePropertiesTest {

	private final ApplicationContextRunner contextRunner =
			new ApplicationContextRunner()
					.withUserConfiguration(
							GovernanceTimelineAggregationModeConfiguration.class
					);

	@Test
	void shouldUseRuntimeFanOutAsDefaultMode() {
		contextRunner.run(context -> {
			GovernanceTimelineAggregationModeProperties properties =
					context.getBean(GovernanceTimelineAggregationModeProperties.class);

			assertThat(properties.getMode())
					.isEqualTo(GovernanceTimelineAggregationMode.RUNTIME_FAN_OUT);
		});
	}

	@Test
	void shouldBindProjectionBackedMode() {
		contextRunner.withPropertyValues(
				"agent.governance.timeline.aggregation.mode=PROJECTION_BACKED"
		).run(context -> {
			GovernanceTimelineAggregationModeProperties properties =
					context.getBean(GovernanceTimelineAggregationModeProperties.class);

			assertThat(properties.getMode())
					.isEqualTo(GovernanceTimelineAggregationMode.PROJECTION_BACKED);
		});
	}
}
