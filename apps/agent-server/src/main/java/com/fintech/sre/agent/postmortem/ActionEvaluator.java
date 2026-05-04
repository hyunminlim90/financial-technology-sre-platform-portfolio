package com.fintech.sre.agent.postmortem;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.actionlog.model.ExecutedActionLog;

import reactor.core.publisher.Mono;

@Component
public class ActionEvaluator {

	public Mono<List<String>> evaluate(PostmortemGenerationInput input) {
		var actions = input.context().actionLogSnapshot().executedActions();
		if (actions.isEmpty()) {
			return Mono.just(List.of("수행된 Action 기록이 없어 대응 효과 평가 신뢰도가 낮음"));
		}

		return Mono.just(actions.stream()
				.map(this::evaluateAction)
				.toList());
	}

	private String evaluateAction(ExecutedActionLog action) {
		String verificationSummary = action.verifications().stream()
				.map(verification -> "%s: %s -> %s (%s)".formatted(
						verification.metricName(),
						verification.beforeValue(),
						verification.afterValue(),
						verification.status()
				))
				.collect(Collectors.joining(", "));

		return """
		Action: %s
		Executed by: %s
		Method: %s
		Expected: %s
		Actual: %s
		Rollback executed: %s
		Verification: %s
		""".formatted(
				action.action(),
				action.executedBy(),
				action.executionMethod(),
				action.expectedEffect(),
				action.actualEffect(),
				action.rollbackExecuted(),
				verificationSummary
		);
	}
}
