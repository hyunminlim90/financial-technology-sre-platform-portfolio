package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
public interface RecommendationRecordR2dbcRepository
		extends ReactiveCrudRepository<RecommendationRecordEntity, String> {

	Flux<RecommendationRecordEntity> findByIncidentIdOrderByGeneratedAtDesc(
			String incidentId
	);

	@Query("""
			SELECT * FROM recommendation_records
			ORDER BY generated_at DESC
			LIMIT :limit
			""")
	Flux<RecommendationRecordEntity> findRecent(int limit);
}
