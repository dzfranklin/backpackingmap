package com.backpackingmap.backpackingmap.main_activity

import android.Manifest
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.backpackingmap.backpackingmap.*
import com.backpackingmap.backpackingmap.databinding.ActivityMainBinding
import com.backpackingmap.backpackingmap.map.ForegroundLocationProcessor
import com.backpackingmap.backpackingmap.map.MapView
import com.backpackingmap.backpackingmap.map.WmtsLayer
import com.backpackingmap.backpackingmap.map.ZoomLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext = Job()

    private lateinit var binding: ActivityMainBinding
    val model: MainActivityViewModel by viewModels()
    var map: MapView? = null
    private val locationProcessor =
        ForegroundLocationProcessor(this, ::requestFineLocation, coroutineContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        val repo = model.repo
        if (repo == null) {
            switchToSetup(this)
            return
        }

        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        map = MapView(
            context = applicationContext,
            initialCenter = NaiveCoordinate(-2.804904, 56.340259)
                .asWgs84()
                .convertTo("EPSG:27700"),
            // Chosen because it's very close to the most zoomed in OS Leisure
            initialZoom = ZoomLevel(MetersPerPixel(1.7)),
            locationProcessor = locationProcessor,
        ).apply {
            baseLayer.value =
                WmtsLayer.Builder(this@MainActivity, model.explorerLayerConfig, repo.tileRepo)
            binding.mapParent.addView(this, binding.mapParent.layoutParams)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel("MainActivity destroyed")
    }

    private fun requestFineLocation() {
        // Based on <https://medium.com/google-developer-experts/exploring-android-q-location-permissions-64d312b0e2e1>
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_FOREGROUND_LOCATION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode == REQUEST_CODE_FOREGROUND_LOCATION) {
            locationProcessor.onPermissionResult(permissions, grantResults)
        }
    }

    companion object {
        // application-specific, to identify request in callback
        private const val REQUEST_CODE_FOREGROUND_LOCATION = 10
    }
}