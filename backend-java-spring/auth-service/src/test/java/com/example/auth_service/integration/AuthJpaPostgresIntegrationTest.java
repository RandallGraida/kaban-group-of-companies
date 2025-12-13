package com.example.auth_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.auth_service.model.UserAccount;
import com.example.auth_service.model.VerificationToken;
import com.example.auth_service.repository.UserAccountRepository;
import com.example.auth_service.repository.VerificationTokenRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthJpaPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("authdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void users_table_has_enabled_column() {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'users'
                  and column_name = 'enabled'
                """,
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void users_email_is_unique() {
        UserAccount u1 = new UserAccount();
        u1.setEmail("dup@example.com");
        u1.setPasswordHash("hash");
        u1.setActive(true);
        u1.setEnabled(false);
        userRepository.saveAndFlush(u1);

        UserAccount u2 = new UserAccount();
        u2.setEmail("dup@example.com");
        u2.setPasswordHash("hash2");
        u2.setActive(true);
        u2.setEnabled(false);

        assertThatThrownBy(() -> userRepository.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void verification_token_is_unique_per_user() {
        UserAccount user = new UserAccount();
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        VerificationToken t1 = new VerificationToken();
        t1.setToken("t1");
        t1.setUser(user);
        t1.setExpiryDate(Instant.now().plusSeconds(60));
        tokenRepository.saveAndFlush(t1);

        VerificationToken t2 = new VerificationToken();
        t2.setToken("t2");
        t2.setUser(user);
        t2.setExpiryDate(Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> tokenRepository.saveAndFlush(t2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
