package com.fintech.sre.agent.knowledge.layering;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fintech.sre.agent.knowledge.rag.KnowledgeContext;
import com.fintech.sre.agent.knowledge.rag.KnowledgeDocument;

import reactor.core.publisher.Mono;

@Component
public class KnowledgeLayeringValidator {

	private final KnowledgeLayeringPolicy policy;
	private final KnowledgePriorityResolver priorityResolver = new KnowledgePriorityResolver();

	public KnowledgeLayeringValidator(KnowledgeLayeringPolicy policy) {
		this.policy = policy;
	}

	public Mono<ValidatedKnowledgeContext> validate(KnowledgeContext context) {
		KnowledgeLayeringValidationResult result = policy.validate(context);

		if (!result.valid()) {
			return Mono.error(new KnowledgeLayeringException(result.issues()));
		}

		List<KnowledgeDocument> priorityDocuments = priorityResolver.resolvePriority(context);

		return Mono.just(new ValidatedKnowledgeContext(
				context,
				priorityDocuments,
				result.issues()
		));
	}

	public record ValidatedKnowledgeContext(
			KnowledgeContext context,
			List<KnowledgeDocument> priorityDocuments,
			List<KnowledgeLayeringIssue> issues
	) {
	}
}
