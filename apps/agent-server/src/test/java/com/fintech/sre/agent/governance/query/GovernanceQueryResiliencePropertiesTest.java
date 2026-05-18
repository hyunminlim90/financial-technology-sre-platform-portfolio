package com.fintech.sre.agent.governance.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GovernanceQueryResiliencePropertiesTest {

	private final ApplicationContextRunner contextRunner =
			new ApplicationContextRunner()
					.withUserConfiguration(GovernanceQueryResilienceConfiguration.class);

	@Test
	void shouldUseSafeDefaultValues() {
		contextRunner.run(context -> {
			GovernanceQueryResilienceProperties properties =
					context.getBean(GovernanceQueryResilienceProperties.class);

			assertThat(properties.isEnabled()).isFalse();
			assertThat(properties.getOptimizedQueryTimeoutMs()).isEqualTo(1500);
			assertThat(properties.isFallbackEnabled()).isTrue();
			assertThat(properties.isFailOpenDashboard()).isTrue();
		});
	}

	@Test
	void shouldBindConfiguredResilienceValues() {
		contextRunner.withPropertyValues(
				"agent.governance.query.resilience.enabled=true",
				"agent.governance.query.resilience.optimized-query-timeout-ms=2500",
				"agent.governance.query.resilience.fallback-enabled=false",
				"agent.governance.query.resilience.fail-open-dashboard=false"
		).run(context -> {
			GovernanceQueryResilienceProperties properties =
					context.getBean(GovernanceQueryResilienceProperties.class);

			assertThat(properties.isEnabled()).isTrue();
			assertThat(properties.getOptimizedQueryTimeoutMs()).isEqualTo(2500);
			assertThat(properties.isFallbackEnabled()).isFalse();
			assertThat(properties.isFailOpenDashboard()).isFalse();
		});
	}
}
