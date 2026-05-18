package com.fintech.sre.agent.knowledge.qdrant;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
public class QdrantKnowledgeConfiguration {
}
