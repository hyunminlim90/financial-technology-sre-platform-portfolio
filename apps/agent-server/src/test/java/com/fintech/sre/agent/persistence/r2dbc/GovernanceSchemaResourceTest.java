package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class GovernanceSchemaResourceTest {

	@Test
	void shouldContainRecommendationRecordsSchemaAndIndexes() throws IOException {
		ClassPathResource resource =
				new ClassPathResource("db/schema-governance.sql");

		assertThat(resource.exists()).isTrue();

		String sql = new String(
				resource.getInputStream().readAllBytes(),
				StandardCharsets.UTF_8
		);

		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS recommendation_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS recommendation_approval_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS execution_plan_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS human_execution_result_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS verification_result_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS incident_lifecycle_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS postmortem_draft_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS postmortem_review_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS learning_candidate_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS knowledge_promotion_review_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS knowledge_promotion_plan_records");
		assertThat(sql).contains("CREATE TABLE IF NOT EXISTS knowledge_update_application_records");
		assertThat(sql).contains("action_types JSONB");
		assertThat(sql).contains("blocked_reasons JSONB");
		assertThat(sql).contains("metadata JSONB");
		assertThat(sql).contains("idx_recommendation_records_incident_id");
		assertThat(sql).contains("idx_recommendation_records_generated_at");
		assertThat(sql).contains("idx_recommendation_records_metadata_gin");
		assertThat(sql).contains("idx_recommendation_approval_records_incident_id");
		assertThat(sql).contains("idx_recommendation_approval_records_recommendation_record_id");
		assertThat(sql).contains("idx_recommendation_approval_records_status");
		assertThat(sql).contains("idx_recommendation_approval_records_decided_at");
		assertThat(sql).contains("idx_recommendation_approval_records_metadata_gin");
		assertThat(sql).contains("idx_execution_plan_records_incident_id");
		assertThat(sql).contains("idx_execution_plan_records_recommendation_record_id");
		assertThat(sql).contains("idx_execution_plan_records_status");
		assertThat(sql).contains("idx_execution_plan_records_created_at");
		assertThat(sql).contains("idx_execution_plan_records_metadata_gin");
		assertThat(sql).contains("idx_human_execution_result_records_incident_id");
		assertThat(sql).contains("idx_human_execution_result_records_execution_plan_id");
		assertThat(sql).contains("idx_human_execution_result_records_recommendation_record_id");
		assertThat(sql).contains("idx_human_execution_result_records_status");
		assertThat(sql).contains("idx_human_execution_result_records_recorded_at");
		assertThat(sql).contains("idx_human_execution_result_records_metadata_gin");
		assertThat(sql).contains("idx_verification_result_records_incident_id");
		assertThat(sql).contains("idx_verification_result_records_execution_result_id");
		assertThat(sql).contains("idx_verification_result_records_recommendation_record_id");
		assertThat(sql).contains("idx_verification_result_records_status");
		assertThat(sql).contains("idx_verification_result_records_verified_at");
		assertThat(sql).contains("idx_verification_result_records_metadata_gin");
		assertThat(sql).contains("idx_incident_lifecycle_records_incident_id");
		assertThat(sql).contains("idx_incident_lifecycle_records_current_status");
		assertThat(sql).contains("idx_incident_lifecycle_records_transitioned_at");
		assertThat(sql).contains("idx_incident_lifecycle_records_incident_transitioned_at");
		assertThat(sql).contains("idx_incident_lifecycle_records_metadata_gin");
		assertThat(sql).contains("idx_postmortem_draft_records_incident_id");
		assertThat(sql).contains("idx_postmortem_draft_records_status");
		assertThat(sql).contains("idx_postmortem_draft_records_created_at");
		assertThat(sql).contains("idx_postmortem_draft_records_metadata_gin");
		assertThat(sql).contains("idx_postmortem_review_records_draft_id");
		assertThat(sql).contains("idx_postmortem_review_records_incident_id");
		assertThat(sql).contains("idx_postmortem_review_records_status");
		assertThat(sql).contains("idx_postmortem_review_records_reviewed_at");
		assertThat(sql).contains("idx_postmortem_review_records_metadata_gin");
		assertThat(sql).contains("idx_learning_candidate_records_incident_id");
		assertThat(sql).contains("idx_learning_candidate_records_draft_id");
		assertThat(sql).contains("idx_learning_candidate_records_review_id");
		assertThat(sql).contains("idx_learning_candidate_records_type");
		assertThat(sql).contains("idx_learning_candidate_records_status");
		assertThat(sql).contains("idx_learning_candidate_records_created_at");
		assertThat(sql).contains("idx_learning_candidate_records_metadata_gin");
		assertThat(sql).contains("idx_knowledge_promotion_review_records_candidate_id");
		assertThat(sql).contains("idx_knowledge_promotion_review_records_incident_id");
		assertThat(sql).contains("idx_knowledge_promotion_review_records_status");
		assertThat(sql).contains("idx_knowledge_promotion_review_records_reviewed_at");
		assertThat(sql).contains("idx_knowledge_promotion_review_records_metadata_gin");
		assertThat(sql).contains("idx_knowledge_promotion_plan_records_candidate_id");
		assertThat(sql).contains("idx_knowledge_promotion_plan_records_incident_id");
		assertThat(sql).contains("idx_knowledge_promotion_plan_records_status");
		assertThat(sql).contains("idx_knowledge_promotion_plan_records_created_at");
		assertThat(sql).contains("idx_knowledge_promotion_plan_records_metadata_gin");
		assertThat(sql).contains("idx_knowledge_update_application_records_incident_id");
		assertThat(sql).contains("idx_knowledge_update_application_records_learning_candidate_id");
		assertThat(sql).contains("idx_knowledge_update_application_records_promotion_plan_id");
		assertThat(sql).contains("idx_knowledge_update_application_records_knowledge_layer");
		assertThat(sql).contains("idx_knowledge_update_application_records_change_type");
		assertThat(sql).contains("idx_knowledge_update_application_records_applied_at");
		assertThat(sql).contains("idx_knowledge_update_application_records_metadata_gin");
		assertThat(sql).doesNotContain("Flyway");
		assertThat(sql).doesNotContain("Liquibase");
		assertThat(sql).doesNotContain("Hibernate");
		assertThat(sql).doesNotContain("JPA");
	}
}
