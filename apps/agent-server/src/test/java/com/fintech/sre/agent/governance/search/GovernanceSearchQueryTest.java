package com.fintech.sre.agent.governance.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovernanceSearchQueryTest {

	@Test
	void shouldClampLimitToSafeRange() {
		assertThat(new GovernanceSearchQuery("a", GovernanceSearchType.ALL, "24h", null).safeLimit())
				.isEqualTo(20);
		assertThat(new GovernanceSearchQuery("a", GovernanceSearchType.ALL, "24h", -1).safeLimit())
				.isEqualTo(20);
		assertThat(new GovernanceSearchQuery("a", GovernanceSearchType.ALL, "24h", 10).safeLimit())
				.isEqualTo(10);
		assertThat(new GovernanceSearchQuery("a", GovernanceSearchType.ALL, "24h", 500).safeLimit())
				.isEqualTo(100);
	}

	@Test
	void shouldNormalizeBlankQuery() {
		assertThat(new GovernanceSearchQuery(null, GovernanceSearchType.ALL, "24h", 20).normalizedQuery())
				.isEmpty();
		assertThat(new GovernanceSearchQuery("  incident-1  ", GovernanceSearchType.ALL, "24h", 20).normalizedQuery())
				.isEqualTo("incident-1");
	}
}
