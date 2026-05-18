package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GovernanceTimelineDegradationTest {

	@Test
	void shouldCreateNoneWithStrictDefaults() {
		GovernanceTimelineDegradation degradation =
				GovernanceTimelineDegradation.none();

		assertThat(degradation.degraded()).isFalse();
		assertThat(degradation.partialTimeline()).isFalse();
		assertThat(degradation.mode()).isEqualTo(
				GovernanceTimelineResilienceMode.STRICT
		);
		assertThat(degradation.failedComponents()).isEmpty();
		assertThat(degradation.reason()).isEqualTo("none");
	}

	@Test
	void shouldCreatePartialWithDefaultReasonAndMode() {
		List<GovernanceTimelineComponentFailure> failures = new ArrayList<>();
		failures.add(new GovernanceTimelineComponentFailure(
				GovernanceTimelineAggregationSource.VERIFICATION,
				"component_query_timeout"
		));

		GovernanceTimelineDegradation degradation =
				GovernanceTimelineDegradation.partial(
						null,
						failures,
						null
				);

		failures.add(new GovernanceTimelineComponentFailure(
				GovernanceTimelineAggregationSource.APPROVAL,
				"projection_failed"
		));

		assertThat(degradation.degraded()).isTrue();
		assertThat(degradation.partialTimeline()).isTrue();
		assertThat(degradation.mode()).isEqualTo(
				GovernanceTimelineResilienceMode.PARTIAL_DEGRADED
		);
		assertThat(degradation.failedComponents()).containsExactly(
				new GovernanceTimelineComponentFailure(
						GovernanceTimelineAggregationSource.VERIFICATION,
						"component_query_timeout"
				)
		);
		assertThat(degradation.reason()).isEqualTo("aggregation_degraded");
	}
}
