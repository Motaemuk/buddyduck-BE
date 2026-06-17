package db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

class V3RequireDemographicProfileTest {

	@Test
	void 닉네임이_비어_있으면_profile_completed로_승격하지_않는다() throws Exception {
		try (Connection connection = DriverManager.getConnection(
			"jdbc:h2:mem:v3_profile_" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE"
		)) {
			createUsersTable(connection);
			insertUser(connection, 1L, " ", "TWENTIES", "FEMALE");
			insertUser(connection, 2L, "duck_fan", "TWENTIES", "FEMALE");

			new V3__require_demographic_profile().migrate(context(connection));

			assertThat(profileCompleted(connection, 1L)).isFalse();
			assertThat(profileCompleted(connection, 2L)).isTrue();
		}
	}

	private void createUsersTable(Connection connection) throws Exception {
		try (Statement statement = connection.createStatement()) {
			statement.execute("""
				CREATE TABLE users (
					id BIGINT NOT NULL PRIMARY KEY,
					nickname VARCHAR(30) NOT NULL,
					age_range VARCHAR(20) NOT NULL,
					gender VARCHAR(20) NOT NULL,
					profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
					age_visible BOOLEAN NOT NULL DEFAULT FALSE,
					gender_visible BOOLEAN NOT NULL DEFAULT FALSE
				)
				""");
		}
	}

	private void insertUser(
		Connection connection,
		Long id,
		String nickname,
		String ageRange,
		String gender
	) throws Exception {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("""
				INSERT INTO users (id, nickname, age_range, gender)
				VALUES (%d, '%s', '%s', '%s')
				""".formatted(id, nickname, ageRange, gender));
		}
	}

	private boolean profileCompleted(Connection connection, Long id) throws Exception {
		try (Statement statement = connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(
				"SELECT profile_completed FROM users WHERE id = " + id
			);
			resultSet.next();
			return resultSet.getBoolean("profile_completed");
		}
	}

	private Context context(Connection connection) {
		return new Context() {
			@Override
			public Configuration getConfiguration() {
				return null;
			}

			@Override
			public Connection getConnection() {
				return connection;
			}
		};
	}
}
