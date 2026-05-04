package com.fintech.sre.agent.postmortem;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.actionlog.model.IncidentActionLogSnapshot;
import com.fintech.sre.agent.actionlog.model.RecommendationLog;
import com.fintech.sre.agent.model.common.IncidentContext;
import com.fintech.sre.agent.model.request.PostmortemGenerateByIncidentRequest;
import com.fintech.sre.agent.observability.model.ObservabilityEvidence;

@Component
public class PostmortemIncidentContextBuilder {

	public PostmortemIncidentContext buildFromSnapshot(
			PostmortemGenerateByIncidentRequest request,
			IncidentActionLogSnapshot snapshot
	) {
		RecommendationLog firstRecommendation = snapshot.recommendations().get(0);
		IncidentContext incidentContext = IncidentContext.builder()
				.incidentId(snapshot.incidentId())
				.alertName(firstRecommendation.alertName())
				.service(firstRecommendation.service())
				.environment(firstRecommendation.environment())
				.severityHint(firstRecommendation.severity())
				.occurredAt(firstRecommendation.createdAt())
				.labels(Map.of("domain", extractDomain(firstRecommendation.failureMode())))
				.operatorNote(request.operatorSummary())
				.observabilityEvidence(new ObservabilityEvidence(List.of(), List.of(), List.of()))
				.build();

		return new PostmortemIncidentContext(
				incidentContext,
				snapshot,
				request.operatorSummary()
		);
	}

	private String extractDomain(String failureMode) {
		if (failureMode == null || !failureMode.contains("-")) {
			return "unknown";
		}
		return failureMode.split("-")[0];
	}
}
