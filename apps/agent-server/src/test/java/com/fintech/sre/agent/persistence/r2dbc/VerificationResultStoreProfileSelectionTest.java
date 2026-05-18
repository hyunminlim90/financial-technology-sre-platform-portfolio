package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.verification.InMemoryVerificationResultStore;
import com.fintech.sre.agent.recommendation.verification.VerificationResultStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class VerificationResultStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryVerificationResultStore.class,
					R2dbcVerificationResultStore.class,
					VerificationResultEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, VerificationResultStore> beans =
					context.getBeansOfType(VerificationResultStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcVerificationResultStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryVerificationResultStore.class,
					R2dbcVerificationResultStore.class,
					VerificationResultEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, VerificationResultStore> beans =
					context.getBeansOfType(VerificationResultStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryVerificationResultStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		VerificationResultR2dbcRepository verificationResultR2dbcRepository() {
			return new VerificationResultR2dbcRepository() {
				@Override
				public <S extends VerificationResultEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends VerificationResultEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends VerificationResultEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<VerificationResultEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<VerificationResultEntity> findById(
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
				public Flux<VerificationResultEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<VerificationResultEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<VerificationResultEntity> findAllById(
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
				public Mono<Void> delete(VerificationResultEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends VerificationResultEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends VerificationResultEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<VerificationResultEntity> findByExecutionResultIdOrderByVerifiedAtDesc(
						String executionResultId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<VerificationResultEntity> findByRecommendationRecordIdOrderByVerifiedAtDesc(
						String recommendationRecordId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<VerificationResultEntity> findByIncidentIdOrderByVerifiedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<VerificationResultEntity> findTop500ByOrderByVerifiedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
