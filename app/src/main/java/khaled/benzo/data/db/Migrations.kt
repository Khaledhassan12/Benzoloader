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

    /**
     * 2 -> 3: per-download speed limit and expected checksum. Plain ADD COLUMN; the NOT NULL column carries the same
     * DEFAULT the entity declares (defaultValue = "0"), the nullable one has none. WRITTEN, UNBUILT: never run against a v2 database.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `downloads` ADD COLUMN `speed_limit_kbps` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `downloads` ADD COLUMN `expected_checksum` TEXT")
        }
    }

    /**
     * 3 -> 4: hand-set list position (Order sort, selection To top / To bottom / Up / Down). NOT NULL with the same DEFAULT
     * the entity declares; 0 means "never moved". WRITTEN, UNBUILT: never run against a v3 database.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `downloads` ADD COLUMN `list_order` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
