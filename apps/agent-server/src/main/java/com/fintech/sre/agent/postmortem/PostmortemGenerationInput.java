package com.fintech.sre.agent.postmortem;

import com.fintech.sre.agent.model.request.PostmortemGenerateByIncidentRequest;
import com.fintech.sre.agent.rag.RagSearchResult;

public record PostmortemGenerationInput(
		PostmortemGenerateByIncidentRequest request,
		PostmortemIncidentContext context,
		RagSearchResult ragSearchResult
) {
}
