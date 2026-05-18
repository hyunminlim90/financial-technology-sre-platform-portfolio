package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.candidate.InMemoryLearningCandidateStore;
import com.fintech.sre.agent.learning.candidate.LearningCandidateStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class LearningCandidateStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryLearningCandidateStore.class,
					R2dbcLearningCandidateStore.class,
					LearningCandidateEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, LearningCandidateStore> beans =
					context.getBeansOfType(LearningCandidateStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcLearningCandidateStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryLearningCandidateStore.class,
					R2dbcLearningCandidateStore.class,
					LearningCandidateEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, LearningCandidateStore> beans =
					context.getBeansOfType(LearningCandidateStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryLearningCandidateStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		LearningCandidateR2dbcRepository learningCandidateR2dbcRepository() {
			return new LearningCandidateR2dbcRepository() {
				@Override
				public <S extends LearningCandidateEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends LearningCandidateEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends LearningCandidateEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<LearningCandidateEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<LearningCandidateEntity> findById(
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
				public Flux<LearningCandidateEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<LearningCandidateEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<LearningCandidateEntity> findAllById(
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
				public Mono<Void> delete(LearningCandidateEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends LearningCandidateEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends LearningCandidateEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<LearningCandidateEntity> findByIncidentIdOrderByCreatedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<LearningCandidateEntity> findTop500ByOrderByCreatedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
