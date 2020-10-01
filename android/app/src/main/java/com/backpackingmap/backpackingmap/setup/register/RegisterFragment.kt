package com.backpackingmap.backpackingmap.setup.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.databinding.FragmentRegisterBinding
import timber.log.Timber

class RegisterFragment : Fragment() {
    lateinit var binding: FragmentRegisterBinding
    lateinit var viewModel: RegisterViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this
        // Using the activity allows us to survive being re-created as a new fragment by the activity
        viewModel = ViewModelProvider(requireActivity()).get(RegisterViewModel::class.java)
        binding.viewModel = viewModel

        cleanInputErrorsOnChange()
        bindInputErrors()

        return binding.root
    }

    private fun cleanInputErrorsOnChange() {
        viewModel.email.observe(viewLifecycleOwner, {
            viewModel.clearEmailError()
        })

        viewModel.password.observe(viewLifecycleOwner, {
            viewModel.clearPasswordError()
        })
    }

    private fun bindInputErrors() {
        viewModel.emailError.observe(viewLifecycleOwner, { error ->
            binding.emailLayout.error = error
        })

        viewModel.passwordError.observe(viewLifecycleOwner, { error ->
            binding.passwordLayout.error = error
        })
    }
}