package com.fintech.sre.agent.persistence.r2dbc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("knowledge_update_application_records")
public class KnowledgeUpdateApplicationEntity {

	@Id
	@Column("knowledge_update_application_id")
	private String knowledgeUpdateApplicationId;

	@Column("incident_id")
	private String incidentId;

	@Column("learning_candidate_id")
	private String learningCandidateId;

	@Column("promotion_plan_id")
	private String promotionPlanId;

	@Column("knowledge_type")
	private String knowledgeType;

	@Column("knowledge_layer")
	private String knowledgeLayer;

	@Column("file_path")
	private String filePath;

	@Column("change_type")
	private String changeType;

	@Column("git_repository")
	private String gitRepository;

	@Column("git_branch")
	private String gitBranch;

	@Column("git_commit_sha")
	private String gitCommitSha;

	@Column("pull_request_reference")
	private String pullRequestReference;

	@Column("applied_by")
	private String appliedBy;

	@Column("reviewed_by")
	private String reviewedBy;

	@Column("approved_by")
	private String approvedBy;

	@Column("validation_checks")
	private String validationChecksJson;

	@Column("applied_at")
	private Instant appliedAt;

	@Column("metadata")
	private String metadataJson;

	public String getKnowledgeUpdateApplicationId() {
		return knowledgeUpdateApplicationId;
	}

	public void setKnowledgeUpdateApplicationId(String knowledgeUpdateApplicationId) {
		this.knowledgeUpdateApplicationId = knowledgeUpdateApplicationId;
	}

	public String getIncidentId() {
		return incidentId;
	}

	public void setIncidentId(String incidentId) {
		this.incidentId = incidentId;
	}

	public String getLearningCandidateId() {
		return learningCandidateId;
	}

	public void setLearningCandidateId(String learningCandidateId) {
		this.learningCandidateId = learningCandidateId;
	}

	public String getPromotionPlanId() {
		return promotionPlanId;
	}

	public void setPromotionPlanId(String promotionPlanId) {
		this.promotionPlanId = promotionPlanId;
	}

	public String getKnowledgeType() {
		return knowledgeType;
	}

	public void setKnowledgeType(String knowledgeType) {
		this.knowledgeType = knowledgeType;
	}

	public String getKnowledgeLayer() {
		return knowledgeLayer;
	}

	public void setKnowledgeLayer(String knowledgeLayer) {
		this.knowledgeLayer = knowledgeLayer;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	public String getGitRepository() {
		return gitRepository;
	}

	public void setGitRepository(String gitRepository) {
		this.gitRepository = gitRepository;
	}

	public String getGitBranch() {
		return gitBranch;
	}

	public void setGitBranch(String gitBranch) {
		this.gitBranch = gitBranch;
	}

	public String getGitCommitSha() {
		return gitCommitSha;
	}

	public void setGitCommitSha(String gitCommitSha) {
		this.gitCommitSha = gitCommitSha;
	}

	public String getPullRequestReference() {
		return pullRequestReference;
	}

	public void setPullRequestReference(String pullRequestReference) {
		this.pullRequestReference = pullRequestReference;
	}

	public String getAppliedBy() {
		return appliedBy;
	}

	public void setAppliedBy(String appliedBy) {
		this.appliedBy = appliedBy;
	}

	public String getReviewedBy() {
		return reviewedBy;
	}

	public void setReviewedBy(String reviewedBy) {
		this.reviewedBy = reviewedBy;
	}

	public String getApprovedBy() {
		return approvedBy;
	}

	public void setApprovedBy(String approvedBy) {
		this.approvedBy = approvedBy;
	}

	public String getValidationChecksJson() {
		return validationChecksJson;
	}

	public void setValidationChecksJson(String validationChecksJson) {
		this.validationChecksJson = validationChecksJson;
	}

	public Instant getAppliedAt() {
		return appliedAt;
	}

	public void setAppliedAt(Instant appliedAt) {
		this.appliedAt = appliedAt;
	}

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}
}
