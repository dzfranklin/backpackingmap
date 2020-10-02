package com.backpackingmap.backpackingmap.setup.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.FragmentRegisterBinding
import com.backpackingmap.backpackingmap.repository.RegisterError

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

        return binding.root
    }

    private fun cleanInputErrorsOnChange() {
        viewModel.email.observe(viewLifecycleOwner, {
            viewModel.hideEmailError()
        })

        viewModel.password.observe(viewLifecycleOwner, {
            viewModel.hidePasswordError()
        })
    }

    private fun bindInputErrors() {
        viewModel.error.observe(viewLifecycleOwner, { error ->
            when (error) {
                null -> null
                is RegisterError.Network -> binding.generalError.text =
                    getString(R.string.network_error, error.cause.localizedMessage)
                is RegisterError.Server -> binding.generalError.text =
                    getString(R.string.server_error, error.type)
                is RegisterError.Api -> {
                    val response = error.response
                    binding.generalError.text = response.message
                    response.field_errors.email?.let {
                        binding.emailLayout.error = it
                    }
                }
            }!!
        })
    }
}