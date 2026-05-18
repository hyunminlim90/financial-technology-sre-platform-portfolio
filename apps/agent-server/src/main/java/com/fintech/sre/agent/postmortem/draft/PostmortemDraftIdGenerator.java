package com.fintech.sre.agent.postmortem.draft;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class PostmortemDraftIdGenerator {

	public String generate() {
		return "postmortem-draft-"
				+ Instant.now().toString().replace(":", "-")
				+ "-"
				+ UUID.randomUUID();
	}
}
