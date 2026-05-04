package com.fintech.sre.agent.runbook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.evidence.Evidence;
import com.fintech.sre.agent.evidence.EvidenceConfidence;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceLayer;
import com.fintech.sre.agent.evidence.EvidenceQueryStatus;
import com.fintech.sre.agent.evidence.EvidenceSeverity;
import com.fintech.sre.agent.evidence.EvidenceSignal;
import com.fintech.sre.agent.evidence.EvidenceSource;
import com.fintech.sre.agent.evidence.EvidenceStatus;

class RunbookConditionMatcherTest {

	private final RunbookConditionMatcher matcher = new RunbookConditionMatcher();

	@Test
	void shouldMatchAllConditions() {
		EvidenceContext context = contextWith(EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH);

		RunbookCondition condition = new RunbookCondition(
				List.of("KAFKA_CONSUMER_LAG_HIGH"),
				List.of(),
				List.of()
		);

		assertThat(matcher.matches(condition, context)).isTrue();
	}

	@Test
	void shouldNotMatchWhenNoneConditionExists() {
		EvidenceContext context = contextWith(
				EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH,
				EvidenceSignal.DB_POOL_PENDING_HIGH
		);

		RunbookCondition condition = new RunbookCondition(
				List.of("KAFKA_CONSUMER_LAG_HIGH"),
				List.of(),
				List.of("DB_POOL_PENDING_HIGH")
		);

		assertThat(matcher.matches(condition, context)).isFalse();
	}

	private EvidenceContext contextWith(EvidenceSignal... signals) {
		List<Evidence> evidences = Arrays.stream(signals)
				.map(signal -> new Evidence(
						EvidenceLayer.QUEUE,
						signal,
						1,
						1,
						Duration.ofMinutes(5),
						EvidenceSource.PROMETHEUS,
						EvidenceSeverity.WARNING,
						EvidenceConfidence.HIGH,
						EvidenceStatus.PRESENT,
						signal.name()
				))
				.toList();

		return new EvidenceContext(
				evidences,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		);
	}
}
