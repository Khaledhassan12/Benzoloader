package khaled.benzo.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every schema change needs an entry here, so installed data is carried over and never wiped.
 * There is deliberately no fallback to a destructive migration: a version bump without a migration fails at startup
 * (loudly) instead of deleting downloads and sites.
 * Rule for a change: raise `version`, add a Migration(from, to) with plain ALTER/CREATE statements, and test it with
 * Room's MigrationTestHelper against the exported schema JSON in app/schemas.
 *
 * 1 -> 2: adds the browser history table. The statement must match what Room expects for HistoryEntity exactly
 * (column names, types, NOT NULL, primary key); it was written by hand and is unchecked until the first build, which also
 * exports the v2 schema JSON. The v1 JSON was never generated, so MigrationTestHelper cannot run for 1 -> 2 yet.
 */
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT NOT NULL, `url` TEXT NOT NULL, `visited_at` INTEGER NOT NULL)"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
