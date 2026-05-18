package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("postmortem_review_records")
public class PostmortemReviewEntity {

	@Id
	@Column("postmortem_review_id")
	private String postmortemReviewId;

	@Column("postmortem_draft_id")
	private String postmortemDraftId;

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

	public String getPostmortemReviewId() {
		return postmortemReviewId;
	}

	public void setPostmortemReviewId(String postmortemReviewId) {
		this.postmortemReviewId = postmortemReviewId;
	}

	public String getPostmortemDraftId() {
		return postmortemDraftId;
	}

	public void setPostmortemDraftId(String postmortemDraftId) {
		this.postmortemDraftId = postmortemDraftId;
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
