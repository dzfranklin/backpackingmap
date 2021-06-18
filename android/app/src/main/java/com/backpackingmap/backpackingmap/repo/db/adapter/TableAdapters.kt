package com.backpackingmap.backpackingmap.repo.db.adapter

import com.backpackingmap.backpackingmap.CoordinateModeTable
import com.backpackingmap.backpackingmap.TrackMetaTable
import com.backpackingmap.backpackingmap.TripMetaTable
import com.squareup.sqldelight.EnumColumnAdapter

object TableAdapters {
    val trackMeta = TrackMetaTable.Adapter(
        idAdapter = TrackIdAdapter,
        tripIdAdapter = TripIdAdapter,
    )

    val tripMeta = TripMetaTable.Adapter(
        idAdapter = TripIdAdapter,
    )

    val coordinateMode = CoordinateModeTable.Adapter(
        valueAdapter = EnumColumnAdapter(),
    )
}