package com.fintech.sre.agent.governance.timeline;

public final class GovernanceTimelineMetricName {

	private GovernanceTimelineMetricName() {
	}

	public static final String QUERY_TOTAL =
			"fin_sre_governance_timeline_query_total";

	public static final String AGGREGATION_TOTAL =
			"fin_sre_governance_timeline_aggregation_total";

	public static final String DEGRADED_TOTAL =
			"fin_sre_governance_timeline_degraded_total";

	public static final String PAGE_SIZE =
			"fin_sre_governance_timeline_page_size";

	public static final String HEALTH_STATUS =
			"fin_sre_governance_timeline_health_status";

	public static final String RUNTIME_MODE =
			"fin_sre_governance_timeline_runtime_mode";
}
