package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("postmortem_draft_records")
public class PostmortemDraftEntity {

	@Id
	@Column("postmortem_draft_id")
	private String postmortemDraftId;

	@Column("incident_id")
	private String incidentId;

	@Column("status")
	private String status;

	@Column("requested_by")
	private String requestedBy;

	@Column("summary")
	private String summary;

	@Column("timeline")
	private String timelineJson;

	@Column("recommendations")
	private String recommendationsJson;

	@Column("execution_results")
	private String executionResultsJson;

	@Column("verification_results")
	private String verificationResultsJson;

	@Column("reanalysis_candidates")
	private String reanalysisCandidatesJson;

	@Column("learning_candidates")
	private String learningCandidatesJson;

	@Column("open_questions")
	private String openQuestionsJson;

	@Column("created_at")
	private Instant createdAt;

	@Column("metadata")
	private String metadataJson;

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

	public String getRequestedBy() {
		return requestedBy;
	}

	public void setRequestedBy(String requestedBy) {
		this.requestedBy = requestedBy;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getTimelineJson() {
		return timelineJson;
	}

	public void setTimelineJson(String timelineJson) {
		this.timelineJson = timelineJson;
	}

	public String getRecommendationsJson() {
		return recommendationsJson;
	}

	public void setRecommendationsJson(String recommendationsJson) {
		this.recommendationsJson = recommendationsJson;
	}

	public String getExecutionResultsJson() {
		return executionResultsJson;
	}

	public void setExecutionResultsJson(String executionResultsJson) {
		this.executionResultsJson = executionResultsJson;
	}

	public String getVerificationResultsJson() {
		return verificationResultsJson;
	}

	public void setVerificationResultsJson(String verificationResultsJson) {
		this.verificationResultsJson = verificationResultsJson;
	}

	public String getReanalysisCandidatesJson() {
		return reanalysisCandidatesJson;
	}

	public void setReanalysisCandidatesJson(String reanalysisCandidatesJson) {
		this.reanalysisCandidatesJson = reanalysisCandidatesJson;
	}

	public String getLearningCandidatesJson() {
		return learningCandidatesJson;
	}

	public void setLearningCandidatesJson(String learningCandidatesJson) {
		this.learningCandidatesJson = learningCandidatesJson;
	}

	public String getOpenQuestionsJson() {
		return openQuestionsJson;
	}

	public void setOpenQuestionsJson(String openQuestionsJson) {
		this.openQuestionsJson = openQuestionsJson;
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
