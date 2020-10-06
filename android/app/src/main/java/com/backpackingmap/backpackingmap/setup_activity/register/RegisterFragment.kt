package com.backpackingmap.backpackingmap.setup_activity.register

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.FragmentRegisterBinding
import com.backpackingmap.backpackingmap.main_activity.MainActivity
import com.backpackingmap.backpackingmap.repo.RemoteError

class RegisterFragment : Fragment() {
    lateinit var binding: FragmentRegisterBinding
    lateinit var model: RegisterViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this
        // Using the activity allows us to survive being re-created as a new fragment by the activity
        model = ViewModelProvider(requireActivity()).get(RegisterViewModel::class.java)
        binding.model = model

        hideInputErrorsOnChange()
        bindErrors()
        exitWhenFinished()

        return binding.root
    }

    private fun hideInputErrorsOnChange() {
        model.email.observe(viewLifecycleOwner, {
            model.hideEmailError()
        })

        model.password.observe(viewLifecycleOwner, {
            model.hidePasswordError()
        })
    }

    private fun bindErrors() {
        model.error.observe(viewLifecycleOwner, { error ->
            when (error) {
                null -> null
                is RemoteError.Network -> {
                    binding.mainError.text = getString(R.string.network_error)
                    binding.mainErrorDetail.text = error.cause.localizedMessage

                }
                is RemoteError.Server -> {
                    binding.mainError.text = getString(R.string.server_error)
                    binding.mainErrorDetail.text = error.type
                }
                is RemoteError.Api -> {
                    val response = error.response
                    binding.mainError.text = response.message
                    binding.mainErrorDetail.text = null
                    response.field_errors.let { errors ->
                        errors.email?.let { binding.emailLayout.error = it }
                        errors.password?.let { binding.passwordLayout.error = it }
                    }
                }
            }!!
        })

        model.hideEmailError.observe(viewLifecycleOwner,
            { binding.emailLayout.isErrorEnabled = !it })
        model.hidePasswordError.observe(viewLifecycleOwner,
            { binding.passwordLayout.isErrorEnabled = !it })
    }

    private fun exitWhenFinished() {
        model.finished.observe(viewLifecycleOwner, { finished ->
            if (finished) {
                val intent = Intent(activity, MainActivity::class.java)
                startActivity(intent)
            }
        })
    }
}