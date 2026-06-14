package com.fintech.sre.agent.runtime.recommendation;

import java.time.Instant;
import java.util.Objects;

public class RecommendationContentBuilder {

	private RecommendationCandidate recommendationCandidate;
	private String recommendationId;
	private String title;
	private String summary;
	private RecommendationContentType recommendationType;
	private RecommendationContentReason reason;
	private RecommendationContentScope scope;
	private String scenarioId;
	private String runbookId;
	private String rollbackId;
	private String verificationId;
	private String paymentSafetyClassification;
	private Instant generatedAt;

	public RecommendationContentBuilder recommendationCandidate(
			RecommendationCandidate recommendationCandidate
	) {
		this.recommendationCandidate = recommendationCandidate;
		return this;
	}

	public RecommendationContentBuilder recommendationId(String recommendationId) {
		this.recommendationId = recommendationId;
		return this;
	}

	public RecommendationContentBuilder title(String title) {
		this.title = title;
		return this;
	}

	public RecommendationContentBuilder summary(String summary) {
		this.summary = summary;
		return this;
	}

	public RecommendationContentBuilder recommendationType(
			RecommendationContentType recommendationType
	) {
		this.recommendationType = recommendationType;
		return this;
	}

	public RecommendationContentBuilder reason(RecommendationContentReason reason) {
		this.reason = reason;
		return this;
	}

	public RecommendationContentBuilder scope(RecommendationContentScope scope) {
		this.scope = scope;
		return this;
	}

	public RecommendationContentBuilder scenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
		return this;
	}

	public RecommendationContentBuilder runbookId(String runbookId) {
		this.runbookId = runbookId;
		return this;
	}

	public RecommendationContentBuilder rollbackId(String rollbackId) {
		this.rollbackId = rollbackId;
		return this;
	}

	public RecommendationContentBuilder verificationId(String verificationId) {
		this.verificationId = verificationId;
		return this;
	}

	public RecommendationContentBuilder paymentSafetyClassification(
			String paymentSafetyClassification
	) {
		this.paymentSafetyClassification = paymentSafetyClassification;
		return this;
	}

	public RecommendationContentBuilder generatedAt(Instant generatedAt) {
		this.generatedAt = generatedAt;
		return this;
	}

	public RecommendationContent build() {
		Objects.requireNonNull(
				recommendationCandidate,
				"recommendationCandidate must not be null"
		);
		if (recommendationCandidate.level() != RecommendationCandidateLevel.ELIGIBLE) {
			throw new IllegalStateException(
					"only ELIGIBLE recommendation candidate can create content"
			);
		}

		return new RecommendationContent(
				recommendationId,
				title,
				summary,
				recommendationType,
				reason == null ? RecommendationContentReason.UNKNOWN : reason,
				scope == null ? RecommendationContentScope.RECOMMENDATION_CONTENT : scope,
				scenarioId,
				runbookId,
				rollbackId,
				verificationId,
				paymentSafetyClassification,
				generatedAt
		);
	}
}
