package com.fintech.sre.agent.governance.detail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GovernanceDetailResiliencePropertiesTest {

	private final ApplicationContextRunner contextRunner =
			new ApplicationContextRunner()
					.withUserConfiguration(GovernanceDetailResilienceConfiguration.class);

	@Test
	void shouldUseSafeDefaultValues() {
		contextRunner.run(context -> {
			GovernanceDetailResilienceProperties properties =
					context.getBean(GovernanceDetailResilienceProperties.class);

			assertThat(properties.isEnabled()).isFalse();
			assertThat(properties.isFailOpenDetail()).isTrue();
			assertThat(properties.isPartialResponseEnabled()).isTrue();
			assertThat(properties.getComponentQueryTimeoutMs()).isEqualTo(1500);
		});
	}

	@Test
	void shouldBindConfiguredResilienceValues() {
		contextRunner.withPropertyValues(
				"agent.governance.detail.resilience.enabled=true",
				"agent.governance.detail.resilience.fail-open-detail=false",
				"agent.governance.detail.resilience.partial-response-enabled=false",
				"agent.governance.detail.resilience.component-query-timeout-ms=2500"
		).run(context -> {
			GovernanceDetailResilienceProperties properties =
					context.getBean(GovernanceDetailResilienceProperties.class);

			assertThat(properties.isEnabled()).isTrue();
			assertThat(properties.isFailOpenDetail()).isFalse();
			assertThat(properties.isPartialResponseEnabled()).isFalse();
			assertThat(properties.getComponentQueryTimeoutMs()).isEqualTo(2500);
		});
	}
}
