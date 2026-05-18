package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("recommendation_approval_records")
public class RecommendationApprovalRecordEntity {

	@Id
	@Column("recommendation_approval_id")
	private String approvalId;

	@Column("recommendation_record_id")
	private String recommendationRecordId;

	@Column("incident_id")
	private String incidentId;

	@Column("status")
	private String status;

	@Column("operator_id")
	private String operatorId;

	@Column("reason")
	private String reason;

	@Column("decided_at")
	private Instant decidedAt;

	@Column("metadata")
	private String metadataJson;

	public String getApprovalId() {
		return approvalId;
	}

	public void setApprovalId(String approvalId) {
		this.approvalId = approvalId;
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

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Instant getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(Instant decidedAt) {
		this.decidedAt = decidedAt;
	}

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}
}
