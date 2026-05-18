package com.fintech.sre.agent.knowledge.embedding.local;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LocalEmbeddingProperties.class)
public class LocalEmbeddingConfiguration {
}
