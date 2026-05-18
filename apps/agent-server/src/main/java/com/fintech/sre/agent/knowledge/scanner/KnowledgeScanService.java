package com.fintech.sre.agent.knowledge.scanner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.ingestion.KnowledgeIngestionDocument;
import com.fintech.sre.agent.knowledge.ingestion.KnowledgeIngestionValidationResult;
import com.fintech.sre.agent.knowledge.ingestion.KnowledgeIngestionValidator;

import reactor.core.publisher.Mono;

@Component
public class KnowledgeScanService {

	private final KnowledgeSourceScanner scanner;
	private final KnowledgeDocumentFactory documentFactory;
	private final KnowledgeIngestionValidator validator;

	public KnowledgeScanService(
			KnowledgeSourceScanner scanner,
			KnowledgeDocumentFactory documentFactory,
			KnowledgeIngestionValidator validator
	) {
		this.scanner = scanner;
		this.documentFactory = documentFactory;
		this.validator = validator;
	}

	public Mono<KnowledgeScanResult> scan(Path rootPath) {
		return scanner.scan(rootPath)
				.collectList()
				.map(this::toScanResult);
	}

	private KnowledgeScanResult toScanResult(List<RawKnowledgeSource> rawSources) {
		List<KnowledgeIngestionDocument> validDocuments = new ArrayList<>();
		List<RejectedKnowledgeDocument> rejectedDocuments = new ArrayList<>();

		for (RawKnowledgeSource source : rawSources) {
			KnowledgeIngestionDocument document = documentFactory.create(source);
			KnowledgeIngestionValidationResult validation = validator.validate(document);

			if (validation.valid()) {
				validDocuments.add(document);
				continue;
			}

			rejectedDocuments.add(new RejectedKnowledgeDocument(source, validation.errors()));
		}

		return new KnowledgeScanResult(validDocuments, rejectedDocuments);
	}
}
