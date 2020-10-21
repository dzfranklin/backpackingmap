package com.backpackingmap.backpackingmap.main_activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.ActivityMainBinding
import com.backpackingmap.backpackingmap.enforceLoggedIn
import com.backpackingmap.backpackingmap.map.Coordinate
import com.backpackingmap.backpackingmap.map.MapPosition
import com.backpackingmap.backpackingmap.map.MapView
import com.backpackingmap.backpackingmap.map.ZoomLevel
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

            val initialPosition = MapPosition(
                center = Coordinate(
                    CRSFactory().createFromName("EPSG:4326"),
                    -2.804904, 56.340259
                ),
                // Chosen because it's very close to the most zoomed in OS Leisure
                zoom = ZoomLevel(1.7)
            )

            map = MapView(
                context = applicationContext,
                parent = binding.mapParent,
                layerConfigs = model.mapLayerConfigs,
                initialPosition = initialPosition,
                repo = repo
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}