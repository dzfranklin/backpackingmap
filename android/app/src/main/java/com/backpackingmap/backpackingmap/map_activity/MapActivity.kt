package com.backpackingmap.backpackingmap.map_activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.ActivityMapBinding
import com.backpackingmap.backpackingmap.enforceLoggedIn

class MapActivity: AppCompatActivity() {
    lateinit var binding: ActivityMapBinding
    lateinit var model: MapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enforceLoggedIn(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_map)
        model = ViewModelProvider(this).get(MapViewModel::class.java)

        model.tile.observe(this, {
            it?.let {
                binding.imageView.setImageBitmap(it)
            }
        })
    }
}