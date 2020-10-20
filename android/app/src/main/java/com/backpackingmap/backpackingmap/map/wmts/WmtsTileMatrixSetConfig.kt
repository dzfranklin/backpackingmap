package com.backpackingmap.backpackingmap.map.wmts

import org.locationtech.proj4j.CoordinateReferenceSystem
import kotlin.math.floor

data class WmtsTileMatrixSetConfig(
    val identifier: String,
    val title: String,
    val boundingBox: WmtsBoundingBox,
    val tileMatrices: Array<WmtsTileMatrixConfig>,
    val crs: CoordinateReferenceSystem,
) {
    fun tileIndices(
        matrix: WmtsTileMatrixConfig,
        minCrsX: Double,
        maxCrsX: Double,
        minCrsY: Double,
        maxCrsY: Double,
    ): WmtsTileRange {
        /* 6.1 Tile matrix set – the geometry of the tiled space
        From OGC 07-057r7 (WMTS standard version 1.0.0) page 8

        The scale denominator is defined with respect to a "standardized rendering pixel size" of
        0.28 mm × 0.28 mm (millimeters). The definition is the same used in WMS 1.3.0 [OGC 06-042]
        and in Symbology Encoding Implementation Specification 1.1.0 [05-077r4]. Frequently, the
        true pixel size is unknown and 0.28 mm is a common actual size for current displays.

        ...

        pixelSpan = scaleDenominator × 0.28 10^-3 / metersPerUnit(crs);
        tileSpanX = tileWidth × pixelSpan;
        tileSpanY = tileHeight × pixelSpan;
        */
        val pixelSpan = pixelsPerMeter(matrix)
        val tileSpanX = matrix.tileWidth.toInt() * pixelSpan
        val tileSpanY = matrix.tileHeight.toInt() * pixelSpan

        /* H.1 From BBOX to tile indices
        From OGC 07-057r7 (WMTS standard version 1.0.0) page 112

        The following fragment of pseudocode could be used to convert from a desired bounding box
        (bBoxMinX, bBoxMinY, bBoxMaxX, bBoxMaxY) in CRS coordinates to a range of tile set indices.
        This pseudocode uses the same notation that subclause 6.1 uses. In this pseudocode we assume
        that bBoxMinX, bBoxMinY, bBoxMaxX, bBoxMaxY, tileMatrixMinX, tileMatrixMinY, tileMatrixMinY,
        tileMatrixMaxY, tileSpanX and tileSpanY are floating point variables (IEEE-754) that has
        accuracy issues derived from the finite precision of the representation. These accuracy
        issues could be amplified in a typical floor() rounding down function that could return a
        value ±1 than that expected. To overcome this issue this code uses a small value (epsilon)
        added or subtracted in a place that is not affected by CRS coordinate precision.

        // to compensate for floating point computation inaccuracies
        epsilon = 1e-6

        tileMinCol = floor((bBoxMinX - tileMatrixMinX) / tileSpanX + epsilon)
        tileMaxCol = floor((bBoxMaxX - tileMatrixMinX) / tileSpanX - epsilon)
        tileMinRow = floor((tileMatrixMaxY - bBoxMaxY) / tileSpanY + epsilon)
        tileMaxRow = floor((tileMatrixMaxY - bBoxMinY) / tileSpanY - epsilon)

        // to avoid requesting out-of-range tiles
        if (tileMinCol < 0) tileMinCol = 0
        if (tileMaxCol >= matrixWidth) tileMaxCol = matrixWidth-1
        if (tileMinRow < 0) tileMinRow = 0
        if (tileMaxRow >= matrixHeight) tileMaxRow = matrixHeight-1

        To fetch all the tiles that cover this bounding box, a client would scan through tileMinCol
        to tileMaxCol and tileMinRow to tileMaxRow, all inclusive. A total of
        (tileMaxCol - tileMinCol + 1) × (tileMaxRow - tileMinRow + 1) will be fetched.
        */

        val epsilon = 1e-6

        val (tileMatrixMinX, tileMatrixMaxY) = matrix.topLeftCorner

        val rawMinCol = (minCrsX - tileMatrixMinX) / tileSpanX
        var tileMinCol = floor(rawMinCol + epsilon).toInt()
        val minColOverageInCrs = (rawMinCol - tileMinCol) * tileSpanX

        var tileMaxCol = floor((maxCrsX - tileMatrixMinX) / tileSpanX - epsilon).toInt()
        var tileMaxRow = floor((tileMatrixMaxY - minCrsY) / tileSpanY - epsilon).toInt()

        val rawMinRow = (tileMatrixMaxY - maxCrsY) / tileSpanY
        var tileMinRow = floor(rawMinRow + epsilon).toInt()
        val minRowOverageInCrs = (rawMinRow - tileMinRow) * tileSpanY

        if (tileMinCol < 0) {
            tileMinCol = 0
        }

        if (tileMaxCol >= matrix.matrixWidthInTiles) {
            tileMaxCol = matrix.matrixWidthInTiles - 1
        }

        if (tileMinRow < 0) {
            tileMinRow = 0
        }

        if (tileMaxRow >= matrix.matrixHeightInTiles) {
            tileMaxRow = matrix.matrixHeightInTiles - 1
        }

        return WmtsTileRange(
            minColOverageInCrs = minColOverageInCrs,
            minRowOverageInCrs = minRowOverageInCrs,
            minColInclusive = tileMinCol,
            maxColInclusive = tileMaxCol,
            minRowInclusive = tileMinRow,
            maxRowInclusive = tileMaxRow
        )
    }

    fun pixelsPerMeter(matrix: WmtsTileMatrixConfig): Double {
        /* 6.1 Tile matrix set – the geometry of the tiled space
        From OGC 07-057r7 (WMTS standard version 1.0.0) page 8

        The scale denominator is defined with respect to a "standardized rendering pixel size" of
        0.28 mm × 0.28 mm (millimeters). The definition is the same used in WMS 1.3.0 [OGC 06-042]
        and in Symbology Encoding Implementation Specification 1.1.0 [05-077r4]. Frequently, the
        true pixel size is unknown and 0.28 mm is a common actual size for current displays.

        ...

        pixelSpan = scaleDenominator × 0.28 10^-3 / metersPerUnit(crs);
        tileSpanX = tileWidth × pixelSpan;
        tileSpanY = tileHeight × pixelSpan;
        */
        // TODO: This currently works only because the OS projections use metres as their unit
        // Every projection in proj4j has 1 for fromMetres
        val metersPerUnit = crs.projection.fromMetres
        return matrix.scaleDenominator * 0.28E-3 / metersPerUnit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WmtsTileMatrixSetConfig

        if (identifier != other.identifier) return false

        return true
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }
}
