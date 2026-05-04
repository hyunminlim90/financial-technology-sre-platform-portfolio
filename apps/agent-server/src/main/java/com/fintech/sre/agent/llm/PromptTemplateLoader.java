package com.fintech.sre.agent.llm;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateLoader {

	public String load(String filename) {
		try {
			ClassPathResource resource = new ClassPathResource("prompts/" + filename);
			return resource.getContentAsString(StandardCharsets.UTF_8);
		} catch (Exception exception) {
			throw new IllegalStateException("Prompt template not found: " + filename, exception);
		}
	}
}
