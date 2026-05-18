package com.fintech.sre.agent.admin.knowledge;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KnowledgeAdminProperties.class)
public class KnowledgeAdminConfiguration {
}
