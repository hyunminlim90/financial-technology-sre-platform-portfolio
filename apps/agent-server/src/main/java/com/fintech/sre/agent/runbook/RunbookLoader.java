package com.fintech.sre.agent.runbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.sre.agent.evidence.EvidenceContext;
import com.fintech.sre.agent.model.request.IncidentRecommendationRequest;

@Component
public class RunbookLoader {

	private static final String RUNBOOK_PATH = "classpath*:runbooks/*.yaml";

	private final ObjectMapper objectMapper;
	private final RunbookConditionMatcher conditionMatcher;

	public RunbookLoader(
			ObjectMapper objectMapper,
			RunbookConditionMatcher conditionMatcher
	) {
		this.objectMapper = objectMapper;
		this.conditionMatcher = conditionMatcher;
	}

	public List<RunbookDefinition> loadAll() {
		try {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			Resource[] resources = resolver.getResources(RUNBOOK_PATH);
			List<RunbookDefinition> runbooks = new ArrayList<>();
			Yaml yaml = new Yaml();

			for (Resource resource : resources) {
				try (InputStream inputStream = resource.getInputStream()) {
					Object loaded = yaml.load(inputStream);
					if (loaded instanceof Map<?, ?> data) {
						runbooks.add(objectMapper.convertValue(data, RunbookDefinition.class));
					}
				}
			}

			return runbooks;
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to load runbook YAML files", exception);
		}
	}

	public reactor.core.publisher.Flux<RunbookDefinition> loadMatching(
			IncidentRecommendationRequest request,
			EvidenceContext evidenceContext
	) {
		return reactor.core.publisher.Flux.fromIterable(loadAll())
				.filter(runbook -> matchesRequiredEvidence(runbook, evidenceContext))
				.map(runbook -> withMatchingBranches(runbook, evidenceContext))
				.filter(runbook -> runbook.branches() != null && !runbook.branches().isEmpty());
	}

	private boolean matchesRequiredEvidence(RunbookDefinition runbook, EvidenceContext evidenceContext) {
		if (runbook.requiredEvidence() == null || runbook.requiredEvidence().isEmpty()) {
			return true;
		}
		if (evidenceContext == null) {
			return false;
		}
		return runbook.requiredEvidence().stream()
				.allMatch(signal -> evidenceContext.hasReliableSignal(com.fintech.sre.agent.evidence.EvidenceSignal.valueOf(signal)));
	}

	private RunbookDefinition withMatchingBranches(RunbookDefinition runbook, EvidenceContext evidenceContext) {
		if (runbook.branches() == null || runbook.branches().isEmpty()) {
			return runbook;
		}
		List<RunbookBranch> branches = runbook.branches().stream()
				.filter(branch -> conditionMatcher.matches(branch.when(), evidenceContext))
				.toList();
		return new RunbookDefinition(
				runbook.id(),
				runbook.scenario(),
				runbook.title(),
				runbook.description(),
				runbook.requiredEvidence(),
				branches,
				runbook.forbiddenActions()
		);
	}
}
