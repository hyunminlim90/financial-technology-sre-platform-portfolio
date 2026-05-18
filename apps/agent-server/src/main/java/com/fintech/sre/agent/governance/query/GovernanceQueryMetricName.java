package com.fintech.sre.agent.governance.query;

public final class GovernanceQueryMetricName {

	private GovernanceQueryMetricName() {
	}

	public static final String OPTIMIZED =
			"fin_sre_governance_query_optimized_total";

	public static final String FALLBACK =
			"fin_sre_governance_query_fallback_total";

	public static final String FAILURE =
			"fin_sre_governance_query_failure_total";
}
