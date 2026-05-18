package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.recommendation.persistence.InMemoryRecommendationRecordStore;
import com.fintech.sre.agent.recommendation.persistence.RecommendationRecordStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class RecommendationRecordStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryRecommendationRecordStore.class,
					R2dbcRecommendationRecordStore.class,
					RecommendationRecordEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, RecommendationRecordStore> beans =
					context.getBeansOfType(RecommendationRecordStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcRecommendationRecordStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		RecommendationRecordR2dbcRepository recommendationRecordR2dbcRepository() {
			return new RecommendationRecordR2dbcRepository() {
				@Override
				public <S extends RecommendationRecordEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends RecommendationRecordEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends RecommendationRecordEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<RecommendationRecordEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<RecommendationRecordEntity> findById(
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
				public Flux<RecommendationRecordEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationRecordEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationRecordEntity> findAllById(
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
				public Mono<Void> delete(RecommendationRecordEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(Iterable<? extends RecommendationRecordEntity> entities) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends RecommendationRecordEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<RecommendationRecordEntity> findByIncidentIdOrderByGeneratedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<RecommendationRecordEntity> findRecent(int limit) {
					return Flux.empty();
				}
			};
		}
	}
}
