package com.backpackingmap.backpackingmap.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import arrow.syntax.function.memoize
import com.backpackingmap.backpackingmap.db.user.User
import com.backpackingmap.backpackingmap.db.user.UserDao

@Database(entities = [User::class], version = 1)
abstract class Db : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        val getDatabase = ::getDatabaseUnmemoized.memoize()

        private fun getDatabaseUnmemoized(context: Context): Db {
            return Room.databaseBuilder(
                context.applicationContext,
                Db::class.java,
                "backpackingmap_database"
            ).build()
        }
    }
}
