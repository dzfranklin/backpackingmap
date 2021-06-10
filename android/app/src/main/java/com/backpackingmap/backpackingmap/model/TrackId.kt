package com.backpackingmap.backpackingmap.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class TrackId(val id: UUID) : Parcelable {
    companion object {
        fun generate() =
            TrackId(UUID.randomUUID())

        @JvmField
        @Suppress("Unused")
        val CREATOR = object : Parcelable.Creator<TrackId> {
            override fun createFromParcel(parcel: Parcel): TrackId {
                val high = parcel.readLong()
                val low = parcel.readLong()
                return TrackId(UUID(high, low))
            }

            override fun newArray(size: Int): Array<TrackId?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id.mostSignificantBits)
        parcel.writeLong(id.leastSignificantBits)
    }

    override fun describeContents(): Int {
        return 0
    }
}