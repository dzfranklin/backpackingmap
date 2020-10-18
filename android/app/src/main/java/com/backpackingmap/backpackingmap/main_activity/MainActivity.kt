package com.backpackingmap.backpackingmap.main_activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.ActivityMainBinding
import com.backpackingmap.backpackingmap.enforceLoggedIn
import com.backpackingmap.backpackingmap.map.*
import org.locationtech.proj4j.CRSFactory

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val model: MainActivityViewModel by lazy {
        ViewModelProvider(this).get(MainActivityViewModel::class.java)
    }
    lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enforceLoggedIn(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        model.repo?.let { repo ->
            val displayMetrics = resources.displayMetrics

            val extents = MapSize(
                screenWidth = Pixel(displayMetrics.widthPixels),
                screenHeight = Pixel(displayMetrics.heightPixels)
            )

            val initialPosition = MapPosition(
                center = Coordinate(
                    CRSFactory().createFromName("EPSG:4326"),
                    -2.804904, 56.340259
                ),
                zoom = ZoomLevel(42.0)
            )

            map = MapView(
                context = applicationContext,
                parent = binding.mapParent,
                service = model.mapService,
                layerConfigs = model.mapLayerConfigs,
                size = extents,
                initialPosition = initialPosition,
                repo = repo
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}