package com.backpackingmap.backpackingmap.setup_activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.ActivitySetupBinding
import com.backpackingmap.backpackingmap.setup_activity.login.LoginFragment
import com.backpackingmap.backpackingmap.setup_activity.register.RegisterFragment
import com.google.android.material.tabs.TabLayout

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding
    private val model: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setup)

        attachTabListener()
        updateOnTabChange()
    }

    private fun attachTabListener() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                // NOTE: There seems to be no clean way to get the tab selected
                // See <https://github.com/material-components/material-components-android/issues/1409>
                when (tab?.text) {
                    getString(R.string.register) -> model.registerSelected.value = true
                    getString(R.string.login) -> model.registerSelected.value = false
                    else -> throw IllegalStateException("Invalid tab selected")
                }
            }
        })
    }

    private fun updateOnTabChange() {
        model.registerSelected.observe(this, { registerSelected ->
            val transaction = supportFragmentManager.beginTransaction()
            if (registerSelected) {
                transaction.replace(R.id.setup_container, RegisterFragment())
            } else {
                transaction.replace(R.id.setup_container, LoginFragment())
            }
            transaction.commit()
        })
    }
}