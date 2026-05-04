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

@Component
public class RunbookLoader {

	private static final String RUNBOOK_PATH = "classpath*:runbooks/*.yaml";

	private final ObjectMapper objectMapper;

	public RunbookLoader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
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
}
