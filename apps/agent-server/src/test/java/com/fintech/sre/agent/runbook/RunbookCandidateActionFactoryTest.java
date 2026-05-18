package com.fintech.sre.agent.runbook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.decision.CandidateAction;
import com.fintech.sre.agent.evidence.Evidence;
import com.fintech.sre.agent.evidence.EvidenceConfidence;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceLayer;
import com.fintech.sre.agent.evidence.EvidenceQueryStatus;
import com.fintech.sre.agent.evidence.EvidenceSeverity;
import com.fintech.sre.agent.evidence.EvidenceSignal;
import com.fintech.sre.agent.evidence.EvidenceSource;
import com.fintech.sre.agent.evidence.EvidenceStatus;

class RunbookCandidateActionFactoryTest {

	private final RunbookCandidateActionFactory factory = new RunbookCandidateActionFactory(
			runbookLoader(),
			new RunbookConditionMatcher(),
			new RunbookActionMapper()
	);

	@Test
	void dbPoolPendingShouldBlockSafeScaleOutBranch() {
		List<CandidateAction> candidates = factory.createCandidates(contextWith(
				EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH,
				EvidenceSignal.DB_POOL_PENDING_HIGH
		), "prod");

		assertThat(candidates).extracting(candidate -> candidate.command().type())
				.contains(ActionType.RATE_LIMIT)
				.doesNotContain(ActionType.SCALE_OUT);
	}

	@Test
	void rebalanceStormShouldProducePauseRolloutCandidate() {
		List<CandidateAction> candidates = factory.createCandidates(contextWith(
				EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH,
				EvidenceSignal.KAFKA_REBALANCE_STORM
		), "prod");

		assertThat(candidates).extracting(candidate -> candidate.command().type())
				.contains(ActionType.PAUSE_ROLLOUT)
				.doesNotContain(ActionType.SCALE_OUT);
	}

	@Test
	void kafkaLagWithoutBlockingEvidenceShouldProduceScaleOutCandidate() {
		List<CandidateAction> candidates = factory.createCandidates(contextWith(
				EvidenceSignal.KAFKA_CONSUMER_LAG_HIGH
		), "prod");

		assertThat(candidates).extracting(candidate -> candidate.command().type())
				.contains(ActionType.SCALE_OUT);
	}

	private RunbookLoader runbookLoader() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return new RunbookLoader(objectMapper, new RunbookConditionMatcher());
	}

	private EvidenceContext contextWith(EvidenceSignal... signals) {
		List<Evidence> evidences = java.util.Arrays.stream(signals)
				.map(signal -> new Evidence(
						signal == EvidenceSignal.DB_POOL_PENDING_HIGH ? EvidenceLayer.DATABASE : EvidenceLayer.QUEUE,
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
