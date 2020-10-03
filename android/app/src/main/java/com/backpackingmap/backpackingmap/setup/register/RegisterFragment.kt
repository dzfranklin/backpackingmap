package com.backpackingmap.backpackingmap.setup.register

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.FragmentRegisterBinding
import com.backpackingmap.backpackingmap.map.MapActivity
import com.backpackingmap.backpackingmap.repository.RemoteError

class RegisterFragment : Fragment() {
    lateinit var binding: FragmentRegisterBinding
    lateinit var viewModel: RegisterViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this
        // Using the activity allows us to survive being re-created as a new fragment by the activity
        viewModel = ViewModelProvider(requireActivity()).get(RegisterViewModel::class.java)
        binding.viewModel = viewModel

        hideInputErrorsOnChange()
        bindErrors()
        exitWhenFinished()

        return binding.root
    }

    private fun hideInputErrorsOnChange() {
        viewModel.email.observe(viewLifecycleOwner, {
            viewModel.hideEmailError()
        })

        viewModel.password.observe(viewLifecycleOwner, {
            viewModel.hidePasswordError()
        })
    }

    private fun bindErrors() {
        viewModel.error.observe(viewLifecycleOwner, { error ->
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
                    response.field_errors.let { errors ->
                        errors.email?.let { binding.emailLayout.error = it }
                        errors.password?.let { binding.passwordLayout.error = it }
                    }
                }
            }!!
        })

        viewModel.hideEmailError.observe(viewLifecycleOwner,
            { binding.emailLayout.isErrorEnabled = !it })
        viewModel.hidePasswordError.observe(viewLifecycleOwner,
            { binding.passwordLayout.isErrorEnabled = !it })
    }

    private fun exitWhenFinished() {
        viewModel.finished.observe(viewLifecycleOwner, { finished ->
            if (finished) {
                val intent = Intent(activity, MapActivity::class.java)
                startActivity(intent)
            }
        })
    }
}