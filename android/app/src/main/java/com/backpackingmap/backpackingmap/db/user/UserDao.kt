package com.backpackingmap.backpackingmap.db.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(dbUser: DbUser)

    @Query("SELECT * from users")
    suspend fun getUsers(): List<DbUser>

    @Query("DELETE FROM users")
    suspend fun deleteUsers()
}