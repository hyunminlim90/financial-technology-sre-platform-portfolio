package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceDispatchExecutionResponse(
		EvidenceDispatchExecutionStatus status,
		EvidenceDispatchExecutionRequest request,
		List<EvidenceQueryResult> results,
		EvidenceDispatchExecutionRejectionReason rejectionReason
) {
	public EvidenceDispatchExecutionResponse {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(request, "request must not be null");
		Objects.requireNonNull(results, "results must not be null");
		results = List.copyOf(results);

		boolean allNormalized = results.stream()
				.allMatch(EvidenceQueryResult::normalizedSemanticEvidenceOnly);
		if (!allNormalized) {
			throw new IllegalArgumentException(
					EvidenceDispatchExecutionRejectionReason
							.NON_NORMALIZED_RESULT.name()
			);
		}
		if (status == EvidenceDispatchExecutionStatus.REJECTED
				&& rejectionReason == null) {
			throw new IllegalArgumentException(
					"rejected evidence dispatch execution requires rejection reason"
			);
		}
		if (status != EvidenceDispatchExecutionStatus.REJECTED
				&& rejectionReason != null) {
			throw new IllegalArgumentException(
					"accepted evidence dispatch execution must not contain rejection reason"
			);
		}
	}

	public boolean systemFailure() {
		return false;
	}

	public boolean uncertaintyOnly() {
		return status == EvidenceDispatchExecutionStatus.UNCERTAIN
				|| results.stream().allMatch(EvidenceQueryResult::maintainsUncertainty);
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean actionExecutionAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
