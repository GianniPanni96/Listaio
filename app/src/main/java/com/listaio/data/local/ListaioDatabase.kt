package com.listaio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ItemEntity::class], version = 2, exportSchema = false)
abstract class ListaioDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        /** v2 adds the soft-delete (trash) flag, preserving existing items. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
