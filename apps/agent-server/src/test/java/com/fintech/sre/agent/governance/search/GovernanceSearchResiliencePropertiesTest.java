package com.fintech.sre.agent.governance.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GovernanceSearchResiliencePropertiesTest {

	private final ApplicationContextRunner contextRunner =
			new ApplicationContextRunner()
					.withUserConfiguration(GovernanceSearchResilienceConfiguration.class);

	@Test
	void shouldUseSafeDefaultValues() {
		contextRunner.run(context -> {
			GovernanceSearchResilienceProperties properties =
					context.getBean(GovernanceSearchResilienceProperties.class);

			assertThat(properties.isEnabled()).isFalse();
			assertThat(properties.isPartialSearchEnabled()).isTrue();
			assertThat(properties.isFailOpenSearch()).isTrue();
			assertThat(properties.getComponentQueryTimeoutMs()).isEqualTo(1500);
		});
	}

	@Test
	void shouldBindConfiguredResilienceValues() {
		contextRunner.withPropertyValues(
				"agent.governance.search.resilience.enabled=true",
				"agent.governance.search.resilience.partial-search-enabled=false",
				"agent.governance.search.resilience.fail-open-search=false",
				"agent.governance.search.resilience.component-query-timeout-ms=2500"
		).run(context -> {
			GovernanceSearchResilienceProperties properties =
					context.getBean(GovernanceSearchResilienceProperties.class);

			assertThat(properties.isEnabled()).isTrue();
			assertThat(properties.isPartialSearchEnabled()).isFalse();
			assertThat(properties.isFailOpenSearch()).isFalse();
			assertThat(properties.getComponentQueryTimeoutMs()).isEqualTo(2500);
		});
	}
}
