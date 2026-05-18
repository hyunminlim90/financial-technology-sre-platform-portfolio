package com.fintech.sre.agent.decision.generator;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fintech.sre.agent.decision.DecisionCandidate;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.knowledge.KnowledgeDocumentCandidateMapper;
import com.fintech.sre.agent.knowledge.KnowledgeRetrievalClient;
import com.fintech.sre.agent.knowledge.KnowledgeSearchQuery;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

import reactor.core.publisher.Mono;

@Component
@Profile({"prod", "stage"})
public class KnowledgeBackedCandidateGenerator implements CandidateGenerator {

	private final KnowledgeRetrievalClient knowledgeRetrievalClient;
	private final KnowledgeDocumentCandidateMapper mapper;

	public KnowledgeBackedCandidateGenerator(
			KnowledgeRetrievalClient knowledgeRetrievalClient,
			KnowledgeDocumentCandidateMapper mapper
	) {
		this.knowledgeRetrievalClient = knowledgeRetrievalClient;
		this.mapper = mapper;
	}

	@Override
	public CandidateGenerationSource source() {
		return CandidateGenerationSource.KNOWLEDGE_RETRIEVAL;
	}

	@Override
	public Mono<List<DecisionCandidate>> generate(
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	) {
		KnowledgeSearchQuery query = KnowledgeSearchQuery.from(
				evidenceContext,
				request == null ? null : request.service(),
				resolveDomain(request)
		);

		return knowledgeRetrievalClient.search(query)
				.map(result -> {
					if (result == null || result.isEmpty()) {
						return List.<DecisionCandidate>of();
					}

					return result.documents().stream()
							.map(document -> mapper.toCandidate(
									document,
									request,
									evidenceContext
							))
							.toList();
				});
	}

	private String resolveDomain(IncidentRecommendationRequest request) {
		if (request == null) {
			return null;
		}
		if (request.labels() != null && request.labels().get("domain") != null) {
			return request.labels().get("domain");
		}
		if (request.service() != null && request.service().toLowerCase(Locale.ROOT).contains("payment")) {
			return "payment";
		}
		return "platform";
	}
}
