package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.application.InMemoryKnowledgeUpdateApplicationStore;
import com.fintech.sre.agent.learning.application.KnowledgeUpdateApplicationStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class KnowledgeUpdateApplicationStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryKnowledgeUpdateApplicationStore.class,
					R2dbcKnowledgeUpdateApplicationStore.class,
					KnowledgeUpdateApplicationEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, KnowledgeUpdateApplicationStore> beans =
					context.getBeansOfType(KnowledgeUpdateApplicationStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcKnowledgeUpdateApplicationStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryKnowledgeUpdateApplicationStore.class,
					R2dbcKnowledgeUpdateApplicationStore.class,
					KnowledgeUpdateApplicationEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, KnowledgeUpdateApplicationStore> beans =
					context.getBeansOfType(KnowledgeUpdateApplicationStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryKnowledgeUpdateApplicationStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		KnowledgeUpdateApplicationR2dbcRepository knowledgeUpdateApplicationR2dbcRepository() {
			return new KnowledgeUpdateApplicationR2dbcRepository() {
				@Override
				public <S extends KnowledgeUpdateApplicationEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends KnowledgeUpdateApplicationEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends KnowledgeUpdateApplicationEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<KnowledgeUpdateApplicationEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<KnowledgeUpdateApplicationEntity> findById(
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
				public Flux<KnowledgeUpdateApplicationEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgeUpdateApplicationEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgeUpdateApplicationEntity> findAllById(
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
				public Mono<Void> delete(KnowledgeUpdateApplicationEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends KnowledgeUpdateApplicationEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends KnowledgeUpdateApplicationEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<KnowledgeUpdateApplicationEntity> findByIncidentIdOrderByAppliedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgeUpdateApplicationEntity> findByLearningCandidateIdOrderByAppliedAtDesc(
						String learningCandidateId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgeUpdateApplicationEntity> findTop500ByOrderByAppliedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
