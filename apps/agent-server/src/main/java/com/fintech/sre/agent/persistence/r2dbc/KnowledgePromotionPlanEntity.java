package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("knowledge_promotion_plan_records")
public class KnowledgePromotionPlanEntity {

	@Id
	@Column("promotion_plan_id")
	private String promotionPlanId;

	@Column("learning_candidate_id")
	private String learningCandidateId;

	@Column("incident_id")
	private String incidentId;

	@Column("status")
	private String status;

	@Column("planned_by")
	private String plannedBy;

	@Column("summary")
	private String summary;

	@Column("targets")
	private String targetsJson;

	@Column("required_human_checks")
	private String requiredHumanChecksJson;

	@Column("blocked_reasons")
	private String blockedReasonsJson;

	@Column("created_at")
	private Instant createdAt;

	@Column("metadata")
	private String metadataJson;

	public String getPromotionPlanId() {
		return promotionPlanId;
	}

	public void setPromotionPlanId(String promotionPlanId) {
		this.promotionPlanId = promotionPlanId;
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

	public String getPlannedBy() {
		return plannedBy;
	}

	public void setPlannedBy(String plannedBy) {
		this.plannedBy = plannedBy;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getTargetsJson() {
		return targetsJson;
	}

	public void setTargetsJson(String targetsJson) {
		this.targetsJson = targetsJson;
	}

	public String getRequiredHumanChecksJson() {
		return requiredHumanChecksJson;
	}

	public void setRequiredHumanChecksJson(String requiredHumanChecksJson) {
		this.requiredHumanChecksJson = requiredHumanChecksJson;
	}

	public String getBlockedReasonsJson() {
		return blockedReasonsJson;
	}

	public void setBlockedReasonsJson(String blockedReasonsJson) {
		this.blockedReasonsJson = blockedReasonsJson;
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
