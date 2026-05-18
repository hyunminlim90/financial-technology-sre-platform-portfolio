package com.fintech.sre.agent.model.response;

import java.util.List;

import com.fintech.sre.agent.evidence.EvidenceContext;

public record EvidenceSummaryView(
		String incidentId,
		List<String> matchedScenarioIds,
		List<String> matchedRunbookIds,
		List<String> matchedPostmortemIds,
		List<String> matchedImprovementIds,
		List<String> matchedPreventiveDesignIds,
		List<String> ragDocumentIds,
		List<EvidenceSignalView> signals
) {
	public static EvidenceSummaryView from(EvidenceContext context) {
		if (context == null) {
			return new EvidenceSummaryView(
					null,
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of(),
					List.of()
			);
		}

		return new EvidenceSummaryView(
				context.incidentId(),
				safe(context.matchedScenarioIds()),
				safe(context.matchedRunbookIds()),
				safe(context.matchedPostmortemIds()),
				safe(context.matchedImprovementIds()),
				safe(context.matchedPreventiveDesignIds()),
				safe(context.ragDocumentIds()),
				safe(context.signals()).stream()
						.map(EvidenceSignalView::from)
						.toList()
		);
	}

	private static <T> List<T> safe(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}
}
