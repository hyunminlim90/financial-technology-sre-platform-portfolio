package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("recommendation_records")
public class RecommendationRecordEntity {

	@Id
	@Column("recommendation_record_id")
	private String recommendationRecordId;

	@Column("incident_id")
	private String incidentId;

	@Column("audit_id")
	private String auditId;

	@Column("source")
	private String source;

	@Column("service")
	private String service;

	@Column("domain")
	private String domain;

	@Column("severity")
	private String severity;

	@Column("status")
	private String status;

	@Column("generated_at")
	private Instant generatedAt;

	@Column("recommended_action_count")
	private int recommendedActionCount;

	@Column("forbidden_action_count")
	private int forbiddenActionCount;

	@Column("policy_decision")
	private String policyDecision;

	@Column("guardrail_decision")
	private String guardrailDecision;

	@Column("action_types_json")
	private String actionTypesJson;

	@Column("blocked_reasons_json")
	private String blockedReasonsJson;

	@Column("metadata_json")
	private String metadataJson;

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

	public String getAuditId() {
		return auditId;
	}

	public void setAuditId(String auditId) {
		this.auditId = auditId;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(Instant generatedAt) {
		this.generatedAt = generatedAt;
	}

	public int getRecommendedActionCount() {
		return recommendedActionCount;
	}

	public void setRecommendedActionCount(int recommendedActionCount) {
		this.recommendedActionCount = recommendedActionCount;
	}

	public int getForbiddenActionCount() {
		return forbiddenActionCount;
	}

	public void setForbiddenActionCount(int forbiddenActionCount) {
		this.forbiddenActionCount = forbiddenActionCount;
	}

	public String getPolicyDecision() {
		return policyDecision;
	}

	public void setPolicyDecision(String policyDecision) {
		this.policyDecision = policyDecision;
	}

	public String getGuardrailDecision() {
		return guardrailDecision;
	}

	public void setGuardrailDecision(String guardrailDecision) {
		this.guardrailDecision = guardrailDecision;
	}

	public String getActionTypesJson() {
		return actionTypesJson;
	}

	public void setActionTypesJson(String actionTypesJson) {
		this.actionTypesJson = actionTypesJson;
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
