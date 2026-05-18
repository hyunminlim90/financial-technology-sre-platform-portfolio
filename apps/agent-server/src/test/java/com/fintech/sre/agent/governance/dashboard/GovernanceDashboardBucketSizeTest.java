package com.fintech.sre.agent.governance.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GovernanceDashboardBucketSizeTest {

	@Test
	void shouldParseFifteenMinutes() {
		assertThat(GovernanceDashboardBucketSize.from("15m"))
				.isEqualTo(GovernanceDashboardBucketSize.FIFTEEN_MINUTES);
	}

	@Test
	void shouldParseDefaultOneHour() {
		assertThat(GovernanceDashboardBucketSize.from(null))
				.isEqualTo(GovernanceDashboardBucketSize.ONE_HOUR);
	}

	@Test
	void shouldParseOneDay() {
		assertThat(GovernanceDashboardBucketSize.from("1d"))
				.isEqualTo(GovernanceDashboardBucketSize.ONE_DAY);
	}

	@Test
	void shouldRejectInvalidBucket() {
		assertThatThrownBy(() -> GovernanceDashboardBucketSize.from("5m"))
				.isInstanceOf(GovernanceDashboardRejectedException.class);
	}
}
