package com.fintech.sre.agent.knowledge.scanner;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class SimpleMarkdownMetadataParser implements MarkdownMetadataParser {

	private final Yaml yaml = new Yaml();

	@Override
	public Map<String, Object> parse(String markdown) {
		if (markdown == null || markdown.isBlank()) {
			return Map.of();
		}

		String normalized = markdown.replace("\r\n", "\n");
		if (!normalized.startsWith("---\n")) {
			return Map.of();
		}

		int closingIndex = normalized.indexOf("\n---\n", 4);
		if (closingIndex < 0) {
			return Map.of();
		}

		String frontmatter = normalized.substring(4, closingIndex);
		Object parsed = yaml.load(frontmatter);
		if (parsed instanceof Map<?, ?> map) {
			return map.entrySet().stream()
					.collect(java.util.stream.Collectors.toMap(
							entry -> String.valueOf(entry.getKey()),
							Map.Entry::getValue
					));
		}

		return Map.of();
	}
}
