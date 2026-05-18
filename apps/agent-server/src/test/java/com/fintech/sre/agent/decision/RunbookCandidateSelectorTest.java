package com.fintech.sre.agent.decision;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.VerificationCommand;
import com.fintech.sre.agent.decision.generator.CandidateGenerationSource;
import com.fintech.sre.agent.decision.generator.CandidateGenerator;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceSignal;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

import reactor.core.publisher.Mono;

class RunbookCandidateSelectorTest {

	@Test
	void shouldPreferNonFallbackCandidatesAndKeepSource() {
		CandidateGenerator localGenerator = new CandidateGenerator() {
			@Override
			public CandidateGenerationSource source() {
				return CandidateGenerationSource.LOCAL_BOOTSTRAP_RUNBOOK;
			}

			@Override
			public Mono<List<DecisionCandidate>> generate(IncidentRecommendationRequest request, EvidenceContext evidenceContext) {
				return Mono.just(List.of(candidate("LOCAL-RATE-LIMIT", ActionType.RATE_LIMIT, 0.5d, source())));
			}
		};

		CandidateGenerator fallbackGenerator = new CandidateGenerator() {
			@Override
			public CandidateGenerationSource source() {
				return CandidateGenerationSource.FALLBACK_NO_ACTION;
			}

			@Override
			public Mono<List<DecisionCandidate>> generate(IncidentRecommendationRequest request, EvidenceContext evidenceContext) {
				return Mono.just(List.of(
						DecisionCandidate.noAction(
								request,
								evidenceContext,
								"NO_MATCH",
								"no candidate"
						).withCandidateGenerationSource(source())
				));
			}
		};

		RunbookCandidateSelector selector = new RunbookCandidateSelector(List.of(localGenerator, fallbackGenerator));

		List<DecisionCandidate> candidates = selector.select(request(), evidence()).block();

		assertThat(candidates).hasSize(1);
		assertThat(candidates.get(0).candidateGenerationSource())
				.isEqualTo(CandidateGenerationSource.LOCAL_BOOTSTRAP_RUNBOOK);
		assertThat(candidates.get(0).actionCommand().type()).isEqualTo(ActionType.RATE_LIMIT);
	}

	@Test
	void shouldReturnFallbackWhenNoNonFallbackCandidateExists() {
		CandidateGenerator fallbackGenerator = new CandidateGenerator() {
			@Override
			public CandidateGenerationSource source() {
				return CandidateGenerationSource.FALLBACK_NO_ACTION;
			}

			@Override
			public Mono<List<DecisionCandidate>> generate(IncidentRecommendationRequest request, EvidenceContext evidenceContext) {
				return Mono.just(List.of(
						DecisionCandidate.noAction(
								request,
								evidenceContext,
								"NO_MATCH",
								"no candidate"
						).withCandidateGenerationSource(source())
				));
			}
		};

		RunbookCandidateSelector selector = new RunbookCandidateSelector(List.of(fallbackGenerator));

		List<DecisionCandidate> candidates = selector.select(request(), evidence()).block();

		assertThat(candidates).hasSize(1);
		assertThat(candidates.get(0).candidateGenerationSource())
				.isEqualTo(CandidateGenerationSource.FALLBACK_NO_ACTION);
		assertThat(candidates.get(0).recommendedActions()).isEmpty();
	}

	private DecisionCandidate candidate(
			String title,
			ActionType actionType,
			double confidence,
			CandidateGenerationSource source
	) {
		ConfidenceLevel confidenceLevel = confidence >= 1.0d
				? ConfidenceLevel.HIGH
				: confidence >= 0.5d ? ConfidenceLevel.MEDIUM : ConfidenceLevel.LOW;

		CandidateAction action = CandidateAction.builder()
				.step(1)
				.action(title)
				.command(new ActionCommand(
						title.toLowerCase(),
						actionType,
						new ActionTarget("payment", "payment-service", "policy", "rate-limit", "prod"),
						true,
						new RollbackCommand("rollback"),
						List.of(new VerificationCommand("payment.consistency", "stable", "check consistency"))
				))
				.expectedEffect("effect")
				.risk("risk")
				.rollbackPlan("rollback")
				.verification(List.of("verify"))
				.requiresHumanApproval(true)
				.source(ActionSource.RUNBOOK)
				.riskLevel(ActionRiskLevel.MEDIUM)
				.build();

		return DecisionCandidate.builder()
				.scenario(new MatchedScenario(
						"LATENCY",
						"payment",
						title,
						"title",
						com.fintech.sre.agent.model.common.Severity.SEV_2,
						com.fintech.sre.agent.model.common.ImpactScope.PARTIAL
				))
				.candidateActions(List.of(action))
				.recommendedActions(List.of(action))
				.alternativeActions(List.of())
				.forbiddenActions(List.of())
				.mostLikelyCauses(List.of("cause"))
				.reasoningNotes(List.of("reason"))
				.confidenceLevel(confidenceLevel)
				.evidenceContext(evidence())
				.candidateGenerationSource(source)
				.build();
	}

	private IncidentRecommendationRequest request() {
		return new IncidentRecommendationRequest(
				"INC-EVIDENCE-1",
				"CheckoutHighLatency",
				"payment-service",
				"prod",
				"SEV_2",
				Instant.parse("2026-05-02T00:00:00Z"),
				Map.of("domain", "payment"),
				null,
				List.of(),
				List.of(),
				null,
				"latency spike"
		);
	}

	private EvidenceContext evidence() {
		return new EvidenceContext(
				"INC-EVIDENCE-1",
				List.of(EvidenceSignal.P99_LATENCY_HIGH, EvidenceSignal.ERROR_RATE_HIGH),
				List.of("scenario/payment-latency-spike"),
				List.of("runbook/payment-latency-mitigation"),
				List.of(),
				List.of(),
				List.of(),
				List.of()
		);
	}
}
