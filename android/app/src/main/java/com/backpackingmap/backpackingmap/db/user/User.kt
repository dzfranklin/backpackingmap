package com.backpackingmap.backpackingmap.db.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    // By having a primary key with the value of zero we enforce that only
    // one User can exist in the database
    @PrimaryKey val __enforceIsSingleton: Int,
    val id: Int,
    val renewal_token: String
) {
    constructor(id: Int, renewal_token: String) : this(0, id, renewal_token)
}