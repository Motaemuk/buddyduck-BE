package db.migration;

import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__require_demographic_profile extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		String databaseProductName = context.getConnection()
			.getMetaData()
			.getDatabaseProductName()
			.toLowerCase(Locale.ROOT);

		try (Statement statement = context.getConnection().createStatement()) {
			if (databaseProductName.contains("h2")) {
				statement.execute("ALTER TABLE users ALTER COLUMN age_range DROP NOT NULL");
				statement.execute("ALTER TABLE users ALTER COLUMN gender DROP NOT NULL");
			} else {
				statement.execute("ALTER TABLE users MODIFY COLUMN age_range VARCHAR(20) NULL");
				statement.execute("ALTER TABLE users MODIFY COLUMN gender VARCHAR(20) NULL");
			}

			statement.execute("UPDATE users SET age_range = NULL WHERE age_range = 'PRIVATE'");
			statement.execute("UPDATE users SET gender = NULL WHERE gender = 'PRIVATE'");
			statement.execute("""
				UPDATE users
				SET profile_completed = FALSE
				WHERE age_range IS NULL
				   OR gender IS NULL
				""");
			statement.execute("""
				UPDATE users
				SET profile_completed = TRUE
				WHERE age_range IS NOT NULL
				  AND gender IS NOT NULL
				""");
			statement.execute("ALTER TABLE users DROP COLUMN age_visible");
			statement.execute("ALTER TABLE users DROP COLUMN gender_visible");
		}
	}
}
