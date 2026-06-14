package com.buddyduck.buddyduck.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FlywayContextTest {

	@Autowired
	private Flyway flyway;

	@Test
	void Flyway가_schema_migration을_관리한다() {
		assertThat(flyway.info().all()).isNotEmpty();
	}
}
