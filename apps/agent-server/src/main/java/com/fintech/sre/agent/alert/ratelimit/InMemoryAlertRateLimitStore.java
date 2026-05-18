package com.fintech.sre.agent.alert.ratelimit;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class InMemoryAlertRateLimitStore implements AlertRateLimitStore {

	private final AlertRateLimitProperties properties;
	private final Map<AlertRateLimitKey, Window> windows = new ConcurrentHashMap<>();

	public InMemoryAlertRateLimitStore(AlertRateLimitProperties properties) {
		this.properties = properties;
	}

	@Override
	public Mono<AlertRateLimitResult> checkAndConsume(AlertRateLimitKey key) {
		if (!properties.enabled()) {
			Instant now = Instant.now();
			return Mono.just(AlertRateLimitResult.allowed(
					key,
					now,
					now,
					0,
					properties.maxRecommendationsPerWindowOrDefault()
			));
		}

		Instant now = Instant.now();
		long limit = properties.maxRecommendationsPerWindowOrDefault();

		Window window = windows.compute(key, (ignored, existing) -> {
			if (existing == null || existing.expired(now, properties.windowOrDefault())) {
				return new Window(now, new AtomicLong(1));
			}

			existing.used.incrementAndGet();
			return existing;
		});

		long used = window.used.get();

		if (used > limit) {
			return Mono.just(AlertRateLimitResult.limited(
					key,
					window.startedAt,
					now,
					used,
					limit
			));
		}

		return Mono.just(AlertRateLimitResult.allowed(
				key,
				window.startedAt,
				now,
				used,
				limit
		));
	}

	private static final class Window {
		private final Instant startedAt;
		private final AtomicLong used;

		private Window(Instant startedAt, AtomicLong used) {
			this.startedAt = startedAt;
			this.used = used;
		}

		private boolean expired(Instant now, java.time.Duration duration) {
			return startedAt.plus(duration).isBefore(now);
		}
	}
}
