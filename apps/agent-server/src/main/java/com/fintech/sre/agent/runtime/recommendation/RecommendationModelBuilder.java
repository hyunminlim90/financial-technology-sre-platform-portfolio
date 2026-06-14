package com.fintech.sre.agent.runtime.recommendation;

import java.time.Instant;
import java.util.Objects;

public class RecommendationModelBuilder {

	private RecommendationGeneration recommendationGeneration;
	private String recommendationId;
	private String title;
	private String summary;
	private RecommendationModelType recommendationType;
	private RecommendationModelReason recommendationReason;
	private RecommendationModelScope scope;
	private String scenarioId;
	private String runbookId;
	private String rollbackId;
	private String verificationId;
	private String evidenceReference;
	private String paymentSafetyClassification;
	private Instant generatedAt;

	public RecommendationModelBuilder recommendationGeneration(
			RecommendationGeneration recommendationGeneration
	) {
		this.recommendationGeneration = recommendationGeneration;
		return this;
	}

	public RecommendationModelBuilder recommendationId(String recommendationId) {
		this.recommendationId = recommendationId;
		return this;
	}

	public RecommendationModelBuilder title(String title) {
		this.title = title;
		return this;
	}

	public RecommendationModelBuilder summary(String summary) {
		this.summary = summary;
		return this;
	}

	public RecommendationModelBuilder recommendationType(
			RecommendationModelType recommendationType
	) {
		this.recommendationType = recommendationType;
		return this;
	}

	public RecommendationModelBuilder recommendationReason(
			RecommendationModelReason recommendationReason
	) {
		this.recommendationReason = recommendationReason;
		return this;
	}

	public RecommendationModelBuilder scope(RecommendationModelScope scope) {
		this.scope = scope;
		return this;
	}

	public RecommendationModelBuilder scenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
		return this;
	}

	public RecommendationModelBuilder runbookId(String runbookId) {
		this.runbookId = runbookId;
		return this;
	}

	public RecommendationModelBuilder rollbackId(String rollbackId) {
		this.rollbackId = rollbackId;
		return this;
	}

	public RecommendationModelBuilder verificationId(String verificationId) {
		this.verificationId = verificationId;
		return this;
	}

	public RecommendationModelBuilder evidenceReference(String evidenceReference) {
		this.evidenceReference = evidenceReference;
		return this;
	}

	public RecommendationModelBuilder paymentSafetyClassification(
			String paymentSafetyClassification
	) {
		this.paymentSafetyClassification = paymentSafetyClassification;
		return this;
	}

	public RecommendationModelBuilder generatedAt(Instant generatedAt) {
		this.generatedAt = generatedAt;
		return this;
	}

	public RecommendationModel build() {
		Objects.requireNonNull(
				recommendationGeneration,
				"recommendationGeneration must not be null"
		);
		if (recommendationGeneration.level()
				!= RecommendationGenerationLevel.GENERATABLE) {
			throw new IllegalStateException(
					"only GENERATABLE recommendation generation can create model"
			);
		}

		return new RecommendationModel(
				recommendationId,
				title,
				summary,
				recommendationType,
				recommendationReason == null
						? RecommendationModelReason.UNKNOWN
						: recommendationReason,
				scope == null ? RecommendationModelScope.RECOMMENDATION_MODEL : scope,
				scenarioId,
				runbookId,
				rollbackId,
				verificationId,
				evidenceReference,
				paymentSafetyClassification,
				generatedAt
		);
	}
}
