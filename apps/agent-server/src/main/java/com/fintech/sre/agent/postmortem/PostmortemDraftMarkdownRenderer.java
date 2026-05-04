package com.fintech.sre.agent.postmortem;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class PostmortemDraftMarkdownRenderer {

	public String render(PostmortemDraft draft) {
		return """
				# Postmortem Draft: %s

				> Status: Draft
				> Human Review Required: %s
				> Created At: %s

				## 1. Incident Summary

				- Incident ID: `%s`
				- Root Cause: **확인 필요**
				- Impact Summary: %s

				## 2. Timeline / Action History

				%s

				## 3. Recommended Actions

				%s

				## 4. Human Decisions

				%s

				## 5. Outcome Summary

				%s

				## 6. Observed Signals

				%s

				## 7. Learning Candidates

				%s

				## 8. Required Human Review

				- 실제 Root Cause 확정
				- 고객 영향 범위 확인
				- 결제 정합성 / 중복 결제 여부 확인
				- 개선 항목 확정
				- preventive-design 연결 여부 확인
				""".formatted(
				draft.title(),
				draft.requiresHumanReview(),
				draft.createdAt(),
				draft.incidentId(),
				draft.impactSummary(),
				renderTimeline(draft),
				renderRecommendedActions(draft),
				renderHumanDecisions(draft),
				renderOutcomes(draft),
				renderSignals(draft),
				renderLearningCandidates(draft)
		);
	}

	private String renderTimeline(PostmortemDraft draft) {
		if (draft.actionLogs().isEmpty()) {
			return "- 확인된 action log 없음";
		}

		return draft.actionLogs().stream()
				.map(log -> "- `%s` — `%s` — %s".formatted(
						log.updatedAt(),
						log.status(),
						safe(log.recommendedActionText())
				))
				.collect(Collectors.joining("\n"));
	}

	private String renderRecommendedActions(PostmortemDraft draft) {
		if (draft.actionLogs().isEmpty()) {
			return "- 추천 Action 없음";
		}

		return draft.actionLogs().stream()
				.map(log -> """
						- ActionLog ID: `%s`
						  - Action: %s
						  - Command Type: `%s`
						  - Target: `%s`
						  - Human Approval Required: `%s`
						  - Rollback: %s
						  - Verification: %s
						""".formatted(
						log.id(),
						safe(log.recommendedActionText()),
						log.command() == null ? "확인 필요" : log.command().type(),
						log.command() == null || log.command().target() == null
								? "확인 필요"
								: log.command().target().domain() + "/" + log.command().target().service(),
						log.command() == null ? "확인 필요" : log.command().requiresHumanApproval(),
						log.command() == null || log.command().rollback() == null
								? "확인 필요"
								: safe(log.command().rollback().description()),
						log.command() == null || log.command().verifications() == null
								? "확인 필요"
								: log.command().verifications().toString()
				))
				.collect(Collectors.joining("\n"));
	}

	private String renderHumanDecisions(PostmortemDraft draft) {
		if (draft.actionLogs().isEmpty()) {
			return "- 확인 필요";
		}

		return draft.actionLogs().stream()
				.map(log -> "- `%s`: %s".formatted(
						log.status(),
						safe(log.humanDecisionReason())
				))
				.collect(Collectors.joining("\n"));
	}

	private String renderOutcomes(PostmortemDraft draft) {
		if (draft.actionLogs().isEmpty()) {
			return "- 확인 필요";
		}

		return draft.actionLogs().stream()
				.map(log -> "- `%s`: %s".formatted(
						log.outcomeStatus(),
						safe(log.outcomeSummary())
				))
				.collect(Collectors.joining("\n"));
	}

	private String renderSignals(PostmortemDraft draft) {
		String rendered = draft.actionLogs().stream()
				.flatMap(log -> log.observedSignals().stream())
				.distinct()
				.map(signal -> "- " + signal)
				.collect(Collectors.joining("\n"));
		return rendered.isBlank() ? "- 확인 필요" : rendered;
	}

	private String renderLearningCandidates(PostmortemDraft draft) {
		if (draft.learningCandidates().isEmpty()) {
			return "- 확인 필요";
		}

		return draft.learningCandidates().stream()
				.map(candidate -> "- " + candidate)
				.collect(Collectors.joining("\n"));
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "확인 필요" : value;
	}
}
