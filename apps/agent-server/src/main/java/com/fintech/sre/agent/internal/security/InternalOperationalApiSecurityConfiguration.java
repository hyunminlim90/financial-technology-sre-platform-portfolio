package com.fintech.sre.agent.internal.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InternalOperationalApiSecurityProperties.class)
public class InternalOperationalApiSecurityConfiguration {
}
