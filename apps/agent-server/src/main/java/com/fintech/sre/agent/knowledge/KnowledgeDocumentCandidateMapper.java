package com.fintech.sre.agent.knowledge;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.ApprovalPolicy;
import com.fintech.sre.agent.action.BlastRadius;
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
import com.fintech.sre.agent.decision.generator.CandidateGenerationSource;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.model.common.AlternativeAction;
import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.common.ImpactScope;
import com.fintech.sre.agent.model.common.Severity;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

@Component
public class KnowledgeDocumentCandidateMapper {

	public DecisionCandidate toCandidate(
			KnowledgeDocument document,
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	) {
		ActionCommand command = toActionCommand(document);
		CandidateAction candidateAction = CandidateAction.builder()
				.step(1)
				.action(document.title())
				.command(command)
				.expectedEffect(document.summary())
				.risk(document.type() == KnowledgeDocumentType.RAG_DOC
						? "Reference-only knowledge. Use for observation and explanation."
						: "Knowledge-retrieved action requires human review.")
				.rollbackPlan(command.rollback() == null ? null : command.rollback().description())
				.verification(command.verifications().stream().map(VerificationCommand::description).toList())
				.requiresHumanApproval(command.requiresHumanApproval())
				.source(document.type() == KnowledgeDocumentType.RAG_DOC ? ActionSource.RUNBOOK : ActionSource.RUNBOOK)
				.riskLevel(command.type() == ActionType.OBSERVE_ONLY ? ActionRiskLevel.LOW : ActionRiskLevel.HIGH)
				.build();

		return DecisionCandidate.builder()
				.scenario(new MatchedScenario(
						document.type().name(),
						resolveDomain(document, request),
						document.title(),
						document.path(),
						parseSeverity(request == null ? null : request.severityHint()),
						ImpactScope.PARTIAL
				))
				.candidateActions(List.of(candidateAction))
				.recommendedActions(List.of(candidateAction))
				.alternativeActions(List.of(
						new AlternativeAction(
								"Continue observation only.",
								"Knowledge retrieval candidate should still be validated by policy and guardrail layers."
						)
				))
				.forbiddenActions(List.of())
				.mostLikelyCauses(List.of("Knowledge retrieval matched document: " + document.title()))
				.reasoningNotes(List.of(document.summary() == null ? "Knowledge retrieval candidate generated." : document.summary()))
				.confidenceLevel(toConfidence(document.score()))
				.policyEvaluationResult(null)
				.evidenceContext(evidenceContext)
				.guardrailDecision(null)
				.blockedReason(null)
				.candidateGenerationSource(CandidateGenerationSource.KNOWLEDGE_RETRIEVAL)
				.build()
				.withCandidateGenerationSource(CandidateGenerationSource.KNOWLEDGE_RETRIEVAL);
	}

	private ActionCommand toActionCommand(KnowledgeDocument document) {
		ActionType actionType = toActionType(document);
		ActionTarget target = new ActionTarget(
				document.domain(),
				document.service(),
				null,
				document.service(),
				null
		);

		return new ActionCommand(
				"knowledge:" + document.id(),
				actionType,
				target,
				RiskLevel.HIGH,
				BlastRadius.SERVICE,
				ApprovalPolicy.humanRequired(),
				RollbackPolicy.requiredPolicy(),
				VerificationPolicy.required(List.of("verify service health", "verify payment duplicate rate")),
				PaymentSafety.requiredSafe(),
				List.of("scenario evidence required", "runbook evidence required"),
				List.of("rag docs only evidence"),
				new RollbackCommand("Rollback using the matched runbook or revert traffic/action safely."),
				List.of(
						new VerificationCommand(
								"payment_duplicate_rate",
								"<= baseline",
								"Verify duplicate payment rate does not increase."
						)
				),
				document.summary()
		);
	}

	private ActionType toActionType(KnowledgeDocument document) {
		if (document.type() == KnowledgeDocumentType.RAG_DOC) {
			return ActionType.OBSERVE_ONLY;
		}

		if (document.actionTypes() == null || document.actionTypes().isEmpty()) {
			return ActionType.OBSERVE_ONLY;
		}

		try {
			return ActionType.valueOf(document.actionTypes().get(0).trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return ActionType.OBSERVE_ONLY;
		}
	}

	private String resolveDomain(KnowledgeDocument document, IncidentRecommendationRequest request) {
		if (document.domain() != null && !document.domain().isBlank()) {
			return document.domain();
		}
		if (request != null && request.labels() != null && request.labels().get("domain") != null) {
			return request.labels().get("domain");
		}
		if (request != null && request.service() != null && request.service().toLowerCase(Locale.ROOT).contains("payment")) {
			return "payment";
		}
		return "platform";
	}

	private ConfidenceLevel toConfidence(double score) {
		if (score >= 0.9d) {
			return ConfidenceLevel.HIGH;
		}
		if (score >= 0.5d) {
			return ConfidenceLevel.MEDIUM;
		}
		return ConfidenceLevel.LOW;
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
