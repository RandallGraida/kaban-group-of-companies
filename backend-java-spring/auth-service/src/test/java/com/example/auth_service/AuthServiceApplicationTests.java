package com.example.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		String testDbUrl = firstNonBlank(System.getProperty("TEST_DB_URL"), System.getenv("TEST_DB_URL"));
		String dbUsername = firstNonBlank(System.getProperty("DB_USERNAME"), System.getenv("DB_USERNAME"));
		String dbPassword = firstNonBlank(System.getProperty("DB_PASSWORD"), System.getenv("DB_PASSWORD"));

		registry.add("spring.datasource.url",
				() -> testDbUrl != null ? testDbUrl : "jdbc:postgresql://localhost:5432/auth_service_db");
		registry.add("spring.datasource.username", () -> dbUsername != null ? dbUsername : "randall");
		registry.add("spring.datasource.password", () -> dbPassword != null ? dbPassword : "");
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
		registry.add("spring.sql.init.mode", () -> "always");
		registry.add("spring.sql.init.continue-on-error", () -> "true");
	}

	private static String firstNonBlank(String a, String b) {
		if (a != null && !a.isBlank()) {
			return a;
		}
		if (b != null && !b.isBlank()) {
			return b;
		}
		return null;
	}

	@Test
	void contextLoads() {
	}

}
