package com.backpackingmap.backpackingmap.repo.db.adapter

import com.squareup.sqldelight.ColumnAdapter
import java.nio.ByteBuffer
import java.util.*

object UuidAdapter : ColumnAdapter<UUID, ByteArray> {
    override fun decode(databaseValue: ByteArray): UUID {
        val buf = ByteBuffer.wrap(databaseValue)
        return UUID(
            buf.getLong(MOST_SIG_IDX),
            buf.getLong(LEAST_SIG_IDX)
        )
    }

    override fun encode(value: UUID): ByteArray {
        val dbValue = ByteBuffer.allocate(Long.SIZE_BYTES * 2)
        dbValue.putLong(MOST_SIG_IDX, value.mostSignificantBits)
        dbValue.putLong(LEAST_SIG_IDX, value.leastSignificantBits)
        return dbValue.array()
    }

}

private const val MOST_SIG_IDX = 0
private const val LEAST_SIG_IDX = 1