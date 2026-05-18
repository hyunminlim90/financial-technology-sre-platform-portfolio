package com.fintech.sre.agent.persistence.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.incident.lifecycle.InMemoryIncidentLifecycleStore;
import com.fintech.sre.agent.incident.lifecycle.IncidentLifecycleStore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class IncidentLifecycleStoreProfileSelectionTest {

	@Test
	void shouldSelectR2dbcStoreWhenR2dbcProfileIsActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("r2dbc");
			context.register(
					InMemoryIncidentLifecycleStore.class,
					R2dbcIncidentLifecycleStore.class,
					IncidentLifecycleEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, IncidentLifecycleStore> beans =
					context.getBeansOfType(IncidentLifecycleStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(R2dbcIncidentLifecycleStore.class);
		}
	}

	@Test
	void shouldSelectInMemoryStoreWhenR2dbcProfileIsNotActive() {
		try (AnnotationConfigApplicationContext context =
				     new AnnotationConfigApplicationContext()) {
			context.register(
					InMemoryIncidentLifecycleStore.class,
					R2dbcIncidentLifecycleStore.class,
					IncidentLifecycleEntityMapper.class,
					TestConfig.class
			);
			context.refresh();

			Map<String, IncidentLifecycleStore> beans =
					context.getBeansOfType(IncidentLifecycleStore.class);

			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next())
					.isInstanceOf(InMemoryIncidentLifecycleStore.class);
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		IncidentLifecycleR2dbcRepository incidentLifecycleR2dbcRepository() {
			return new IncidentLifecycleR2dbcRepository() {
				@Override
				public <S extends IncidentLifecycleEntity> Mono<S> save(S entity) {
					return Mono.just(entity);
				}

				@Override
				public <S extends IncidentLifecycleEntity> Flux<S> saveAll(
						Iterable<S> entities
				) {
					return Flux.fromIterable(entities);
				}

				@Override
				public <S extends IncidentLifecycleEntity> Flux<S> saveAll(
						org.reactivestreams.Publisher<S> entityStream
				) {
					return Flux.from(entityStream);
				}

				@Override
				public Mono<IncidentLifecycleEntity> findById(String s) {
					return Mono.empty();
				}

				@Override
				public Mono<IncidentLifecycleEntity> findById(
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
				public Flux<IncidentLifecycleEntity> findAll() {
					return Flux.empty();
				}

				@Override
				public Flux<IncidentLifecycleEntity> findAllById(
						Iterable<String> strings
				) {
					return Flux.empty();
				}

				@Override
				public Flux<IncidentLifecycleEntity> findAllById(
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
				public Mono<Void> delete(IncidentLifecycleEntity entity) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						Iterable<? extends IncidentLifecycleEntity> entities
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll(
						org.reactivestreams.Publisher<? extends IncidentLifecycleEntity> entityStream
				) {
					return Mono.empty();
				}

				@Override
				public Mono<Void> deleteAll() {
					return Mono.empty();
				}

				@Override
				public Mono<IncidentLifecycleEntity> findFirstByIncidentIdOrderByTransitionedAtDesc(
						String incidentId
				) {
					return Mono.empty();
				}

				@Override
				public Flux<IncidentLifecycleEntity> findByIncidentIdOrderByTransitionedAtDesc(
						String incidentId
				) {
					return Flux.empty();
				}

				@Override
				public Flux<IncidentLifecycleEntity> findTop500ByOrderByTransitionedAtDesc() {
					return Flux.empty();
				}
			};
		}
	}
}
