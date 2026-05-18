package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.learning.promotion.InMemoryKnowledgePromotionReviewStore;
import com.fintech.sre.agent.learning.promotion.KnowledgePromotionReviewStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class KnowledgePromotionReviewStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryKnowledgePromotionReviewStore.class,
					R2dbcKnowledgePromotionReviewStore.class,
					KnowledgePromotionReviewEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, KnowledgePromotionReviewStore> beans =
					context.getBeansOfType(KnowledgePromotionReviewStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcKnowledgePromotionReviewStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryKnowledgePromotionReviewStore.class,
					R2dbcKnowledgePromotionReviewStore.class,
					KnowledgePromotionReviewEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, KnowledgePromotionReviewStore> beans =
					context.getBeansOfType(KnowledgePromotionReviewStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryKnowledgePromotionReviewStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		KnowledgePromotionReviewR2dbcRepository knowledgePromotionReviewR2dbcRepository() {
			return new KnowledgePromotionReviewR2dbcRepository() {
				@Override
				public <S extends KnowledgePromotionReviewEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends KnowledgePromotionReviewEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends KnowledgePromotionReviewEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<KnowledgePromotionReviewEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<KnowledgePromotionReviewEntity> findById(
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
				public Flux<KnowledgePromotionReviewEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgePromotionReviewEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgePromotionReviewEntity> findAllById(
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
				public Mono<Void> delete(KnowledgePromotionReviewEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends KnowledgePromotionReviewEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends KnowledgePromotionReviewEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Mono<KnowledgePromotionReviewEntity> findFirstByLearningCandidateIdOrderByReviewedAtDesc(
						String learningCandidateId
				) {
					return Mono.empty();
				}

				@Override
				public Flux<KnowledgePromotionReviewEntity> findByLearningCandidateIdOrderByReviewedAtDesc(
						String learningCandidateId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgePromotionReviewEntity> findByIncidentIdOrderByReviewedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<KnowledgePromotionReviewEntity> findTop500ByOrderByReviewedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
