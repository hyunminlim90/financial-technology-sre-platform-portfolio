package com.fintech.sre.agent.decision.report;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.evidence.Evidence;
import com.fintech.sre.agent.knowledge.layering.KnowledgeLayeringIssue;
import com.fintech.sre.agent.policy.PolicyViolation;

@Component
public class DecisionReportMarkdownRenderer {

	public String render(DecisionReport report) {
		return """
				# Decision Report

				> Status: %s
				> Incident ID: `%s`
				> Scenario ID: `%s`
				> Runbook ID: `%s`

				## 1. Evidence Used

				%s

				## 2. Recommended Actions

				%s

				## 3. Blocked Actions

				%s

				## 4. Human Review Requirements

				%s

				## 5. Knowledge Layering Issues

				%s

				## 6. Safety Notes

				- AI did not execute any action.
				- Human approval is required before execution.
				- Root cause is not determined by AI.
				- rag/docs alone must not justify action selection.
				- Payment consistency, idempotency, and duplicate payment prevention remain mandatory.
				""".formatted(
				report.status(),
				safe(report.incidentId()),
				safe(report.scenarioId()),
				safe(report.runbookId()),
				renderEvidence(report.evidenceSignals()),
				renderActions(report.actions(), true),
				renderActions(report.actions(), false),
				renderHumanReviewRequirements(report.humanReviewRequirements()),
				renderKnowledgeLayeringIssues(report.knowledgeLayeringIssues())
		);
	}

	private String renderEvidence(List<Evidence> signals) {
		if (signals == null || signals.isEmpty()) {
			return "- 확인된 Evidence 없음";
		}

		return signals.stream()
				.map(signal -> "- `%s` `%s` = `%s` — %s".formatted(
						signal.signalType(),
						signal.signalName(),
						signal.signalValue(),
						safe(signal.description())
				))
				.collect(Collectors.joining("\n"));
	}

	private String renderActions(List<DecisionReportAction> actions, boolean recommended) {
		if (actions == null || actions.isEmpty()) {
			return "- 없음";
		}

		List<DecisionReportAction> filtered = actions.stream()
				.filter(action -> recommended ? action.recommended() : action.blocked())
				.toList();

		if (filtered.isEmpty()) {
			return "- 없음";
		}

		return filtered.stream()
				.map(action -> """
						- Action: %s
						  - Command Type: `%s`
						  - Target: `%s`
						  - Reason: %s
						  - Policy Violations: %s
						  - Guardrail Violations: %s
						""".formatted(
						safe(action.actionText()),
						action.command() == null ? "확인 필요" : action.command().type(),
						action.command() == null || action.command().target() == null
								? "확인 필요"
								: action.command().target().domain() + "/" + action.command().target().service(),
						safe(action.decisionReason()),
						renderPolicyViolations(action.policyViolations()),
						renderGuardrailViolations(action.guardrailViolations())
				))
				.collect(Collectors.joining("\n"));
	}

	private String renderPolicyViolations(List<PolicyViolation> violations) {
		if (violations == null || violations.isEmpty()) {
			return "없음";
		}

		return violations.stream()
				.map(violation -> "`%s:%s`".formatted(violation.severity(), violation.code()))
				.collect(Collectors.joining(", "));
	}

	private String renderGuardrailViolations(List<String> violations) {
		if (violations == null || violations.isEmpty()) {
			return "없음";
		}

		return violations.stream()
				.map(violation -> "`" + violation + "`")
				.collect(Collectors.joining(", "));
	}

	private String renderHumanReviewRequirements(List<String> requirements) {
		if (requirements == null || requirements.isEmpty()) {
			return "- Human approval required before execution";
		}

		return requirements.stream()
				.map(requirement -> "- " + requirement)
				.collect(Collectors.joining("\n"));
	}

	private String renderKnowledgeLayeringIssues(List<KnowledgeLayeringIssue> issues) {
		if (issues == null || issues.isEmpty()) {
			return "- 없음";
		}

		return issues.stream()
				.map(issue -> "- `%s:%s` %s".formatted(
						issue.severity(),
						issue.code(),
						issue.message()
				))
				.collect(Collectors.joining("\n"));
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "확인 필요" : value;
	}
}
