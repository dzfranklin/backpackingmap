package com.backpackingmap.backpackingmap.map.wmts.os

import com.backpackingmap.backpackingmap.map.NaiveCoordinate
import com.backpackingmap.backpackingmap.map.Pixel
import com.backpackingmap.backpackingmap.map.wmts.*
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem

class OsServiceConfig : WmtsServiceConfig {
    override val identifier = "OS 1.0.0"
    private val crsFactory = CRSFactory()

    private val epsg27700Crs: CoordinateReferenceSystem =
        crsFactory.createFromName("EPSG:27700")

    private val epsg27700TileMatrix0 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:0",
        scaleDenominator = 3199999.999496063,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 5,
        matrixHeightInTiles = 7
    )

    private val epsg27700TileMatrix1 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:1",
        scaleDenominator = 1599999.9997480316,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 10,
        matrixHeightInTiles = 13,
    )

    private val epsg27700TileMatrix2 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:2",
        scaleDenominator = 799999.9998740158,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 20,
        matrixHeightInTiles = 25,
    )

    private val epsg27700TileMatrix3 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:3",
        scaleDenominator = 399999.9999370079,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 40,
        matrixHeightInTiles = 49,
    )

    private val epsg27700TileMatrix4 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:4",
        scaleDenominator = 199999.99996850395,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 80,
        matrixHeightInTiles = 98,
    )

    private val epsg27700TileMatrix5 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:5",
        scaleDenominator = 99999.99998425198,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 159,
        matrixHeightInTiles = 195,
    )

    private val epsg27700TileMatrix6 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:6",
        scaleDenominator = 49999.99999212599,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 318,
        matrixHeightInTiles = 390,
    )

    private val epsg27700TileMatrix7 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:7",
        scaleDenominator = 24999.999996062994,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 636,
        matrixHeightInTiles = 779,
    )

    private val epsg27700TileMatrix8 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:8",
        scaleDenominator = 12499.999998031497,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 1271,
        matrixHeightInTiles = 1558,
    )

    private val epsg27700TileMatrix9 = WmtsTileMatrixConfig(
        identifier = "EPSG:27700:9",
        scaleDenominator = 6249.9999990157485,
        topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 2542,
        matrixHeightInTiles = 3116,
    )

    private val epsg27700TileMatrixSet = WmtsTileMatrixSetConfig(
        identifier = "EPSG:27700",
        title = "TileMatrix for EPSG:27700 using 0.28mm",
        boundingBox = WmtsBoundingBox(
            lowerCorner = NaiveCoordinate( -238375.0000149319, 0.0),
            upperCorner = NaiveCoordinate( 900000.00000057, 1376256.0000176653)
        ),
        tileMatrices = arrayOf(
            epsg27700TileMatrix0,
            epsg27700TileMatrix1,
            epsg27700TileMatrix2,
            epsg27700TileMatrix3,
            epsg27700TileMatrix4,
            epsg27700TileMatrix5,
            epsg27700TileMatrix6,
            epsg27700TileMatrix7,
            epsg27700TileMatrix8,
            epsg27700TileMatrix9,
            WmtsTileMatrixConfig(
                identifier = "EPSG:27700:10",
                scaleDenominator = 3124.9999995078742,
                topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
                tileWidth = Pixel(256),
                tileHeight = Pixel(256),
                matrixWidthInTiles = 5083,
                matrixHeightInTiles = 6232,
            ),
            WmtsTileMatrixConfig(
                identifier = "EPSG:27700:11",
                scaleDenominator = 1562.4999997539371,
                topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
                tileWidth = Pixel(256),
                tileHeight = Pixel(256),
                matrixWidthInTiles = 10165,
                matrixHeightInTiles = 12463,
            ),
            WmtsTileMatrixConfig(
                identifier = "EPSG:27700:12",
                scaleDenominator = 781.2499998769686,
                topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
                tileWidth = Pixel(256),
                tileHeight = Pixel(256),
                matrixWidthInTiles = 20329,
                matrixHeightInTiles = 24925,
            ),
            WmtsTileMatrixConfig(
                identifier = "EPSG:27700:13",
                scaleDenominator = 390.6249999384843,
                topLeftCorner = NaiveCoordinate( -238375.0, 1376256.0),
                tileWidth = Pixel(256),
                tileHeight = Pixel(256),
                matrixWidthInTiles = 40657,
                matrixHeightInTiles = 49849,
            ),
        ),
        crs = epsg27700Crs
    )

    private val epsg3857Crs: CoordinateReferenceSystem =
        crsFactory.createFromName("EPSG:3857")

    private val epsg3857TileMatrix7 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:7",
        scaleDenominator = 4367830.1870353315,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 65,
        matrixHeightInTiles = 44,
    )

    private val epsg3857TileMatrix8 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:8",
        scaleDenominator = 2183915.0935181477,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 130,
        matrixHeightInTiles = 88,
    )

    private val epsg3857TileMatrix9 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:9",
        scaleDenominator = 1091957.5467586026,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 260,
        matrixHeightInTiles = 175,
    )

    private val epsg3857TileMatrix10 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:10",
        scaleDenominator = 545978.7733797727,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 519,
        matrixHeightInTiles = 350,
    )

    private val epsg3857TileMatrix11 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:11",
        scaleDenominator = 272989.3866894138,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 1037,
        matrixHeightInTiles = 699,
    )

    private val epsg3857TileMatrix12 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:12",
        scaleDenominator = 136494.6933447069,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 2073,
        matrixHeightInTiles = 1398,
    )

    private val epsg3857TileMatrix13 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:13",
        scaleDenominator = 68247.34667235345,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 4145,
        matrixHeightInTiles = 2795,
    )

    private val epsg3857TileMatrix14 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:14",
        scaleDenominator = 34123.673336176726,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 8290,
        matrixHeightInTiles = 5590,
    )

    private val epsg3857TileMatrix15 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:15",
        scaleDenominator = 17061.836668560845,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 16580,
        matrixHeightInTiles = 11180,
    )


    private val epsg3857TileMatrix16 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:16",
        scaleDenominator = 8530.918334280406,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 33159,
        matrixHeightInTiles = 22360,
    )

    private val epsg3857TileMatrix17 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:17",
        scaleDenominator = 4265.459166667739,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 66317,
        matrixHeightInTiles = 44719,
    )

    private val epsg3857TileMatrix18 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:18",
        scaleDenominator = 2132.7295838063405,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 132634,
        matrixHeightInTiles = 89437,
    )

    private val epsg3857TileMatrix19 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:19",
        scaleDenominator = 1066.3647914307007,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 265267,
        matrixHeightInTiles = 178873,
    )

    private val epsg3857TileMatrix20 = WmtsTileMatrixConfig(
        identifier = "EPSG:3857:20",
        scaleDenominator = 533.1823957153497,
        topLeftCorner = NaiveCoordinate( -2.0037508342787E7, 2.0037508342787E7),
        tileWidth = Pixel(256),
        tileHeight = Pixel(256),
        matrixWidthInTiles = 530533,
        matrixHeightInTiles = 357746,
    )

    private val epsg3857TileMatrixSet = WmtsTileMatrixSetConfig(
        identifier = "EPSG:3857",
        title = "TileMatrix for EPSG:3857 using 0.28mm",
        boundingBox = WmtsBoundingBox(
            lowerCorner = NaiveCoordinate(-1198263.0364071354, 6365004.037965424),
            upperCorner = NaiveCoordinate( 213000.0, 8702260.01),
        ),
        tileMatrices = arrayOf(
            epsg3857TileMatrix7,
            epsg3857TileMatrix8,
            epsg3857TileMatrix9,
            epsg3857TileMatrix10,
            epsg3857TileMatrix11,
            epsg3857TileMatrix12,
            epsg3857TileMatrix13,
            epsg3857TileMatrix14,
            epsg3857TileMatrix15,
            epsg3857TileMatrix16,
            epsg3857TileMatrix17,
            epsg3857TileMatrix18,
            epsg3857TileMatrix19,
            epsg3857TileMatrix20,
        ),
        crs = epsg3857Crs
    )

    override val layers = arrayOf(
        WmtsLayerConfig(
            identifier = "Light_27700",
            title = "OS Light",
            wgs84BoundingBox = WmtsBoundingBox(
                lowerCorner = NaiveCoordinate(-10.8342841886, 49.38802038505334),
                upperCorner = NaiveCoordinate(7.551552493184295, 62.2662683043619)
            ),
            set = epsg27700TileMatrixSet,
            matrices = emptyMap()
        ),
        WmtsLayerConfig(
            identifier = "Light_3857",
            title = "OS Light",
            wgs84BoundingBox = WmtsBoundingBox(
                lowerCorner = NaiveCoordinate(-10.764179999999964, 49.52842300000003),
                upperCorner = NaiveCoordinate(1.9134115552, 61.3311510086)
            ),
            set = epsg3857TileMatrixSet,
            matrices = mapOf(
                epsg3857TileMatrix7 to WmtsTileMatrixLimits(
                    minTileRow = 35,
                    maxTileRow = 43,
                    minTileCol = 60,
                    maxTileCol = 64
                ),
                epsg3857TileMatrix8 to WmtsTileMatrixLimits(
                    minTileRow = 71,
                    maxTileRow = 87,
                    minTileCol = 120,
                    maxTileCol = 129
                ),
                epsg3857TileMatrix9 to WmtsTileMatrixLimits(
                    minTileRow = 143,
                    maxTileRow = 174,
                    minTileCol = 240,
                    maxTileCol = 259
                ),
                epsg3857TileMatrix10 to WmtsTileMatrixLimits(
                    minTileRow = 286,
                    maxTileRow = 349,
                    minTileCol = 481,
                    maxTileCol = 518
                ),
                epsg3857TileMatrix11 to WmtsTileMatrixLimits(
                    minTileRow = 573,
                    maxTileRow = 698,
                    minTileCol = 962,
                    maxTileCol = 1036
                ),
                epsg3857TileMatrix12 to WmtsTileMatrixLimits(
                    minTileRow = 1146,
                    maxTileRow = 1397,
                    minTileCol = 1925,
                    maxTileCol = 2072
                ),
                epsg3857TileMatrix13 to WmtsTileMatrixLimits(
                    minTileRow = 2292,
                    maxTileRow = 2794,
                    minTileCol = 3851,
                    maxTileCol = 4144
                ),
                epsg3857TileMatrix14 to WmtsTileMatrixLimits(
                    minTileRow = 4584,
                    maxTileRow = 5589,
                    minTileCol = 7702,
                    maxTileCol = 8289
                ),
                epsg3857TileMatrix15 to WmtsTileMatrixLimits(
                    minTileRow = 9169,
                    maxTileRow = 11179,
                    minTileCol = 15404,
                    maxTileCol = 16579
                ),
                epsg3857TileMatrix16 to WmtsTileMatrixLimits(
                    minTileRow = 18338,
                    maxTileRow = 22359,
                    minTileCol = 30808,
                    maxTileCol = 33158
                ),
                epsg3857TileMatrix17 to WmtsTileMatrixLimits(
                    minTileRow = 36676,
                    maxTileRow = 44718,
                    minTileCol = 61616,
                    maxTileCol = 66316
                ),
                epsg3857TileMatrix18 to WmtsTileMatrixLimits(
                    minTileRow = 73353,
                    maxTileRow = 89436,
                    minTileCol = 123233,
                    maxTileCol = 132633
                ),
                epsg3857TileMatrix19 to WmtsTileMatrixLimits(
                    minTileRow = 146706,
                    maxTileRow = 178872,
                    minTileCol = 246467,
                    maxTileCol = 265266
                ),
                epsg3857TileMatrix20 to WmtsTileMatrixLimits(
                    minTileRow = 293412,
                    maxTileRow = 357745,
                    minTileCol = 492935,
                    maxTileCol = 530532
                ),
            )
        ),
        WmtsLayerConfig(
            identifier = "Outdoor_27700",
            title = "OS Outdoor",
            wgs84BoundingBox = WmtsBoundingBox(
                lowerCorner = NaiveCoordinate(-10.8342841886, 49.38802038505334),
                upperCorner = NaiveCoordinate(7.551552493184295, 62.2662683043619)
            ),
            set = epsg27700TileMatrixSet,
            matrices = emptyMap()
        ),
        WmtsLayerConfig(
            identifier = "Outdoor_3857",
            title = "OS Outdoor",
            wgs84BoundingBox = WmtsBoundingBox(
                lowerCorner = NaiveCoordinate(-10.764179999999964, 49.52842300000003),
                upperCorner = NaiveCoordinate(1.9134115552, 61.3311510086)
            ),
            set = epsg3857TileMatrixSet,
            matrices = mapOf(
                epsg3857TileMatrix7 to WmtsTileMatrixLimits(
                    minTileRow = 35,
                    maxTileRow = 43,
                    minTileCol = 60,
                    maxTileCol = 64
                ),
                epsg3857TileMatrix8 to WmtsTileMatrixLimits(
                    minTileRow = 71,
                    maxTileRow = 87,
                    minTileCol = 120,
                    maxTileCol = 129
                ),
                epsg3857TileMatrix9 to WmtsTileMatrixLimits(
                    minTileRow = 143,
                    maxTileRow = 174,
                    minTileCol = 240,
                    maxTileCol = 259
                ),
                epsg3857TileMatrix10 to WmtsTileMatrixLimits(
                    minTileRow = 286,
                    maxTileRow = 349,
                    minTileCol = 481,
                    maxTileCol = 518
                ),
                epsg3857TileMatrix11 to WmtsTileMatrixLimits(
                    minTileRow = 573,
                    maxTileRow = 698,
                    minTileCol = 962,
                    maxTileCol = 1036
                ),
                epsg3857TileMatrix12 to WmtsTileMatrixLimits(
                    minTileRow = 1146,
                    maxTileRow = 1397,
                    minTileCol = 1925,
                    maxTileCol = 2072
                ),
                epsg3857TileMatrix13 to WmtsTileMatrixLimits(
                    minTileRow = 2292,
                    maxTileRow = 2794,
                    minTileCol = 3851,
                    maxTileCol = 4144
                ),
                epsg3857TileMatrix14 to WmtsTileMatrixLimits(
                    minTileRow = 4584,
                    maxTileRow = 5589,
                    minTileCol = 7702,
                    maxTileCol = 8289
                ),
                epsg3857TileMatrix15 to WmtsTileMatrixLimits(
                    minTileRow = 9169,
                    maxTileRow = 11179,
                    minTileCol = 15404,
                    maxTileCol = 16579
                ),
                epsg3857TileMatrix16 to WmtsTileMatrixLimits(
                    minTileRow = 18338,
                    maxTileRow = 22359,
                    minTileCol = 30808,
                    maxTileCol = 33158
                ),
                epsg3857TileMatrix17 to WmtsTileMatrixLimits(
                    minTileRow = 36676,
                    maxTileRow = 44718,
                    minTileCol = 61616,
                    maxTileCol = 66316
                ),
                epsg3857TileMatrix18 to WmtsTileMatrixLimits(
                    minTileRow = 73353,
                    maxTileRow = 89436,
                    minTileCol = 123233,
                    maxTileCol = 132633
                ),
                epsg3857TileMatrix19 to WmtsTileMatrixLimits(
                    minTileRow = 146706,
                    maxTileRow = 178872,
                    minTileCol = 246467,
                    maxTileCol = 265266
                ),
                epsg3857TileMatrix20 to WmtsTileMatrixLimits(
                    minTileRow = 293412,
                    maxTileRow = 357745,
                    minTileCol = 492935,
                    maxTileCol = 530532
                ),
            )
        ),
        WmtsLayerConfig(
            identifier = "Road_27700",
            title = "OS Road",
            wgs84BoundingBox = WmtsBoundingBox(
                lowerCorner = NaiveCoordinate(-10.8342841886, 49.38802038505334),
                upperCorner = NaiveCoordinate(7.551552493184295, 62.2662683043619)
            ),
            set = epsg27700TileMatrixSet,
            matrices = emptyMap()
        ),
        WmtsLayerConfig(
            identifier = "Road_3857",
            title = "OS Road",
            wgs84BoundingBox = WmtsBoundingBox(
                lowerCorner = NaiveCoordinate(-10.764179999999964, 49.52842300000003),
                upperCorner = NaiveCoordinate(1.9134115552, 61.3311510086)
            ),
            set = epsg3857TileMatrixSet,
            matrices = mapOf(
                epsg3857TileMatrix7 to WmtsTileMatrixLimits(
                    minTileRow = 35,
                    maxTileRow = 43,
                    minTileCol = 60,
                    maxTileCol = 64
                ),
                epsg3857TileMatrix8 to WmtsTileMatrixLimits(
                    minTileRow = 71,
                    maxTileRow = 87,
                    minTileCol = 120,
                    maxTileCol = 129
                ),
                epsg3857TileMatrix9 to WmtsTileMatrixLimits(
                    minTileRow = 143,
                    maxTileRow = 174,
                    minTileCol = 240,
                    maxTileCol = 259
                ),
                epsg3857TileMatrix10 to WmtsTileMatrixLimits(
                    minTileRow = 286,
                    maxTileRow = 349,
                    minTileCol = 481,
                    maxTileCol = 518
                ),
                epsg3857TileMatrix11 to WmtsTileMatrixLimits(
                    minTileRow = 573,
                    maxTileRow = 698,
                    minTileCol = 962,
                    maxTileCol = 1036
                ),
                epsg3857TileMatrix12 to WmtsTileMatrixLimits(
                    minTileRow = 1146,
                    maxTileRow = 1397,
                    minTileCol = 1925,
                    maxTileCol = 2072
                ),
                epsg3857TileMatrix13 to WmtsTileMatrixLimits(
                    minTileRow = 2292,
                    maxTileRow = 2794,
                    minTileCol = 3851,
                    maxTileCol = 4144
                ),
                epsg3857TileMatrix14 to WmtsTileMatrixLimits(
                    minTileRow = 4584,
                    maxTileRow = 5589,
                    minTileCol = 7702,
                    maxTileCol = 8289
                ),
                epsg3857TileMatrix15 to WmtsTileMatrixLimits(
                    minTileRow = 9169,
                    maxTileRow = 11179,
                    minTileCol = 15404,
                    maxTileCol = 16579
                ),
                epsg3857TileMatrix16 to WmtsTileMatrixLimits(
                    minTileRow = 18338,
                    maxTileRow = 22359,
                    minTileCol = 30808,
                    maxTileCol = 33158
                ),
                epsg3857TileMatrix17 to WmtsTileMatrixLimits(
                    minTileRow = 36676,
                    maxTileRow = 44718,
                    minTileCol = 61616,
                    maxTileCol = 66316
                ),
                epsg3857TileMatrix18 to WmtsTileMatrixLimits(
                    minTileRow = 73353,
                    maxTileRow = 89436,
                    minTileCol = 123233,
                    maxTileCol = 132633
                ),
                epsg3857TileMatrix19 to WmtsTileMatrixLimits(
                    minTileRow = 146706,
                    maxTileRow = 178872,
                    minTileCol = 246467,
                    maxTileCol = 265266
                ),
                epsg3857TileMatrix20 to WmtsTileMatrixLimits(
                    minTileRow = 293412,
                    maxTileRow = 357745,
                    minTileCol = 492935,
                    maxTileCol = 530532
                ),
            )
        ),
        WmtsLayerConfig(
            identifier = "Leisure_27700",
            title = "OS Leisure",
            wgs84BoundingBox = WmtsBoundingBox(
                lowerCorner = NaiveCoordinate(-10.8342841886, 49.38802038505334),
                upperCorner = NaiveCoordinate(7.551552493184295, 62.2662683043619)
            ),
            set = epsg27700TileMatrixSet,
            matrices = mapOf(
                epsg27700TileMatrix0 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 6,
                    minTileCol = 0,
                    maxTileCol = 4
                ),
                epsg27700TileMatrix1 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 12,
                    minTileCol = 0,
                    maxTileCol = 9
                ),
                epsg27700TileMatrix2 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 24,
                    minTileCol = 0,
                    maxTileCol = 19
                ),
                epsg27700TileMatrix3 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 48,
                    minTileCol = 0,
                    maxTileCol = 39
                ),
                epsg27700TileMatrix4 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 96,
                    minTileCol = 0,
                    maxTileCol = 79
                ),
                epsg27700TileMatrix5 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 192,
                    minTileCol = 0,
                    maxTileCol = 158
                ),
                epsg27700TileMatrix6 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 384,
                    minTileCol = 0,
                    maxTileCol = 317
                ),
                epsg27700TileMatrix7 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 768,
                    minTileCol = 0,
                    maxTileCol = 635
                ),
                epsg27700TileMatrix8 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 1536,
                    minTileCol = 0,
                    maxTileCol = 1270
                ),
                epsg27700TileMatrix9 to WmtsTileMatrixLimits(
                    minTileRow = 0,
                    maxTileRow = 3072,
                    minTileCol = 0,
                    maxTileCol = 2541
                ),
            )
        )
    )
}
