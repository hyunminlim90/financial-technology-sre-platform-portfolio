package com.fintech.sre.agent.knowledge.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.fintech.sre.agent.knowledge.KnowledgeDocumentType;

import reactor.core.publisher.Flux;

@Component
public class FileSystemKnowledgeSourceScanner implements KnowledgeSourceScanner {

	private final MarkdownMetadataParser markdownMetadataParser;
	private final Yaml yaml = new Yaml();

	public FileSystemKnowledgeSourceScanner(MarkdownMetadataParser markdownMetadataParser) {
		this.markdownMetadataParser = markdownMetadataParser;
	}

	@Override
	public Flux<RawKnowledgeSource> scan(Path rootPath) {
		return Flux.defer(() -> {
			if (rootPath == null || !Files.exists(rootPath)) {
				return Flux.empty();
			}

			try {
				Stream<Path> stream = Files.walk(rootPath);
				return Flux.fromStream(stream)
						.filter(Files::isRegularFile)
						.filter(this::isSupportedFile)
						.flatMap(path -> toRawSource(rootPath, path));
			} catch (IOException exception) {
				return Flux.error(new IllegalStateException("Failed to scan knowledge sources: " + rootPath, exception));
			}
		});
	}

	private Flux<RawKnowledgeSource> toRawSource(Path rootPath, Path path) {
		try {
			String content = Files.readString(path);
			String relativePath = rootPath.relativize(path).toString().replace('\\', '/');
			KnowledgeDocumentType type = inferType(relativePath);
			if (type == null) {
				return Flux.empty();
			}

			return Flux.just(new RawKnowledgeSource(
					relativePath,
					path,
					relativePath,
					content,
					parseMetadata(path, content),
					type
			));
		} catch (IOException exception) {
			return Flux.error(new IllegalStateException("Failed to read knowledge file: " + path, exception));
		}
	}

	private Map<String, Object> parseMetadata(Path path, String content) {
		String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
		if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
			return markdownMetadataParser.parse(content);
		}
		if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
			Object parsed = yaml.load(content);
			if (parsed instanceof Map<?, ?> map) {
				return map.entrySet().stream()
						.collect(java.util.stream.Collectors.toMap(
								entry -> String.valueOf(entry.getKey()),
								Map.Entry::getValue
						));
			}
		}
		return Map.of();
	}

	private boolean isSupportedFile(Path path) {
		String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
		return fileName.endsWith(".md")
				|| fileName.endsWith(".markdown")
				|| fileName.endsWith(".yaml")
				|| fileName.endsWith(".yml");
	}

	private KnowledgeDocumentType inferType(String relativePath) {
		String normalized = relativePath.toLowerCase(Locale.ROOT);
		if (normalized.startsWith("scenarios/")) {
			return KnowledgeDocumentType.SCENARIO;
		}
		if (normalized.startsWith("runbooks/")) {
			return KnowledgeDocumentType.RUNBOOK;
		}
		if (normalized.startsWith("postmortems/")) {
			return KnowledgeDocumentType.POSTMORTEM;
		}
		if (normalized.startsWith("improvements/")) {
			return KnowledgeDocumentType.IMPROVEMENT;
		}
		if (normalized.startsWith("preventive-designs/")) {
			return KnowledgeDocumentType.PREVENTIVE_DESIGN;
		}
		if (normalized.startsWith("policies/")) {
			return KnowledgeDocumentType.POLICY;
		}
		if (normalized.startsWith("protocols/")) {
			return KnowledgeDocumentType.PROTOCOL;
		}
		if (normalized.startsWith("rag/docs/")) {
			return KnowledgeDocumentType.RAG_DOC;
		}
		return null;
	}
}
