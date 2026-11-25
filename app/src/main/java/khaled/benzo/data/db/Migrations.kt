package khaled.benzo.data.db

import androidx.room.migration.Migration

/**
 * Every schema change needs an entry here, so installed data is carried over and never wiped.
 * Version 1 is the first schema, so the list is empty. There is deliberately no fallback to a destructive migration:
 * a version bump without a migration fails at startup (loudly) instead of deleting downloads and sites.
 * Rule for a change: raise `version`, add a Migration(from, to) with plain ALTER/CREATE statements, and test it with
 * Room's MigrationTestHelper against the exported schema JSON in app/schemas.
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
