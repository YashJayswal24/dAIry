package com.yashjayswal.dairy.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// A real migration, not destructive fallback: this database holds real
// personal diary entries on real devices, so a schema change must not
// wipe existing rows. Existing entries get an empty title; the UI falls
// back to deriving a display title from the entry text when title is
// blank (see DiaryEntry.displayTitle()).
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entries ADD COLUMN title TEXT NOT NULL DEFAULT ''")
    }
}
