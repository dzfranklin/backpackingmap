package com.backpackingmap.backpackingmap.db.user

import androidx.room.*

@Dao
interface UserDao {
    @Update
    suspend fun updateUsers(vararg user: DbUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(dbUser: DbUser)

    @Query("SELECT * from users")
    suspend fun getUsers(): List<DbUser>

    @Query("DELETE FROM users")
    suspend fun deleteUsers()
}