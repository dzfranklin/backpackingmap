package com.backpackingmap.backpackingmap.map_activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.enforceLoggedIn

class MapActivity: AppCompatActivity() {
    lateinit var model: MapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enforceLoggedIn(this)

        model = ViewModelProvider(this).get(MapViewModel::class.java)
    }
}