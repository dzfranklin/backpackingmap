package com.backpackingmap.backpackingmap.repo.db.adapter

import com.backpackingmap.backpackingmap.model.TrackId
import com.squareup.sqldelight.ColumnAdapter

object TrackIdAdapter : ColumnAdapter<TrackId, ByteArray> {
    override fun decode(databaseValue: ByteArray) =
        TrackId(UuidAdapter.decode(databaseValue))

    override fun encode(value: TrackId) =
        UuidAdapter.encode(value.id)
}