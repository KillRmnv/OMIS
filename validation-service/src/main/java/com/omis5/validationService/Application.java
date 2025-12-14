package com.omis5.validationService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.omis5") // чтобы видел Entity из common-dto
@EnableJpaRepositories(basePackages = "com.omis5")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
