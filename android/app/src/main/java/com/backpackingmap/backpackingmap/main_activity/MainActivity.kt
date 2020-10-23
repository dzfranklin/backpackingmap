package com.backpackingmap.backpackingmap.main_activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.ActivityMainBinding
import com.backpackingmap.backpackingmap.enforceLoggedIn
import com.backpackingmap.backpackingmap.map.*
import com.backpackingmap.backpackingmap.map.NaiveCoordinate

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val model: MainActivityViewModel by viewModels()
    lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enforceLoggedIn(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        model.repo?.let { repo ->
            val initialPosition = MapPosition(
                center = NaiveCoordinate(-2.804904, 56.340259).toCoordinate("EPSG:4326"),
                // Chosen because it's very close to the most zoomed in OS Leisure
                zoom = ZoomLevel(1.7f)
            )

            map = MapView(
                context = applicationContext,
                parent = binding.mapParent,
                layerConfigs = model.mapLayerConfigs,
                initialPosition = initialPosition,
            )
        }
    }

}