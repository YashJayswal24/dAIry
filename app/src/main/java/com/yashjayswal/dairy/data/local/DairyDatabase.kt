package com.yashjayswal.dairy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yashjayswal.dairy.data.local.dao.EntryDao
import com.yashjayswal.dairy.data.local.entity.EntryEntity

@Database(entities = [EntryEntity::class], version = 2, exportSchema = true)
abstract class DairyDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
