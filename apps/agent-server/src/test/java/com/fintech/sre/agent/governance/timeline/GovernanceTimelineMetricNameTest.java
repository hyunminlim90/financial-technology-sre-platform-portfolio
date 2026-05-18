package com.fintech.sre.agent.governance.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceTimelineMetricNameTest {

	@Test
	void shouldExposeMetricNameConstants() {
		assertThat(GovernanceTimelineMetricName.QUERY_TOTAL)
				.isEqualTo("fin_sre_governance_timeline_query_total");
		assertThat(GovernanceTimelineMetricName.AGGREGATION_TOTAL)
				.isEqualTo("fin_sre_governance_timeline_aggregation_total");
		assertThat(GovernanceTimelineMetricName.DEGRADED_TOTAL)
				.isEqualTo("fin_sre_governance_timeline_degraded_total");
		assertThat(GovernanceTimelineMetricName.PAGE_SIZE)
				.isEqualTo("fin_sre_governance_timeline_page_size");
		assertThat(GovernanceTimelineMetricName.HEALTH_STATUS)
				.isEqualTo("fin_sre_governance_timeline_health_status");
		assertThat(GovernanceTimelineMetricTag.RESULT).isEqualTo("result");
		assertThat(GovernanceTimelineMetricTag.MODE).isEqualTo("mode");
		assertThat(GovernanceTimelineMetricTag.REASON).isEqualTo("reason");
		assertThat(GovernanceTimelineMetricTag.SOURCE).isEqualTo("source");
	}
}
