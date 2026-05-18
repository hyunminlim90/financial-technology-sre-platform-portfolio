package com.fintech.sre.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(excludeName = {
		"org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration",
		"org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcRepositoriesAutoConfiguration",
		"org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration"
})
@ConfigurationPropertiesScan
public class AgentServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentServerApplication.class, args);
	}

}
