package com.fintech.sre.agent.postmortem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.response.TimelineEvent;

import reactor.core.publisher.Mono;

@Component
public class TimelineReconstructor {

	public Mono<List<TimelineEvent>> reconstruct(PostmortemGenerationInput input) {
		var snapshot = input.context().actionLogSnapshot();
		List<TimelineEvent> events = new ArrayList<>();

		snapshot.recommendations().forEach(recommendation ->
				events.add(new TimelineEvent(
						recommendation.createdAt(),
						"AI recommendation created: " + recommendation.failureMode(),
						"recommendation"
				))
		);

		snapshot.executedActions().forEach(action ->
				events.add(new TimelineEvent(
						action.executedAt(),
						"Human executed action: " + action.action(),
						"executed_action"
				))
		);

		snapshot.verifications().forEach(verification ->
				events.add(new TimelineEvent(
						verification.checkedAt(),
						"Verification " + verification.status() + ": " + verification.metricName(),
						"verification"
				))
		);

		snapshot.rollbacks().forEach(rollback ->
				events.add(new TimelineEvent(
						rollback.rollbackAt(),
						"Rollback executed: " + rollback.rollbackAction(),
						"rollback"
				))
		);

		events.sort(Comparator.comparing(TimelineEvent::time));
		return Mono.just(events);
	}
}
