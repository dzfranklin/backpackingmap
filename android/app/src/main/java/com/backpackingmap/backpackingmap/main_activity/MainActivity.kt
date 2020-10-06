package com.backpackingmap.backpackingmap.main_activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.ActivityMainBinding
import com.backpackingmap.backpackingmap.enforceLoggedIn

class MainActivity: AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var model: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enforceLoggedIn(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        model = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        model.tile.observe(this, {
            it?.let {
                binding.imageView.setImageBitmap(it)
            }
        })
    }
}