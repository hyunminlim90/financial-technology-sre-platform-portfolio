package com.fintech.sre.agent.runbook;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ApprovalPolicy;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.BlastRadius;
import com.fintech.sre.agent.action.DuplicateExecutionRisk;
import com.fintech.sre.agent.action.PaymentSafety;
import com.fintech.sre.agent.action.RiskLevel;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.RollbackPolicy;
import com.fintech.sre.agent.action.VerificationCommand;
import com.fintech.sre.agent.action.VerificationPolicy;
import com.fintech.sre.agent.decision.ActionRiskLevel;
import com.fintech.sre.agent.decision.ActionSource;
import com.fintech.sre.agent.decision.CandidateAction;
import com.fintech.sre.agent.decision.DecisionCandidate;
import com.fintech.sre.agent.decision.MatchedScenario;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.model.common.AlternativeAction;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.ImpactScope;
import com.fintech.sre.agent.model.common.Severity;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

@Component
public class RunbookActionMapper {

	public ActionCommand toCommand(RunbookAction action) {
		return toCommand(action, "unknown");
	}

	public ActionCommand toCommand(RunbookAction action, String environment) {
		ActionTarget target = new ActionTarget(
				extractDomain(action.targetService()),
				action.targetService(),
				resourceType(action.targetLayer()),
				action.targetService(),
				environment
		);
		return new ActionCommand(
				buildId(action),
				toActionType(action.type()),
				target,
				toRiskLevel(action.riskLevel()),
				toBlastRadius(action.targetLayer()),
				action.approval() == null
						? ApprovalPolicy.humanRequired()
						: new ApprovalPolicy(action.approval().required()),
				RollbackPolicy.requiredPolicy(),
				VerificationPolicy.required(action.verification() == null ? List.of() : safeList(action.verification().checks())),
				toPaymentSafety(target),
				List.of(),
				List.of(),
				action.rollback() == null ? null : new RollbackCommand(action.rollback().plan()),
				withPaymentSafetyVerification(
						target,
						action.verification() == null ? List.of() : safeList(action.verification().checks()).stream()
						.map(check -> new VerificationCommand(
								normalizeMetric(check),
								"expected",
								check
						))
						.toList()
				),
				action.description()
		);
	}

	public DecisionCandidate toCandidate(
			RunbookDefinition runbook,
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	) {
		List<CandidateAction> actions = candidateActions(runbook, request, evidenceContext);

		return DecisionCandidate.builder()
				.scenario(new MatchedScenario(
						runbook.scenario() == null ? request.alertName() : runbook.scenario(),
						extractDomain(request.service()),
						runbook.title(),
						runbook.id(),
						parseSeverity(request.severityHint()),
						ImpactScope.PARTIAL
				))
				.candidateActions(actions)
				.recommendedActions(actions)
				.alternativeActions(List.of(
						new AlternativeAction(
								"Continue observation only.",
								"Local bootstrap runbook candidate was used as the safest available curated source."
						)
				))
				.forbiddenActions(List.of())
				.mostLikelyCauses(List.of("Matched local bootstrap runbook: " + runbook.title()))
				.reasoningNotes(List.of("Candidate generated from local bootstrap runbook knowledge."))
				.confidenceLevel(ConfidenceLevel.MEDIUM)
				.policyEvaluationResult(null)
				.evidenceContext(evidenceContext)
				.guardrailDecision(null)
				.blockedReason(null)
				.build();
	}

	private List<CandidateAction> candidateActions(
			RunbookDefinition runbook,
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	) {
		if (runbook.branches() == null) {
			return List.of();
		}

		java.util.ArrayList<CandidateAction> actions = new java.util.ArrayList<>();
		int step = 1;
		for (RunbookBranch branch : runbook.branches()) {
			if (branch.actions() == null) {
				continue;
			}

			for (RunbookAction action : branch.actions()) {
				ActionCommand command = toCommand(action, request.environment());
				actions.add(CandidateAction.builder()
						.step(step++)
						.action(action.description())
						.command(command)
						.expectedEffect(expectedEffect(command))
						.risk(riskDescription(action.riskLevel()))
						.rollbackPlan(action.rollback() == null ? null : action.rollback().plan())
						.verification(action.verification() == null ? List.of() : safeList(action.verification().checks()))
						.requiresHumanApproval(action.approval() == null || action.approval().required())
						.source(ActionSource.RUNBOOK)
						.riskLevel(toCandidateRiskLevel(action.riskLevel()))
						.build());
			}
		}
		return List.copyOf(actions);
	}

	private String buildId(RunbookAction action) {
		return (action.type() + "-" + action.targetService())
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-|-$", "");
	}

	private List<String> safeList(List<String> values) {
		return values == null ? List.of() : values;
	}

	private String expectedEffect(ActionCommand command) {
		return switch (command.type()) {
			case RATE_LIMIT -> "Reduce incoming pressure and protect downstream dependencies";
			case SCALE_OUT -> "Increase queue processing capacity under safe conditions";
			case PAUSE_ROLLOUT -> "Stabilize workload and prevent additional rebalance or restart impact";
			default -> "Mitigate incident impact while preserving payment safety";
		};
	}

