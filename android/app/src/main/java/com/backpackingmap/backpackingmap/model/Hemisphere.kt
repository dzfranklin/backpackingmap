package com.backpackingmap.backpackingmap.model

import androidx.annotation.StringRes
import com.backpackingmap.backpackingmap.R

enum class Hemisphere(@StringRes val label: Int) {
    North(R.string.north),
    South(R.string.south);

    fun toWWId(): String =
        when (this) {
            North -> WW_ID_N
            South -> WW_ID_S
        }

    companion object {
        fun fromWWId(id: String): Hemisphere =
            when (id) {
                WW_ID_N -> North
                WW_ID_S -> South
                else -> throw IllegalArgumentException("Unexpected value for UTMCoord.hemisphere: $id")
            }

        private const val WW_ID_N = "gov.nasa.worldwind.avkey.North"
        private const val WW_ID_S = "gov.nasa.worldwind.avkey.South"
    }
}