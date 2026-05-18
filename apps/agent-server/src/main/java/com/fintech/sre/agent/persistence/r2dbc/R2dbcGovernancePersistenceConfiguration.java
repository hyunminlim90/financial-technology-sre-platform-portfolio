package com.fintech.sre.agent.persistence.r2dbc;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration;
import org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;

@Configuration
@Profile("r2dbc")
@ImportAutoConfiguration({
		R2dbcAutoConfiguration.class,
		DataR2dbcAutoConfiguration.class,
		DataR2dbcRepositoriesAutoConfiguration.class
})
public class R2dbcGovernancePersistenceConfiguration {
}
