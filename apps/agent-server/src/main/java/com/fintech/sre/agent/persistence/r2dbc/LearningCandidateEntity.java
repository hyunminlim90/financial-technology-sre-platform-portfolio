package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("learning_candidate_records")
public class LearningCandidateEntity {

	@Id
	@Column("learning_candidate_id")
	private String learningCandidateId;

	@Column("incident_id")
	private String incidentId;

	@Column("postmortem_draft_id")
	private String postmortemDraftId;

	@Column("postmortem_review_id")
	private String postmortemReviewId;

	@Column("type")
	private String type;

	@Column("status")
	private String status;

	@Column("promoted_by")
	private String promotedBy;

	@Column("summary")
	private String summary;

	@Column("proposed_changes")
	private String proposedChangesJson;

	@Column("created_at")
	private Instant createdAt;

	@Column("metadata")
	private String metadataJson;

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

	public String getPostmortemDraftId() {
		return postmortemDraftId;
	}

	public void setPostmortemDraftId(String postmortemDraftId) {
		this.postmortemDraftId = postmortemDraftId;
	}

	public String getPostmortemReviewId() {
		return postmortemReviewId;
	}

	public void setPostmortemReviewId(String postmortemReviewId) {
		this.postmortemReviewId = postmortemReviewId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPromotedBy() {
		return promotedBy;
	}

	public void setPromotedBy(String promotedBy) {
		this.promotedBy = promotedBy;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getProposedChangesJson() {
		return proposedChangesJson;
	}

	public void setProposedChangesJson(String proposedChangesJson) {
		this.proposedChangesJson = proposedChangesJson;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}
}
