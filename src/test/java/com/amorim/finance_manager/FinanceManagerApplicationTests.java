package com.amorim.finance_manager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class FinanceManagerApplicationTests {

	@Autowired
	private Environment environment;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Flyway flyway;

	@Autowired
	private PostgreSQLContainer postgres;

	@Test
	void contextLoads() {
	}

	@Test
	void usesIsolatedPostgresqlWithFlyway() throws SQLException {
		assertThat(environment.getActiveProfiles()).contains("test").doesNotContain("local");
		assertThat(postgres.isRunning()).isTrue();

		try (var connection = dataSource.getConnection()) {
			assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
			assertThat(connection.getMetaData().getURL()).isEqualTo(postgres.getJdbcUrl());
		}

		assertThat(flyway.info().current()).isNotNull();
	}

}
