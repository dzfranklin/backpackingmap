package com.backpackingmap.backpackingmap.db.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class DbUser(
    // By having a primary key with the value of zero we enforce that only
    // one User can exist in the database
    @PrimaryKey val __enforceIsSingleton: Int,
    val id: Int,
    var renewalToken: String
) {
    constructor(id: Int, renewalToken: String) : this(0, id, renewalToken)
}