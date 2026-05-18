package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.approval.InMemoryRecommendationApprovalStore;
import com.fintech.sre.agent.recommendation.approval.RecommendationApprovalStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class RecommendationApprovalStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryRecommendationApprovalStore.class,
					R2dbcRecommendationApprovalStore.class,
					RecommendationApprovalRecordEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, RecommendationApprovalStore> beans =
					context.getBeansOfType(RecommendationApprovalStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcRecommendationApprovalStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryRecommendationApprovalStore.class,
					R2dbcRecommendationApprovalStore.class,
					RecommendationApprovalRecordEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, RecommendationApprovalStore> beans =
					context.getBeansOfType(RecommendationApprovalStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryRecommendationApprovalStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		RecommendationApprovalRecordR2dbcRepository recommendationApprovalRecordR2dbcRepository() {
			return new RecommendationApprovalRecordR2dbcRepository() {
				@Override
				public <S extends RecommendationApprovalRecordEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends RecommendationApprovalRecordEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends RecommendationApprovalRecordEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<RecommendationApprovalRecordEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<RecommendationApprovalRecordEntity> findById(
						org.reactivestreams.Publisher<String> idPublisher
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Boolean> existsById(String s) {
					return Mono.just(false);
				}

				@Override
				public Mono<Boolean> existsById(
						org.reactivestreams.Publisher<String> idPublisher
				) {
					return Mono.just(false);
				}

				@Override
				public Flux<RecommendationApprovalRecordEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationApprovalRecordEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationApprovalRecordEntity> findAllById(
						org.reactivestreams.Publisher<String> idStream
				) {
					return Flux.empty();
				}

				@Override
				public Mono<Long> count() {
					return Mono.just(0L);
				}

				@Override
				public Mono<Void> deleteById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteById(
						org.reactivestreams.Publisher<String> idPublisher
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> delete(RecommendationApprovalRecordEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends RecommendationApprovalRecordEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends RecommendationApprovalRecordEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<RecommendationApprovalRecordEntity> findByIncidentIdOrderByDecidedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationApprovalRecordEntity> findByRecommendationRecordIdOrderByDecidedAtDesc(
						String recommendationRecordId
				) {
					return Flux.empty();
				}

				@Override
				public Mono<RecommendationApprovalRecordEntity> findFirstByRecommendationRecordIdOrderByDecidedAtDesc(
						String recommendationRecordId
				) {
					return Mono.empty();
				}

				@Override
				public Flux<RecommendationApprovalRecordEntity> findTop500ByOrderByDecidedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
