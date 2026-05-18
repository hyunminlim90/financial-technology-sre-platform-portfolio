package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.plan.InMemoryKnowledgePromotionPlanStore;
import com.fintech.sre.agent.learning.plan.KnowledgePromotionPlanStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class KnowledgePromotionPlanStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryKnowledgePromotionPlanStore.class,
					R2dbcKnowledgePromotionPlanStore.class,
					KnowledgePromotionPlanEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, KnowledgePromotionPlanStore> beans =
					context.getBeansOfType(KnowledgePromotionPlanStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcKnowledgePromotionPlanStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryKnowledgePromotionPlanStore.class,
					R2dbcKnowledgePromotionPlanStore.class,
					KnowledgePromotionPlanEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, KnowledgePromotionPlanStore> beans =
					context.getBeansOfType(KnowledgePromotionPlanStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryKnowledgePromotionPlanStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		KnowledgePromotionPlanR2dbcRepository knowledgePromotionPlanR2dbcRepository() {
			return new KnowledgePromotionPlanR2dbcRepository() {
				@Override
				public <S extends KnowledgePromotionPlanEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends KnowledgePromotionPlanEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends KnowledgePromotionPlanEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<KnowledgePromotionPlanEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<KnowledgePromotionPlanEntity> findById(
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
				public Flux<KnowledgePromotionPlanEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgePromotionPlanEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgePromotionPlanEntity> findAllById(
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
				public Mono<Void> delete(KnowledgePromotionPlanEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends KnowledgePromotionPlanEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends KnowledgePromotionPlanEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<KnowledgePromotionPlanEntity> findByLearningCandidateIdOrderByCreatedAtDesc(
						String learningCandidateId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgePromotionPlanEntity> findByIncidentIdOrderByCreatedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgePromotionPlanEntity> findTop500ByOrderByCreatedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
