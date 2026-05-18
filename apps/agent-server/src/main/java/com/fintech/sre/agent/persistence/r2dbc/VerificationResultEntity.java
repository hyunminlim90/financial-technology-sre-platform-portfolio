package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("verification_result_records")
public class VerificationResultEntity {

	@Id
	@Column("verification_result_id")
	private String verificationResultId;

	@Column("execution_result_id")
	private String executionResultId;

	@Column("execution_plan_id")
	private String executionPlanId;

	@Column("recommendation_record_id")
	private String recommendationRecordId;

	@Column("incident_id")
	private String incidentId;

	@Column("status")
	private String status;

	@Column("operator_id")
	private String operatorId;

	@Column("summary")
	private String summary;

	@Column("verified_at")
	private Instant verifiedAt;

	@Column("metadata")
	private String metadataJson;

	public String getVerificationResultId() {
		return verificationResultId;
	}

	public void setVerificationResultId(String verificationResultId) {
		this.verificationResultId = verificationResultId;
	}

	public String getExecutionResultId() {
		return executionResultId;
	}

	public void setExecutionResultId(String executionResultId) {
		this.executionResultId = executionResultId;
	}

	public String getExecutionPlanId() {
		return executionPlanId;
	}

	public void setExecutionPlanId(String executionPlanId) {
		this.executionPlanId = executionPlanId;
	}

	public String getRecommendationRecordId() {
		return recommendationRecordId;
	}

	public void setRecommendationRecordId(String recommendationRecordId) {
		this.recommendationRecordId = recommendationRecordId;
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

	public String getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(String operatorId) {
		this.operatorId = operatorId;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public void setVerifiedAt(Instant verifiedAt) {
		this.verifiedAt = verifiedAt;
	}

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}
}
