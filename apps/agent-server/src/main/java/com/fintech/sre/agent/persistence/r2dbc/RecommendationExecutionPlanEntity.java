package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("execution_plan_records")
public class RecommendationExecutionPlanEntity {

	@Id
	@Column("execution_plan_id")
	private String executionPlanId;

	@Column("recommendation_record_id")
	private String recommendationRecordId;

	@Column("incident_id")
	private String incidentId;

	@Column("status")
	private String status;

	@Column("executable")
	private boolean executable;

	@Column("requires_final_approval")
	private boolean requiresFinalApproval;

	@Column("created_by")
	private String createdBy;

	@Column("reason")
	private String reason;

	@Column("created_at")
	private Instant createdAt;

	@Column("steps")
	private String stepsJson;

	@Column("blocked_reasons")
	private String blockedReasonsJson;

	@Column("metadata")
	private String metadataJson;

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

	public boolean isExecutable() {
		return executable;
	}

	public void setExecutable(boolean executable) {
		this.executable = executable;
	}

	public boolean isRequiresFinalApproval() {
		return requiresFinalApproval;
	}

	public void setRequiresFinalApproval(boolean requiresFinalApproval) {
		this.requiresFinalApproval = requiresFinalApproval;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getStepsJson() {
		return stepsJson;
	}

	public void setStepsJson(String stepsJson) {
		this.stepsJson = stepsJson;
	}

	public String getBlockedReasonsJson() {
		return blockedReasonsJson;
	}

	public void setBlockedReasonsJson(String blockedReasonsJson) {
		this.blockedReasonsJson = blockedReasonsJson;
	}

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}
}
