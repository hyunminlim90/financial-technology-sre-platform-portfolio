package com.fintech.sre.agent.runbook;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.decision.ActionRiskLevel;
import com.fintech.sre.agent.decision.ActionSource;
import com.fintech.sre.agent.decision.CandidateAction;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.evidence.EvidenceSignal;

@Component
public class RunbookCandidateActionFactory {

	private final RunbookLoader runbookLoader;
	private final RunbookConditionMatcher conditionMatcher;
	private final RunbookActionMapper actionMapper;

	public RunbookCandidateActionFactory(
			RunbookLoader runbookLoader,
			RunbookConditionMatcher conditionMatcher,
			RunbookActionMapper actionMapper
	) {
		this.runbookLoader = runbookLoader;
		this.conditionMatcher = conditionMatcher;
		this.actionMapper = actionMapper;
	}

	public List<CandidateAction> createCandidates(EvidenceContext evidenceContext) {
		return createCandidates(evidenceContext, "unknown");
	}

	public List<CandidateAction> createCandidates(EvidenceContext evidenceContext, String environment) {
		List<CandidateAction> candidates = new ArrayList<>();
		int step = 1;

		for (RunbookDefinition runbook : runbookLoader.loadAll()) {
			if (!requiredEvidenceSatisfied(runbook, evidenceContext)) {
				continue;
			}
			if (runbook.branches() == null) {
				continue;
			}

			for (RunbookBranch branch : runbook.branches()) {
				if (!conditionMatcher.matches(branch.when(), evidenceContext) || branch.actions() == null) {
					continue;
				}

				for (RunbookAction action : branch.actions()) {
					ActionCommand command = actionMapper.toCommand(action, environment);
					candidates.add(CandidateAction.builder()
							.step(step++)
							.action(action.description())
							.command(command)
							.expectedEffect(expectedEffect(command))
							.risk(riskDescription(action.riskLevel()))
							.rollbackPlan(action.rollback() == null ? null : action.rollback().plan())
							.verification(action.verification() == null ? List.of() : safeList(action.verification().checks()))
							.requiresHumanApproval(action.approval() != null && action.approval().required())
							.source(ActionSource.RUNBOOK)
							.riskLevel(toRiskLevel(action.riskLevel()))
							.build());
				}
			}
		}

		return candidates;
	}

	private boolean requiredEvidenceSatisfied(RunbookDefinition runbook, EvidenceContext context) {
		if (runbook.requiredEvidence() == null || runbook.requiredEvidence().isEmpty()) {
			return true;
		}
		return runbook.requiredEvidence().stream()
				.allMatch(signal -> {
					try {
						return context.hasReliableSignal(EvidenceSignal.valueOf(signal));
					} catch (Exception exception) {
						return false;
					}
				});
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

	private ActionRiskLevel toRiskLevel(String riskLevel) {
		if ("LOW".equalsIgnoreCase(riskLevel)) {
			return ActionRiskLevel.LOW;
		}
		if ("MEDIUM".equalsIgnoreCase(riskLevel)) {
			return ActionRiskLevel.MEDIUM;
		}
		return ActionRiskLevel.HIGH;
	}

	private List<String> safeList(List<String> values) {
		return values == null ? List.of() : values;
	}
}
