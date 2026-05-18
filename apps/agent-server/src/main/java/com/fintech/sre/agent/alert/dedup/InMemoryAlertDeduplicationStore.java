package com.fintech.sre.agent.alert.dedup;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class InMemoryAlertDeduplicationStore implements AlertDeduplicationStore {

	private final AlertDeduplicationProperties properties;
	private final Map<AlertDeduplicationKey, Entry> entries = new ConcurrentHashMap<>();

	public InMemoryAlertDeduplicationStore(AlertDeduplicationProperties properties) {
		this.properties = properties;
	}

	@Override
	public Mono<AlertDeduplicationResult> checkAndRecord(AlertDeduplicationKey key) {
		if (!properties.enabled()) {
			return Mono.just(AlertDeduplicationResult.firstSeen(key, Instant.now()));
		}

		Instant now = Instant.now();

		Entry entry = entries.compute(key, (ignored, existing) -> {
			if (existing == null || existing.expired(now, properties.windowOrDefault())) {
				return new Entry(now, now, new AtomicLong(1));
			}

			existing.lastSeenAt = now;
			existing.count.incrementAndGet();
			return existing;
		});

		boolean duplicate = entry.count.get() > 1;

		if (duplicate) {
			return Mono.just(AlertDeduplicationResult.duplicate(
					key,
					entry.firstSeenAt,
					entry.lastSeenAt,
					entry.count.get()
			));
		}

		return Mono.just(AlertDeduplicationResult.firstSeen(key, now));
	}

	private static final class Entry {
		private final Instant firstSeenAt;
		private volatile Instant lastSeenAt;
		private final AtomicLong count;

		private Entry(
				Instant firstSeenAt,
				Instant lastSeenAt,
				AtomicLong count
		) {
			this.firstSeenAt = firstSeenAt;
			this.lastSeenAt = lastSeenAt;
			this.count = count;
		}

		private boolean expired(Instant now, java.time.Duration window) {
			return firstSeenAt.plus(window).isBefore(now);
		}
	}
}
