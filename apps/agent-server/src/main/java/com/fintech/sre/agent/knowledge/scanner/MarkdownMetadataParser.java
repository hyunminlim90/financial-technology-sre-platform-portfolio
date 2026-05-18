package com.fintech.sre.agent.knowledge.scanner;

import java.util.Map;

public interface MarkdownMetadataParser {

	Map<String, Object> parse(String markdown);
}
