package com.fintech.sre.agent.governance.search;

public final class GovernanceSearchMetricName {

	private GovernanceSearchMetricName() {
	}

	public static final String QUERY_TOTAL =
			"fin_sre_governance_search_query_total";

	public static final String RESULT_COUNT =
			"fin_sre_governance_search_result_count";

	public static final String DEGRADED_TOTAL =
			"fin_sre_governance_search_degraded_total";

	public static final String HEALTH_STATUS =
			"fin_sre_governance_search_health_status";
}
