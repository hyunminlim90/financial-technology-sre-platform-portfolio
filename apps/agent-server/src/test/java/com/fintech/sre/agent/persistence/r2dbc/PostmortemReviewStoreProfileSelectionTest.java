package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.postmortem.review.InMemoryPostmortemReviewStore;
import com.fintech.sre.agent.postmortem.review.PostmortemReviewStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class PostmortemReviewStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryPostmortemReviewStore.class,
					R2dbcPostmortemReviewStore.class,
					PostmortemReviewEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, PostmortemReviewStore> beans =
					context.getBeansOfType(PostmortemReviewStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcPostmortemReviewStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryPostmortemReviewStore.class,
					R2dbcPostmortemReviewStore.class,
					PostmortemReviewEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, PostmortemReviewStore> beans =
					context.getBeansOfType(PostmortemReviewStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryPostmortemReviewStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		PostmortemReviewR2dbcRepository postmortemReviewR2dbcRepository() {
			return new PostmortemReviewR2dbcRepository() {
				@Override
				public <S extends PostmortemReviewEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends PostmortemReviewEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends PostmortemReviewEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<PostmortemReviewEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<PostmortemReviewEntity> findById(
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
				public Flux<PostmortemReviewEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<PostmortemReviewEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<PostmortemReviewEntity> findAllById(
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
				public Mono<Void> delete(PostmortemReviewEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends PostmortemReviewEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends PostmortemReviewEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<PostmortemReviewEntity> findByPostmortemDraftIdOrderByReviewedAtDesc(
						String postmortemDraftId
				) {
					return Flux.empty();
				}

				@Override
				public Mono<PostmortemReviewEntity> findFirstByPostmortemDraftIdOrderByReviewedAtDesc(
						String postmortemDraftId
				) {
					return Mono.empty();
				}

				@Override
				public Flux<PostmortemReviewEntity> findByIncidentIdOrderByReviewedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<PostmortemReviewEntity> findTop500ByOrderByReviewedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
