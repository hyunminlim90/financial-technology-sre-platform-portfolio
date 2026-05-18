package com.fintech.sre.agent.alert.ratelimit;

import java.time.Instant;

public record AlertRateLimitResult(
		boolean allowed,
		AlertRateLimitKey key,
		Instant windowStartedAt,
		Instant checkedAt,
		long used,
		long limit
) {
	public static AlertRateLimitResult allowed(
			AlertRateLimitKey key,
			Instant windowStartedAt,
			Instant checkedAt,
			long used,
			long limit
	) {
		return new AlertRateLimitResult(true, key, windowStartedAt, checkedAt, used, limit);
	}

	public static AlertRateLimitResult limited(
			AlertRateLimitKey key,
			Instant windowStartedAt,
			Instant checkedAt,
			long used,
			long limit
	) {
		return new AlertRateLimitResult(false, key, windowStartedAt, checkedAt, used, limit);
	}
}
