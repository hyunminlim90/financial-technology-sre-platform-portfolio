package com.fintech.sre.agent.knowledge.scanner;

import java.nio.file.Path;

import reactor.core.publisher.Flux;

public interface KnowledgeSourceScanner {

	Flux<RawKnowledgeSource> scan(Path rootPath);
}
