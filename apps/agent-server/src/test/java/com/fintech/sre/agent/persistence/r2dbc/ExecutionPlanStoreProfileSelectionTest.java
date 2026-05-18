package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.execution.ExecutionPlanStore;
import com.fintech.sre.agent.recommendation.execution.InMemoryExecutionPlanStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ExecutionPlanStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryExecutionPlanStore.class,
					R2dbcExecutionPlanStore.class,
					RecommendationExecutionPlanEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, ExecutionPlanStore> beans =
					context.getBeansOfType(ExecutionPlanStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcExecutionPlanStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryExecutionPlanStore.class,
					R2dbcExecutionPlanStore.class,
					RecommendationExecutionPlanEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, ExecutionPlanStore> beans =
					context.getBeansOfType(ExecutionPlanStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryExecutionPlanStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		RecommendationExecutionPlanR2dbcRepository recommendationExecutionPlanR2dbcRepository() {
			return new RecommendationExecutionPlanR2dbcRepository() {
				@Override
				public <S extends RecommendationExecutionPlanEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends RecommendationExecutionPlanEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends RecommendationExecutionPlanEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<RecommendationExecutionPlanEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<RecommendationExecutionPlanEntity> findById(
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
				public Flux<RecommendationExecutionPlanEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationExecutionPlanEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationExecutionPlanEntity> findAllById(
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
				public Mono<Void> delete(RecommendationExecutionPlanEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends RecommendationExecutionPlanEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends RecommendationExecutionPlanEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<RecommendationExecutionPlanEntity> findByRecommendationRecordIdOrderByCreatedAtDesc(
						String recommendationRecordId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationExecutionPlanEntity> findByIncidentIdOrderByCreatedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationExecutionPlanEntity> findTop500ByOrderByCreatedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
