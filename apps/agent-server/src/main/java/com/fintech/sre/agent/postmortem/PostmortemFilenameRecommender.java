package com.fintech.sre.agent.postmortem;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class PostmortemFilenameRecommender {

	public String recommend(PostmortemGenerationInput input) {
		String failureMode = !input.context().actionLogSnapshot().recommendations().isEmpty()
				&& input.context().actionLogSnapshot().recommendations().get(0).failureMode() != null
				? input.context().actionLogSnapshot().recommendations().get(0).failureMode()
				: normalize(input.context().incidentContext().alertName());

		String date = DateTimeFormatter.ISO_LOCAL_DATE
				.withZone(ZoneOffset.UTC)
				.format(input.context().incidentContext().occurredAt());

		return "postmortems/%s-%s.md".formatted(failureMode, date);
	}

	private String normalize(String value) {
		return value.toLowerCase()
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-|-$", "");
	}
}
