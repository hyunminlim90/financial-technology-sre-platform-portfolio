package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("human_execution_result_records")
public class HumanExecutionResultEntity {

	@Id
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

	@Column("started_at")
	private Instant startedAt;

	@Column("finished_at")
	private Instant finishedAt;

	@Column("recorded_at")
	private Instant recordedAt;

	@Column("metadata")
	private String metadataJson;

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

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(Instant finishedAt) {
		this.finishedAt = finishedAt;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}

	public void setRecordedAt(Instant recordedAt) {
		this.recordedAt = recordedAt;
	}

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}
}
