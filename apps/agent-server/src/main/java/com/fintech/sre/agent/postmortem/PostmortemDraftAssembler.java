package com.fintech.sre.agent.postmortem;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.model.common.ConfidenceLevel;
import com.fintech.sre.agent.model.response.LearningCandidate;
import com.fintech.sre.agent.model.response.PostmortemDraft;
import com.fintech.sre.agent.model.response.PostmortemDraftResponse;
import com.fintech.sre.agent.model.response.PostmortemFrontMatter;
import com.fintech.sre.agent.model.response.TimelineEvent;

@Component
public class PostmortemDraftAssembler {

	public PostmortemDraftResponse assemble(
			PostmortemGenerationInput input,
			List<TimelineEvent> timeline,
			List<String> rootCauseHypotheses,
			List<String> actionEvaluations,
			List<LearningCandidate> learningCandidates,
			String filename
	) {
		var firstRecommendation = input.context().actionLogSnapshot().recommendations().get(0);
		java.time.Instant incidentStart = timeline.isEmpty()
				? input.context().incidentContext().occurredAt()
				: timeline.get(0).time();
		java.time.Instant incidentEnd = timeline.isEmpty()
				? incidentStart
				: timeline.get(timeline.size() - 1).time();
		long durationMinutes = Duration.between(incidentStart, incidentEnd).toMinutes();

		PostmortemFrontMatter frontMatter = new PostmortemFrontMatter(
				firstRecommendation.alertName() + " Postmortem",
				"postmortem",
				input.context().incidentContext().domainHint(),
				firstRecommendation.failureMode(),
				firstRecommendation.severity() == null ? "UNKNOWN" : firstRecommendation.severity(),
				firstRecommendation.environment(),
				List.of(firstRecommendation.service()),
				incidentStart,
				incidentEnd,
				durationMinutes,
				input.ragSearchResult().scenarios().stream().map(d -> d.path()).toList(),
				input.ragSearchResult().runbooks().stream().map(d -> d.path()).toList(),
				input.ragSearchResult().improvements().stream().map(d -> d.path()).toList(),
				input.ragSearchResult().preventiveDesigns().stream().map(d -> d.path()).toList(),
				List.of("incident", firstRecommendation.service()),
				"draft"
		);

		PostmortemDraft draft = new PostmortemDraft(
				"Incident " + input.request().incidentId() + " occurred on service " + firstRecommendation.service(),
				"Impact should be validated by Human based on business metrics.",
				timeline,
				List.of("Symptoms generated from metrics/logs/traces should be reviewed."),
				rootCauseHypotheses,
				List.of("Contributing factors require Human validation."),
				List.of("To be filled by Human."),
				actionEvaluations,
				learningCandidates.stream().map(LearningCandidate::title).toList(),
				List.of("Lessons learned must be finalized by Human."),
				List.of("Reproduction condition must be validated before approval.")
		);

		List<LearningCandidate> improvements = learningCandidates.stream()
				.filter(candidate -> candidate.type().equals("improvement"))
				.toList();

		List<LearningCandidate> preventiveDesigns = learningCandidates.stream()
				.filter(candidate -> candidate.type().equals("preventive-design"))
				.toList();

		return new PostmortemDraftResponse(
				input.request().incidentId(),
				"DRAFT_CREATED",
				filename,
				frontMatter,
				draft,
				improvements,
				preventiveDesigns,
				ConfidenceLevel.MEDIUM,
				true,
				List.of(
						"AI는 Root Cause를 확정하지 않았습니다.",
						"Human 검증 후에만 RAG에 반영해야 합니다."
				)
		);
	}
}
