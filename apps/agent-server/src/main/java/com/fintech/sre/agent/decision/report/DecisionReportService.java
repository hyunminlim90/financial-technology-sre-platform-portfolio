package com.fintech.sre.agent.decision.report;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.decision.CandidateAction;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.knowledge.layering.KnowledgeLayeringIssue;
import com.fintech.sre.agent.policy.PolicyEngine;
import com.fintech.sre.agent.policy.PolicyEvaluationResult;
import com.fintech.sre.agent.policy.PolicyViolation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DecisionReportService {

	private final DecisionReportRepository repository;
	private final DecisionReportMarkdownRenderer markdownRenderer;
	private final PolicyEngine policyEngine;

	public DecisionReportService(
			DecisionReportRepository repository,
			DecisionReportMarkdownRenderer markdownRenderer,
			PolicyEngine policyEngine
	) {
		this.repository = repository;
		this.markdownRenderer = markdownRenderer;
		this.policyEngine = policyEngine;
	}

	public Mono<DecisionReport> createReport(
			String incidentId,
			String scenarioId,
			String runbookId,
			EvidenceContext evidence,
			List<CandidateAction> candidates,
			List<CandidateAction> recommendedActions,
			List<KnowledgeLayeringIssue> knowledgeLayeringIssues
	) {
		Set<String> recommendedKeys = (recommendedActions == null ? List.<CandidateAction>of() : recommendedActions).stream()
				.map(this::actionKey)
				.collect(Collectors.toSet());

		return Flux.fromIterable(candidates == null ? List.<CandidateAction>of() : candidates)
				.flatMap(candidate -> policyEngine.evaluate(candidate.command(), evidence)
						.map(result -> toReportAction(candidate, result, recommendedKeys.contains(actionKey(candidate)))))
				.collectList()
				.flatMap(actions -> {
					Instant now = Instant.now();

					DecisionReport raw = new DecisionReport(
							UUID.randomUUID().toString(),
							incidentId,
							scenarioId,
							runbookId,
							DecisionReportStatus.HUMAN_REVIEW_REQUIRED,
							evidence == null ? List.of() : evidence.evidences(),
							actions,
							knowledgeLayeringIssues == null ? List.of() : knowledgeLayeringIssues,
							humanReviewRequirements(),
							null,
							now,
							now
					);

					DecisionReport withMarkdown = new DecisionReport(
							raw.id(),
							raw.incidentId(),
							raw.scenarioId(),
							raw.runbookId(),
							raw.status(),
							raw.evidenceSignals(),
							raw.actions(),
							raw.knowledgeLayeringIssues(),
							raw.humanReviewRequirements(),
							markdownRenderer.render(raw),
							raw.createdAt(),
							raw.updatedAt()
					);

					return repository.save(withMarkdown);
				});
	}

	public Flux<DecisionReport> findByIncidentId(String incidentId) {
		return repository.findByIncidentId(incidentId);
	}

	public Mono<DecisionReport> findById(String id) {
		return repository.findById(id)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("DecisionReport not found: " + id)));
	}

	private DecisionReportAction toReportAction(
			CandidateAction candidate,
			PolicyEvaluationResult result,
			boolean recommended
	) {
		List<PolicyViolation> violations = result.violations() == null ? List.of() : result.violations();
		boolean blocked = !recommended || !result.allowed();

		return new DecisionReportAction(
				candidate.action(),
				candidate.command(),
				recommended && result.allowed(),
				blocked,
				violations,
				List.of(),
				blocked
						? (violations.isEmpty()
						? "Decision pipeline blocked this action after evidence/policy review."
						: "PolicyEngine blocked this action.")
						: "PolicyEngine allowed this action. Human approval is still required."
		);
	}

	private List<String> humanReviewRequirements() {
		return List.of(
				"Human must approve before execution.",
				"Root cause must be confirmed by human.",
				"Rollback plan must be reviewed.",
				"Verification metrics must be checked.",
				"Payment consistency/idempotency/duplicate payment impact must be reviewed."
		);
	}

	private String actionKey(CandidateAction action) {
		if (action.command() == null || action.command().type() == null) {
			return action.action();
		}
		return action.command().type().name();
	}
}
