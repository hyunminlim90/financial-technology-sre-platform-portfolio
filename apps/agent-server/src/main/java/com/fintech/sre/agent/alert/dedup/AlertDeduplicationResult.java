package com.fintech.sre.agent.alert.dedup;

import java.time.Instant;

public record AlertDeduplicationResult(
		boolean duplicate,
		AlertDeduplicationKey key,
		Instant firstSeenAt,
		Instant lastSeenAt,
		long occurrenceCount
) {
	public static AlertDeduplicationResult firstSeen(
			AlertDeduplicationKey key,
			Instant now
	) {
		return new AlertDeduplicationResult(false, key, now, now, 1);
	}

	public static AlertDeduplicationResult duplicate(
			AlertDeduplicationKey key,
			Instant firstSeenAt,
			Instant now,
			long occurrenceCount
	) {
		return new AlertDeduplicationResult(true, key, firstSeenAt, now, occurrenceCount);
	}
}
