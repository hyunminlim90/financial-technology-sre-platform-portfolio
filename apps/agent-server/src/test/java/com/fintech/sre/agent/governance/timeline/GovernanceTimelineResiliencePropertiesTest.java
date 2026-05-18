package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GovernanceTimelineResiliencePropertiesTest {

	private final ApplicationContextRunner contextRunner =
			new ApplicationContextRunner()
					.withUserConfiguration(
							GovernanceTimelineResilienceConfiguration.class
					);

	@Test
	void shouldUseSafeDefaultValues() {
		contextRunner.run(context -> {
			GovernanceTimelineResilienceProperties properties =
					context.getBean(GovernanceTimelineResilienceProperties.class);

			assertThat(properties.isEnabled()).isFalse();
			assertThat(properties.isPartialTimelineEnabled()).isTrue();
			assertThat(properties.isFailOpenReadOnly()).isTrue();
			assertThat(properties.getComponentQueryTimeoutMs()).isEqualTo(1500);
		});
	}

	@Test
	void shouldBindConfiguredResilienceValues() {
		contextRunner.withPropertyValues(
				"agent.governance.timeline.resilience.enabled=true",
				"agent.governance.timeline.resilience.partial-timeline-enabled=false",
				"agent.governance.timeline.resilience.fail-open-read-only=false",
				"agent.governance.timeline.resilience.component-query-timeout-ms=2500"
		).run(context -> {
			GovernanceTimelineResilienceProperties properties =
					context.getBean(GovernanceTimelineResilienceProperties.class);

			assertThat(properties.isEnabled()).isTrue();
			assertThat(properties.isPartialTimelineEnabled()).isFalse();
			assertThat(properties.isFailOpenReadOnly()).isFalse();
			assertThat(properties.getComponentQueryTimeoutMs()).isEqualTo(2500);
		});
	}
}
