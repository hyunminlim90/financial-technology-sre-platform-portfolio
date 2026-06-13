package com.fintech.sre.agent.runtime.reliability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record EvidenceLineage(
		List<EvidenceLineageNode> nodes,
		List<EvidenceLineageEdge> edges,
		EvidenceLineageStatus status,
		EvidenceLineageReason reason,
		EvidenceGovernancePolicy governancePolicy,
		OperationalUncertainty riskLevel
) {
	public EvidenceLineage {
		Objects.requireNonNull(nodes, "nodes must not be null");
		Objects.requireNonNull(edges, "edges must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(
				governancePolicy,
				"governancePolicy must not be null"
		);
		Objects.requireNonNull(riskLevel, "riskLevel must not be null");
		nodes = List.copyOf(nodes);
		edges = List.copyOf(edges);
	}

	public static EvidenceLineage trace(
			EvidenceGovernancePolicy governancePolicy,
			boolean collectionStagePresent,
			boolean assessmentStagePresent
	) {
		Objects.requireNonNull(
				governancePolicy,
				"governancePolicy must not be null"
		);

		List<EvidenceLineageNode> nodes = new ArrayList<>();
		nodes.add(EvidenceLineageNode.SOURCE);
		nodes.add(EvidenceLineageNode.ADAPTER);
		nodes.add(EvidenceLineageNode.ROUTING);
		nodes.add(EvidenceLineageNode.DISPATCH);
		nodes.add(EvidenceLineageNode.EXECUTION);
		if (collectionStagePresent) {
			nodes.add(EvidenceLineageNode.COLLECTION);
		}
		if (assessmentStagePresent) {
			nodes.add(EvidenceLineageNode.ASSESSMENT);
		}
		nodes.add(EvidenceLineageNode.SUMMARY);

		List<EvidenceLineageEdge> edges = new ArrayList<>();
		for (int i = 0; i < nodes.size() - 1; i++) {
			edges.add(new EvidenceLineageEdge(nodes.get(i), nodes.get(i + 1)));
		}

		return new EvidenceLineage(
				nodes,
				edges,
				status(governancePolicy, collectionStagePresent, assessmentStagePresent),
				reason(governancePolicy, collectionStagePresent, assessmentStagePresent),
				governancePolicy,
				riskLevel(governancePolicy)
		);
	}

	public boolean readOnlyTraceabilityModel() {
		return true;
	}

	public boolean mutatesEvidence() {
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

	private static EvidenceLineageStatus status(
			EvidenceGovernancePolicy governancePolicy,
			boolean collectionStagePresent,
			boolean assessmentStagePresent
	) {
		if (governancePolicy.classification() == EvidenceClassification.BLOCKED) {
			return EvidenceLineageStatus.BLOCKED;
		}
		if (governancePolicy.classification() == EvidenceClassification.GOVERNANCE_PROTECTED
				|| governancePolicy.classification() == EvidenceClassification.RESTRICTED) {
			return EvidenceLineageStatus.RESTRICTED;
		}
		if (governancePolicy.provenance().provenanceMissing()
				|| !collectionStagePresent
				|| !assessmentStagePresent) {
			return EvidenceLineageStatus.INCOMPLETE;
		}
		if (governancePolicy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY) {
			return EvidenceLineageStatus.PARTIAL;
		}
		if (governancePolicy.integrityStatus() == EvidenceIntegrityStatus.UNKNOWN) {
			return EvidenceLineageStatus.UNKNOWN;
		}
		return EvidenceLineageStatus.COMPLETE;
	}

	private static EvidenceLineageReason reason(
			EvidenceGovernancePolicy governancePolicy,
			boolean collectionStagePresent,
			boolean assessmentStagePresent
	) {
		if (governancePolicy.classification() == EvidenceClassification.BLOCKED) {
			return EvidenceLineageReason.BLOCKED_EVIDENCE;
		}
		if (governancePolicy.classification() == EvidenceClassification.GOVERNANCE_PROTECTED) {
			return EvidenceLineageReason.GOVERNANCE_PROTECTED_EVIDENCE;
		}
		if (governancePolicy.classification() == EvidenceClassification.RESTRICTED) {
			return EvidenceLineageReason.PAYMENT_RESTRICTED_EVIDENCE;
		}
		if (governancePolicy.provenance().provenanceMissing()) {
			return EvidenceLineageReason.MISSING_PROVENANCE;
		}
		if (!collectionStagePresent) {
			return EvidenceLineageReason.MISSING_COLLECTION_STAGE;
		}
		if (!assessmentStagePresent) {
			return EvidenceLineageReason.MISSING_ASSESSMENT_STAGE;
		}
		if (governancePolicy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY) {
			return EvidenceLineageReason.CONTRADICTORY_EVIDENCE;
		}
		return EvidenceLineageReason.UNKNOWN;
	}

	private static OperationalUncertainty riskLevel(
			EvidenceGovernancePolicy governancePolicy
	) {
		if (governancePolicy.classification() == EvidenceClassification.BLOCKED) {
			return OperationalUncertainty.CRITICAL;
		}
		if (governancePolicy.integrityStatus() == EvidenceIntegrityStatus.CONTRADICTORY) {
			return OperationalUncertainty.HIGH;
		}
		if (governancePolicy.classification() == EvidenceClassification.RESTRICTED
				|| governancePolicy.classification() == EvidenceClassification.GOVERNANCE_PROTECTED) {
			return OperationalUncertainty.HIGH;
		}
		if (governancePolicy.provenance().provenanceMissing()) {
			return OperationalUncertainty.HIGH;
		}
		if (governancePolicy.trustLevel() == EvidenceTrustLevel.UNKNOWN) {
			return OperationalUncertainty.MODERATE;
		}
		return OperationalUncertainty.LOW;
	}
}
