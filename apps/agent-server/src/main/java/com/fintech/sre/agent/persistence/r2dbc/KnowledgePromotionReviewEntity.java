package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("knowledge_promotion_review_records")
public class KnowledgePromotionReviewEntity {

	@Id
	@Column("promotion_review_id")
	private String promotionReviewId;

	@Column("learning_candidate_id")
	private String learningCandidateId;

	@Column("incident_id")
	private String incidentId;

	@Column("status")
	private String status;

	@Column("reviewed_by")
	private String reviewedBy;

	@Column("review_reason")
	private String reviewReason;

	@Column("review_summary")
	private String reviewSummary;

	@Column("reviewed_at")
	private Instant reviewedAt;

	@Column("metadata")
	private String metadataJson;

	public String getPromotionReviewId() {
		return promotionReviewId;
	}

	public void setPromotionReviewId(String promotionReviewId) {
		this.promotionReviewId = promotionReviewId;
	}

	public String getLearningCandidateId() {
		return learningCandidateId;
	}

	public void setLearningCandidateId(String learningCandidateId) {
		this.learningCandidateId = learningCandidateId;
	}

	public String getIncidentId() {
		return incidentId;
	}

	public void setIncidentId(String incidentId) {
		this.incidentId = incidentId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getReviewedBy() {
		return reviewedBy;
	}

	public void setReviewedBy(String reviewedBy) {
		this.reviewedBy = reviewedBy;
	}

	public String getReviewReason() {
		return reviewReason;
	}

	public void setReviewReason(String reviewReason) {
		this.reviewReason = reviewReason;
	}

	public String getReviewSummary() {
		return reviewSummary;
	}

	public void setReviewSummary(String reviewSummary) {
		this.reviewSummary = reviewSummary;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(Instant reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}
}
