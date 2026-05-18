package com.fintech.sre.agent.governance.detail;

public final class GovernanceDetailMetricName {

	private GovernanceDetailMetricName() {
	}

	public static final String QUERY_TOTAL =
			"fin_sre_governance_detail_query_total";

	public static final String QUERY_NOT_FOUND =
			"fin_sre_governance_detail_query_not_found_total";

	public static final String DEGRADED_TOTAL =
			"fin_sre_governance_detail_degraded_total";

	public static final String HEALTH_STATUS =
			"fin_sre_governance_detail_health_status";
}
