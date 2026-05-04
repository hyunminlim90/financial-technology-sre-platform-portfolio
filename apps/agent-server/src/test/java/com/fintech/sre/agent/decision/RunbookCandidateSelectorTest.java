package com.fintech.sre.agent.decision;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.evidence.Evidence;
import com.fintech.sre.agent.evidence.EvidenceConfidence;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceLayer;
import com.fintech.sre.agent.evidence.EvidenceQueryStatus;
import com.fintech.sre.agent.evidence.EvidenceSeverity;
import com.fintech.sre.agent.evidence.EvidenceSignal;
import com.fintech.sre.agent.evidence.EvidenceSource;
import com.fintech.sre.agent.evidence.EvidenceStatus;
import com.fintech.sre.agent.knowledge.rag.KnowledgeContext;
import com.fintech.sre.agent.knowledge.rag.KnowledgeDocument;
import com.fintech.sre.agent.knowledge.rag.KnowledgeLayer;
import com.fintech.sre.agent.model.common.IncidentContext;
import com.fintech.sre.agent.runbook.RunbookCandidateActionFactory;
import com.fintech.sre.agent.runbook.RunbookConditionMatcher;
import com.fintech.sre.agent.runbook.RunbookLoader;
import com.fintech.sre.agent.runbook.RunbookActionMapper;

class RunbookCandidateSelectorTest {

	@Test
	void shouldCreateEvidenceBasedActionsFromLatencyAndErrorSignals() {
		RunbookCandidateSelector selector = new RunbookCandidateSelector(new RunbookCandidateActionFactory(
				runbookLoader(),
				new RunbookConditionMatcher(),
				new RunbookActionMapper()
		));

		DecisionInput input = new DecisionInput(
				IncidentContext.builder()
						.incidentId("INC-EVIDENCE-1")
						.alertName("CheckoutHighLatency")
						.service("payment-service")
						.environment("prod")
						.build(),
				null,
				knowledgeContext(),
				List.of()
		);

		DecisionCandidate candidate = selector.select(
						input,
						new MatchedScenario("LATENCY_AND_TIMEOUT", "PAYMENTS", "Payment latency", "scenario", null, null),
						contextWith(EvidenceSignal.P99_LATENCY_HIGH, EvidenceSignal.ERROR_RATE_HIGH)
				)
				.block();

		assertThat(candidate).isNotNull();
		assertThat(candidate.recommendedActions()).extracting(action -> action.command().type())
				.contains(ActionType.SCALE_OUT, ActionType.RATE_LIMIT);
	}

	private RunbookLoader runbookLoader() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return new RunbookLoader(objectMapper);
	}

	private EvidenceContext contextWith(EvidenceSignal... signals) {
		List<Evidence> evidences = java.util.Arrays.stream(signals)
				.map(signal -> new Evidence(
						EvidenceLayer.APPLICATION,
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
				"INC-EVIDENCE-1",
				"payment-service",
				"prod",
				evidences,
				java.util.Map.of("domain", "payment"),
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS,
				EvidenceQueryStatus.SUCCESS
		);
	}

	private KnowledgeContext knowledgeContext() {
		return new KnowledgeContext(
				List.of(new KnowledgeDocument(
						"scenario-payment-high-latency",
						KnowledgeLayer.SCENARIO,
						"scenarios/payment-api/high-latency.md",
						"Payment API High Latency",
						"scenario snippet",
						java.util.Map.of("domain", "payment")
				)),
				List.of(new KnowledgeDocument(
						"runbook-payment-high-latency",
						KnowledgeLayer.RUNBOOK,
						"runbooks/payment-api/high-latency.md",
						"Payment API High Latency Runbook",
						"runbook snippet",
						java.util.Map.of("domain", "payment")
				)),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
	}
}
