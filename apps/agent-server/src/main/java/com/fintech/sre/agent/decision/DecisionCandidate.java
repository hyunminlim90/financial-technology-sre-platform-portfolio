package com.fintech.sre.agent.decision;

import java.util.List;

import com.fintech.sre.agent.model.common.AlternativeAction;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.ForbiddenAction;
import com.fintech.sre.agent.model.common.ImpactScope;
import com.fintech.sre.agent.model.common.Severity;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.decision.generator.CandidateGenerationSource;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;
import com.fintech.sre.agent.policy.PolicyEvaluationResult;

import lombok.Builder;

@Builder(toBuilder = true)
public record DecisionCandidate(
		MatchedScenario scenario,
		List<CandidateAction> candidateActions,
		List<CandidateAction> recommendedActions,
		List<AlternativeAction> alternativeActions,
		List<ForbiddenAction> forbiddenActions,
		List<String> mostLikelyCauses,
		List<String> reasoningNotes,
		ConfidenceLevel confidenceLevel,
		PolicyEvaluationResult policyEvaluationResult,
		EvidenceContext evidenceContext,
		String guardrailDecision,
		String blockedReason,
		CandidateGenerationSource candidateGenerationSource
) {
	public DecisionCandidate {
		candidateActions = candidateActions == null ? List.of() : List.copyOf(candidateActions);
		recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
		alternativeActions = alternativeActions == null ? List.of() : List.copyOf(alternativeActions);
		forbiddenActions = forbiddenActions == null ? List.of() : List.copyOf(forbiddenActions);
		mostLikelyCauses = mostLikelyCauses == null ? List.of() : List.copyOf(mostLikelyCauses);
		reasoningNotes = reasoningNotes == null ? List.of() : List.copyOf(reasoningNotes);
		candidateGenerationSource = candidateGenerationSource == null
				? CandidateGenerationSource.LOCAL_BOOTSTRAP_RUNBOOK
				: candidateGenerationSource;
	}

	public DecisionCandidate withPolicyEvaluationResult(PolicyEvaluationResult result) {
		return this.toBuilder()
				.policyEvaluationResult(result)
				.build();
	}

	public DecisionCandidate rejectByPolicy(PolicyEvaluationResult result) {
		return this.toBuilder()
				.recommendedActions(List.of())
				.policyEvaluationResult(result)
				.build();
	}

	public DecisionCandidate withEvidenceContext(EvidenceContext evidenceContext) {
		return this.toBuilder()
				.evidenceContext(evidenceContext)
				.build();
	}

	public DecisionCandidate withForbiddenActions(List<ForbiddenAction> forbiddenActions) {
		return this.toBuilder()
				.forbiddenActions(forbiddenActions)
				.build();
	}

	public DecisionCandidate withCandidateGenerationSource(CandidateGenerationSource source) {
		return this.toBuilder()
				.candidateGenerationSource(source)
				.build();
	}

	public DecisionCandidate rejectByGuardrail(String code, String reason) {
		return this.toBuilder()
				.recommendedActions(List.of())
				.guardrailDecision(code == null ? "DENY" : code)
				.blockedReason(reason)
				.build();
	}

	public ActionCommand actionCommand() {
		if (recommendedActions != null && !recommendedActions.isEmpty() && recommendedActions.get(0).command() != null) {
			return recommendedActions.get(0).command();
		}
		if (candidateActions != null && !candidateActions.isEmpty()) {
			return candidateActions.get(0).command();
		}
		return null;
	}

	public double confidence() {
		if (confidenceLevel == null) {
			return 0.0d;
		}
		return switch (confidenceLevel) {
			case HIGH -> 1.0d;
			case MEDIUM -> 0.5d;
			case LOW -> 0.1d;
		};
	}

	public static DecisionCandidate noAction(
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext,
			String reasonCode,
			String reason
	) {
		return DecisionCandidate.builder()
				.scenario(new MatchedScenario(
						request == null || request.alertName() == null ? "NO_ACTION" : request.alertName(),
						request != null && request.service() != null && request.service().toLowerCase().contains("payment")
								? "payment"
								: "platform",
						reasonCode,
						"no-action",
						parseSeverity(request == null ? null : request.severityHint()),
						ImpactScope.PARTIAL
				))
				.candidateActions(List.of())
				.recommendedActions(List.of())
				.alternativeActions(List.of())
				.forbiddenActions(List.of())
				.mostLikelyCauses(List.of())
				.reasoningNotes(List.of(reason))
				.confidenceLevel(ConfidenceLevel.LOW)
				.policyEvaluationResult(null)
				.evidenceContext(evidenceContext)
				.guardrailDecision(null)
				.blockedReason(reason)
				.candidateGenerationSource(CandidateGenerationSource.FALLBACK_NO_ACTION)
				.build();
	}

	private static Severity parseSeverity(String severityHint) {
		if (severityHint == null || severityHint.isBlank()) {
			return Severity.SEV_2;
		}
		return switch (severityHint.trim().toUpperCase()) {
			case "SEV_1", "SEV1" -> Severity.SEV_1;
			case "SEV_3", "SEV3" -> Severity.SEV_3;
			default -> Severity.SEV_2;
		};
	}
}
