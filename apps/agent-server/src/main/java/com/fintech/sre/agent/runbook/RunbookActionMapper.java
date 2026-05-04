package com.fintech.sre.agent.runbook;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.action.ActionTarget;
import com.fintech.sre.agent.action.ActionType;
import com.fintech.sre.agent.action.RollbackCommand;
import com.fintech.sre.agent.action.VerificationCommand;

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
				action.approval() != null && action.approval().required(),
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
				)
		);
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

	private ActionType toActionType(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("ActionType is required");
		}

		return switch (value) {
			case "APPLY_RATE_LIMIT" -> ActionType.RATE_LIMIT;
			case "SCALE_OUT_WORKER" -> ActionType.SCALE_OUT;
			case "SCALE_IN_WORKER" -> ActionType.SCALE_DOWN;
			case "RESTART_POD" -> ActionType.RESTART_POD;
			case "FAILOVER" -> ActionType.FAILOVER;
			case "TRAFFIC_SHED" -> ActionType.TRAFFIC_SHED;
			case "PAUSE_ROLLOUT" -> ActionType.PAUSE_ROLLOUT;
			case "OBSERVE_ONLY" -> ActionType.OBSERVE_ONLY;
			default -> ActionType.valueOf(value);
		};
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
}
