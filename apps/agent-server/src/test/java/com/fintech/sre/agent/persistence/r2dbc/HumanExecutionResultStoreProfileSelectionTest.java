package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.execution.result.HumanExecutionResultStore;
import com.fintech.sre.agent.recommendation.execution.result.InMemoryHumanExecutionResultStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class HumanExecutionResultStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryHumanExecutionResultStore.class,
					R2dbcHumanExecutionResultStore.class,
					HumanExecutionResultEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, HumanExecutionResultStore> beans =
					context.getBeansOfType(HumanExecutionResultStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcHumanExecutionResultStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryHumanExecutionResultStore.class,
					R2dbcHumanExecutionResultStore.class,
					HumanExecutionResultEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, HumanExecutionResultStore> beans =
					context.getBeansOfType(HumanExecutionResultStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryHumanExecutionResultStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		HumanExecutionResultR2dbcRepository humanExecutionResultR2dbcRepository() {
			return new HumanExecutionResultR2dbcRepository() {
				@Override
				public <S extends HumanExecutionResultEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends HumanExecutionResultEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends HumanExecutionResultEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<HumanExecutionResultEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<HumanExecutionResultEntity> findById(
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
				public Flux<HumanExecutionResultEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<HumanExecutionResultEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<HumanExecutionResultEntity> findAllById(
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
				public Mono<Void> delete(HumanExecutionResultEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends HumanExecutionResultEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends HumanExecutionResultEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<HumanExecutionResultEntity> findByExecutionPlanIdOrderByRecordedAtDesc(
						String executionPlanId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<HumanExecutionResultEntity> findByRecommendationRecordIdOrderByRecordedAtDesc(
						String recommendationRecordId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<HumanExecutionResultEntity> findByIncidentIdOrderByRecordedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<HumanExecutionResultEntity> findTop500ByOrderByRecordedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
