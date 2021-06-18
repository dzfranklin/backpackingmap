package com.backpackingmap.backpackingmap.repo.db.adapter

import com.backpackingmap.backpackingmap.model.TripId
import com.squareup.sqldelight.ColumnAdapter

object TripIdAdapter : ColumnAdapter<TripId, ByteArray> {
    override fun decode(databaseValue: ByteArray) =
        TripId(UuidAdapter.decode(databaseValue))

    override fun encode(value: TripId) =
        UuidAdapter.encode(value.id)
}