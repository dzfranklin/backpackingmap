package com.backpackingmap.backpackingmap.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import arrow.syntax.function.memoize
import com.backpackingmap.backpackingmap.db.user.DbUser
import com.backpackingmap.backpackingmap.db.user.UserDao

val MIGRATIONS: Array<Migration> = arrayOf(
    object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE users_new ('__enforceIsSingleton' INTEGER NOT NULL, id INTEGER NOT NULL, renewal_token TEXT NOT NULL, PRIMARY KEY('__enforceIsSingleton'))")
            database.execSQL("INSERT INTO users_new ('__enforceIsSingleton', id, renewal_token) SELECT '__enforceIsSingleton', id, renewal_token FROM users")
            database.execSQL("DROP TABLE users")
            database.execSQL("ALTER TABLE users_new RENAME TO users")
        }
    },
    object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE users RENAME COLUMN renewal_token TO renewalToken")
        }
    }
)

@Database(entities = [DbUser::class], version = 3)
abstract class Db : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        val getDatabase = ::getDatabaseUnmemoized.memoize()

        private fun getDatabaseUnmemoized(context: Context): Db {
            return Room.databaseBuilder(
                context.applicationContext,
                Db::class.java,
                "backpackingmap_database"
            )
                .addMigrations(*MIGRATIONS)
                .build()
        }
    }
}
