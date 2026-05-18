package com.fintech.sre.agent.postmortem.review;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class PostmortemReviewIdGenerator {

	public String generate() {
		return "postmortem-review-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
