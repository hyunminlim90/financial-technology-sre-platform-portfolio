package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("incident_lifecycle_records")
public class IncidentLifecycleEntity {

	@Id
	@Column("incident_lifecycle_id")
	private String incidentLifecycleId;

	@Column("incident_id")
	private String incidentId;

	@Column("previous_status")
	private String previousStatus;

	@Column("current_status")
	private String currentStatus;

	@Column("transition_reason")
	private String transitionReason;

	@Column("operator_id")
	private String operatorId;

	@Column("summary")
	private String summary;

	@Column("transitioned_at")
	private Instant transitionedAt;

	@Column("metadata")
	private String metadataJson;

	public String getIncidentLifecycleId() {
		return incidentLifecycleId;
	}

	public void setIncidentLifecycleId(String incidentLifecycleId) {
		this.incidentLifecycleId = incidentLifecycleId;
	}

	public String getIncidentId() {
		return incidentId;
	}

	public void setIncidentId(String incidentId) {
		this.incidentId = incidentId;
	}

	public String getPreviousStatus() {
		return previousStatus;
	}

	public void setPreviousStatus(String previousStatus) {
		this.previousStatus = previousStatus;
	}

	public String getCurrentStatus() {
		return currentStatus;
	}

	public void setCurrentStatus(String currentStatus) {
		this.currentStatus = currentStatus;
	}

	public String getTransitionReason() {
		return transitionReason;
	}

	public void setTransitionReason(String transitionReason) {
		this.transitionReason = transitionReason;
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

	public Instant getTransitionedAt() {
		return transitionedAt;
	}

	public void setTransitionedAt(Instant transitionedAt) {
		this.transitionedAt = transitionedAt;
	}

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}
}
