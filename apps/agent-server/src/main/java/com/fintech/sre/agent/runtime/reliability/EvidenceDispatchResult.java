package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceDispatchResult(
		EvidenceDispatchStatus status,
		EvidenceDispatchRequest request,
		List<EvidenceQueryResult> results,
		EvidenceDispatchRejectionReason rejectionReason
) {
	public EvidenceDispatchResult {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(request, "request must not be null");
		Objects.requireNonNull(results, "results must not be null");
		results = List.copyOf(results);

		boolean allNormalized = results.stream()
				.allMatch(EvidenceQueryResult::normalizedSemanticEvidenceOnly);
		if (!allNormalized) {
			throw new IllegalArgumentException(
					EvidenceDispatchRejectionReason.NON_NORMALIZED_RESULT.name()
			);
		}
		if (status == EvidenceDispatchStatus.REJECTED && rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected evidence dispatch requires rejection reason"
			);
		}
		if (status != EvidenceDispatchStatus.REJECTED && rejectionReason != null) {
			throw new IllegalArgumentException(
					"accepted evidence dispatch must not contain rejection reason"
			);
		}
	}

	public boolean uncertaintyOnly() {
		return status == EvidenceDispatchStatus.UNCERTAIN
				|| results.stream().allMatch(EvidenceQueryResult::maintainsUncertainty);
	}

	public boolean systemFailure() {
		return false;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