	private String riskDescription(String riskLevel) {
		if (riskLevel == null) {
			return "Operational risk requires human review.";
		}
		return switch (riskLevel) {
			case "LOW" -> "Low operational risk with limited blast radius.";
			case "MEDIUM" -> "Moderate operational risk that still requires human review.";
			case "HIGH" -> "High operational risk if applied under unstable dependency conditions.";
			case "CRITICAL" -> "Critical operational risk that must be tightly controlled.";
			default -> "Operational risk requires human review.";
		};
	}

	private ActionRiskLevel toCandidateRiskLevel(String riskLevel) {
		if ("LOW".equalsIgnoreCase(riskLevel)) {
			return ActionRiskLevel.LOW;
		}
		if ("MEDIUM".equalsIgnoreCase(riskLevel)) {
			return ActionRiskLevel.MEDIUM;
		}
		return ActionRiskLevel.HIGH;
	}

	private ActionType toActionType(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("ActionType is required");
		}

		return switch (value.toUpperCase(Locale.ROOT)) {
			case "APPLY_RATE_LIMIT" -> ActionType.RATE_LIMIT;
			case "SCALE_OUT_WORKER" -> ActionType.SCALE_OUT;
			case "SCALE_IN_WORKER" -> ActionType.SCALE_DOWN;
			case "RESTART_POD" -> ActionType.RESTART_POD;
			case "FAILOVER" -> ActionType.FAILOVER;
			case "TRAFFIC_SHED" -> ActionType.TRAFFIC_SHED;
			case "PAUSE_ROLLOUT" -> ActionType.PAUSE_ROLLOUT;
			case "OBSERVE_ONLY" -> ActionType.OBSERVE_ONLY;
			default -> ActionType.valueOf(value.toUpperCase(Locale.ROOT));
		};
	}

	private RiskLevel toRiskLevel(String riskLevel) {
		if (riskLevel == null || riskLevel.isBlank()) {
			return RiskLevel.HIGH;
		}

		return switch (riskLevel.toUpperCase(Locale.ROOT)) {
			case "LOW" -> RiskLevel.LOW;
			case "MEDIUM" -> RiskLevel.MEDIUM;
			case "HIGH" -> RiskLevel.HIGH;
			case "CRITICAL" -> RiskLevel.CRITICAL;
			default -> RiskLevel.HIGH;
		};
	}

	private BlastRadius toBlastRadius(String targetLayer) {
		if (targetLayer == null || targetLayer.isBlank()) {
			return BlastRadius.SERVICE;
		}

		return switch (targetLayer.toUpperCase(Locale.ROOT)) {
			case "POD", "INSTANCE" -> BlastRadius.INSTANCE;
			case "SERVICE", "APPLICATION", "DEPLOYMENT", "WORKER", "QUEUE" -> BlastRadius.SERVICE;
			case "SYSTEM", "CLUSTER", "DATABASE" -> BlastRadius.SYSTEM;
			case "GLOBAL" -> BlastRadius.GLOBAL;
			default -> BlastRadius.SERVICE;
		};
	}

	private PaymentSafety toPaymentSafety(ActionTarget target) {
		if (target != null && "payment".equalsIgnoreCase(target.domain())) {
			return PaymentSafety.requiredSafe();
		}

		return new PaymentSafety(true, true, DuplicateExecutionRisk.LOW);
	}

	private String extractDomain(String targetService) {
		if (targetService == null || targetService.isBlank()) {
			return "unknown";
		}
		int index = targetService.indexOf('-');
		return index > 0 ? targetService.substring(0, index) : targetService;
	}

	private String resourceType(String targetLayer) {
		if (targetLayer == null || targetLayer.isBlank()) {
			return "unknown";
		}
		return targetLayer.toLowerCase(Locale.ROOT);
	}

	private String normalizeMetric(String check) {
		if (check == null || check.isBlank()) {
			return "unknown";
		}
		int spaceIndex = check.indexOf(' ');
		return spaceIndex > 0 ? check.substring(0, spaceIndex) : check;
	}

	private List<VerificationCommand> withPaymentSafetyVerification(
			ActionTarget target,
			List<VerificationCommand> verifications
	) {
		if (target == null || !"payment".equalsIgnoreCase(target.domain())) {
			return verifications;
		}
		boolean alreadyPresent = verifications.stream()
				.anyMatch(verification -> contains(verification.metric(), "idempotency")
						|| contains(verification.metric(), "duplicate")
						|| contains(verification.metric(), "consistency")
						|| contains(verification.description(), "멱등성")
						|| contains(verification.description(), "중복 결제")
						|| contains(verification.description(), "정합성"));
		if (alreadyPresent) {
			return verifications;
		}

		java.util.ArrayList<VerificationCommand> enriched = new java.util.ArrayList<>(verifications);
		enriched.add(new VerificationCommand(
				"payment.consistency",
				"stable",
				"결제 정합성 / 멱등성 / 중복 결제 여부 확인"
		));
		return List.copyOf(enriched);
	}

	private boolean contains(String value, String keyword) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
	}

	private Severity parseSeverity(String severityHint) {
		if (severityHint == null || severityHint.isBlank()) {
			return Severity.SEV_2;
		}
		return switch (severityHint.trim().toUpperCase(Locale.ROOT)) {
			case "SEV_1", "SEV1" -> Severity.SEV_1;
			case "SEV_3", "SEV3" -> Severity.SEV_3;
			default -> Severity.SEV_2;
		};
	}
}
