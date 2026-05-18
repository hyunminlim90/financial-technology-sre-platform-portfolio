package com.fintech.sre.agent.governance.retention;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GovernanceRetentionPropertiesTest {

	private final ApplicationContextRunner contextRunner =
			new ApplicationContextRunner()
					.withUserConfiguration(GovernanceRetentionConfiguration.class);

	@Test
	void shouldUseSafeDefaultValues() {
		contextRunner.run(context -> {
			GovernanceRetentionProperties properties =
					context.getBean(GovernanceRetentionProperties.class);

			assertThat(properties.isEnabled()).isFalse();
			assertThat(properties.isDeleteEnabled()).isFalse();
			assertThat(properties.getHotRetentionDays()).isEqualTo(90);
			assertThat(properties.getArchiveRetentionDays()).isEqualTo(365);
		});
	}

	@Test
	void shouldBindConfiguredRetentionValues() {
		contextRunner
				.withPropertyValues(
						"agent.governance.retention.enabled=true",
						"agent.governance.retention.delete-enabled=false",
						"agent.governance.retention.hot-retention-days=180",
						"agent.governance.retention.archive-retention-days=730"
				)
				.run(context -> {
					GovernanceRetentionProperties properties =
							context.getBean(GovernanceRetentionProperties.class);

					assertThat(properties.isEnabled()).isTrue();
					assertThat(properties.isDeleteEnabled()).isFalse();
					assertThat(properties.getHotRetentionDays()).isEqualTo(180);
					assertThat(properties.getArchiveRetentionDays()).isEqualTo(730);
				});
	}
}
