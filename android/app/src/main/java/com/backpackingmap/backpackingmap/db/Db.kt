package com.backpackingmap.backpackingmap.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.backpackingmap.backpackingmap.db.user.User
import com.backpackingmap.backpackingmap.db.user.UserDao
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized

// From <https://codelabs.developers.google.com/codelabs/android-room-with-a-view-kotlin/index.html?index=..%2F..index#7>
@Database(entities = [User::class], version = 1)
abstract class Db : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        // Singleton prevents multiple instances of the db opening at the same time
        @Volatile private var INSTANCE: Db? = null

        @OptIn(InternalCoroutinesApi::class)
        fun getDatabase(context: Context): Db {
            val tempInstance = INSTANCE

            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Db::class.java,
                    "backpackingmap_database"
                ).build()

                INSTANCE = instance

                return instance
            }
        }
    }
}