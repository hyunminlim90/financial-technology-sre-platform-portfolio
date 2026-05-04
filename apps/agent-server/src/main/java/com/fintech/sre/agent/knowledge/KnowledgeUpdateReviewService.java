package com.fintech.sre.agent.knowledge;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fintech.sre.agent.incident.IncidentLifecycleService;
import com.fintech.sre.agent.incident.IncidentStatus;
import com.fintech.sre.agent.improvement.ImprovementCandidate;
import com.fintech.sre.agent.improvement.ImprovementCandidateRepository;
import com.fintech.sre.agent.improvement.ImprovementCandidateType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class KnowledgeUpdateReviewService {

	private final KnowledgeUpdateReviewRepository repository;
	private final ImprovementCandidateRepository improvementCandidateRepository;
	private final IncidentLifecycleService incidentLifecycleService;

	public KnowledgeUpdateReviewService(
			KnowledgeUpdateReviewRepository repository,
			ImprovementCandidateRepository improvementCandidateRepository,
			IncidentLifecycleService incidentLifecycleService
	) {
		this.repository = repository;
		this.improvementCandidateRepository = improvementCandidateRepository;
		this.incidentLifecycleService = incidentLifecycleService;
	}

	public Mono<KnowledgeUpdateReview> create(KnowledgeUpdateCreateRequest request) {
		Instant now = Instant.now();

		KnowledgeUpdateReview review = new KnowledgeUpdateReview(
				UUID.randomUUID().toString(),
				request.incidentId(),
				request.improvementCandidateId(),
				request.type(),
				KnowledgeUpdateStatus.REQUESTED,
				request.targetKnowledgePath(),
				request.title(),
				request.reason(),
				request.evidence() == null ? List.of() : request.evidence(),
				request.proposedContentSummary(),
				null,
				now,
				now
		);

		return repository.save(review)
				.flatMap(saved -> incidentLifecycleService.advanceTo(
								saved.incidentId(),
								IncidentStatus.KNOWLEDGE_REVIEW_REQUESTED,
								"knowledge update review created"
						)
						.thenReturn(saved));
	}

	public Flux<KnowledgeUpdateReview> createFromAcceptedImprovementCandidate(String candidateId) {
		return improvementCandidateRepository.findById(candidateId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("ImprovementCandidate not found: " + candidateId)))
				.flatMapMany(candidate -> Flux.fromIterable(toReviews(candidate)))
				.flatMap(repository::save)
				.collectList()
				.flatMapMany(reviews -> {
					if (reviews.isEmpty()) {
						return Flux.fromIterable(reviews);
					}
					return incidentLifecycleService.advanceTo(
									reviews.get(0).incidentId(),
									IncidentStatus.KNOWLEDGE_REVIEW_REQUESTED,
									"knowledge update reviews created from accepted improvement candidate"
							)
							.thenMany(Flux.fromIterable(reviews));
				});
	}

	public Flux<KnowledgeUpdateReview> findByIncidentId(String incidentId) {
		return repository.findByIncidentId(incidentId);
	}

	public Flux<KnowledgeUpdateReview> findRequested() {
		return repository.findByStatus(KnowledgeUpdateStatus.REQUESTED);
	}

	public Mono<KnowledgeUpdateReview> approve(String reviewId, String reason) {
		return repository.findById(reviewId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("KnowledgeUpdateReview not found: " + reviewId)))
				.flatMap(review -> repository.save(review.approve(reason)));
	}

	public Mono<KnowledgeUpdateReview> reject(String reviewId, String reason) {
		return repository.findById(reviewId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("KnowledgeUpdateReview not found: " + reviewId)))
				.flatMap(review -> repository.save(review.reject(reason)));
	}

	public Mono<KnowledgeUpdateReview> markAppliedExternally(String reviewId, String reason) {
		return repository.findById(reviewId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("KnowledgeUpdateReview not found: " + reviewId)))
				.flatMap(review -> repository.save(review.markAppliedExternally(reason)));
	}

	public Mono<KnowledgeUpdateReview> cancel(String reviewId, String reason) {
		return repository.findById(reviewId)
				.switchIfEmpty(Mono.error(new IllegalArgumentException("KnowledgeUpdateReview not found: " + reviewId)))
				.flatMap(review -> repository.save(review.cancel(reason)));
	}

	private List<KnowledgeUpdateReview> toReviews(ImprovementCandidate candidate) {
		Instant now = Instant.now();

		return List.of(new KnowledgeUpdateReview(
				UUID.randomUUID().toString(),
				candidate.incidentId(),
				candidate.id(),
				mapType(candidate.type()),
				KnowledgeUpdateStatus.REQUESTED,
				candidate.targetKnowledgePath(),
				candidate.title(),
				candidate.reason(),
				candidate.evidence(),
				buildProposedContentSummary(candidate),
				null,
				now,
				now
		));
	}

	private KnowledgeUpdateType mapType(ImprovementCandidateType type) {
		return switch (type) {
			case RUNBOOK_UPDATE -> KnowledgeUpdateType.RUNBOOK;
			case SCENARIO_UPDATE -> KnowledgeUpdateType.SCENARIO;
			case PREVENTIVE_DESIGN_REQUIRED -> KnowledgeUpdateType.PREVENTIVE_DESIGN;
			case POLICY_REQUIRED -> KnowledgeUpdateType.POLICY;
			case RAG_DOC_UPDATE -> KnowledgeUpdateType.RAG_DOC;
			case GUARDRAIL_RULE_REQUIRED -> KnowledgeUpdateType.IMPROVEMENT;
		};
	}

	private String buildProposedContentSummary(ImprovementCandidate candidate) {
		return """
				Human review required before modifying portfolio repo.

				Candidate Type: %s
				Target Path: %s

				Reason:
				%s

				Evidence:
				%s

				Required Checks:
				- source scenario/runbook/postmortem consistency
				- payment idempotency / duplicate payment impact
				- rollback and verification requirements
				- knowledge layering rule
				""".formatted(
				candidate.type(),
				candidate.targetKnowledgePath(),
				candidate.reason(),
				String.join("\n", candidate.evidence() == null ? List.of() : candidate.evidence())
		);
	}
}
