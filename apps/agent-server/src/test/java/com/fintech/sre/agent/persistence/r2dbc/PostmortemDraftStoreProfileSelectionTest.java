package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.postmortem.draft.InMemoryPostmortemDraftStore;
import com.fintech.sre.agent.postmortem.draft.PostmortemDraftStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class PostmortemDraftStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryPostmortemDraftStore.class,
					R2dbcPostmortemDraftStore.class,
					PostmortemDraftEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, PostmortemDraftStore> beans =
					context.getBeansOfType(PostmortemDraftStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcPostmortemDraftStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryPostmortemDraftStore.class,
					R2dbcPostmortemDraftStore.class,
					PostmortemDraftEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, PostmortemDraftStore> beans =
					context.getBeansOfType(PostmortemDraftStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryPostmortemDraftStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		PostmortemDraftR2dbcRepository postmortemDraftR2dbcRepository() {
			return new PostmortemDraftR2dbcRepository() {
				@Override
				public <S extends PostmortemDraftEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends PostmortemDraftEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends PostmortemDraftEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<PostmortemDraftEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<PostmortemDraftEntity> findById(
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
				public Flux<PostmortemDraftEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<PostmortemDraftEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<PostmortemDraftEntity> findAllById(
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
				public Mono<Void> delete(PostmortemDraftEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends PostmortemDraftEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends PostmortemDraftEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Flux<PostmortemDraftEntity> findByIncidentIdOrderByCreatedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<PostmortemDraftEntity> findTop500ByOrderByCreatedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
