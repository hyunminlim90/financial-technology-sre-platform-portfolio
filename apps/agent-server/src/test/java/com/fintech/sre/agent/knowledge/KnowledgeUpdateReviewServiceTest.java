package com.fintech.sre.agent.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fintech.sre.agent.incident.InMemoryIncidentLifecycleRepository;
import com.fintech.sre.agent.incident.IncidentLifecycleService;
import com.fintech.sre.agent.improvement.ImprovementCandidate;
import com.fintech.sre.agent.improvement.ImprovementCandidateRepository;
import com.fintech.sre.agent.improvement.ImprovementCandidateStatus;
import com.fintech.sre.agent.improvement.ImprovementCandidateType;
import com.fintech.sre.agent.improvement.InMemoryImprovementCandidateRepository;

class KnowledgeUpdateReviewServiceTest {

	@Test
	void shouldCreateReviewFromAcceptedImprovementCandidate() {
		KnowledgeUpdateReviewRepository repository = new InMemoryKnowledgeUpdateReviewRepository();
		ImprovementCandidateRepository improvementCandidateRepository = new InMemoryImprovementCandidateRepository();
		KnowledgeUpdateReviewService service = new KnowledgeUpdateReviewService(
				repository,
				improvementCandidateRepository,
				new IncidentLifecycleService(new InMemoryIncidentLifecycleRepository())
		);

		improvementCandidateRepository.save(new ImprovementCandidate(
				"candidate-1",
				"INC-KNOWLEDGE-1",
				"action-log-1",
				ImprovementCandidateType.RUNBOOK_UPDATE,
				ImprovementCandidateStatus.ACCEPTED_BY_HUMAN,
				"Runbook update candidate",
				"Runbook should be updated after review",
				"runbooks/payment-timeout.md",
				List.of("Outcome: ROLLED_BACK", "Target Domain: payment"),
				"Accepted for review queue",
				Instant.now(),
				Instant.now()
		)).block();

		List<KnowledgeUpdateReview> reviews = service.createFromAcceptedImprovementCandidate("candidate-1")
				.collectList()
				.block();

		assertThat(reviews).hasSize(1);
		assertThat(reviews.get(0).type()).isEqualTo(KnowledgeUpdateType.RUNBOOK);
		assertThat(reviews.get(0).status()).isEqualTo(KnowledgeUpdateStatus.REQUESTED);
		assertThat(reviews.get(0).targetKnowledgePath()).isEqualTo("runbooks/payment-timeout.md");
		assertThat(reviews.get(0).proposedContentSummary()).contains("Human review required");
	}
}
