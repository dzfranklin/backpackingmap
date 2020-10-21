package com.backpackingmap.backpackingmap.setup_activity.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.databinding.FragmentLoginBinding
import com.backpackingmap.backpackingmap.main_activity.MainActivity
import com.backpackingmap.backpackingmap.repo.UnauthenticatedRemoteError

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    lateinit var model: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        // Using the activity allows us to survive being re-created as a new fragment by the activity
        model = ViewModelProvider(requireActivity()).get(LoginViewModel::class.java)
        binding.model = model

        bindErrors()
        exitWhenFinished()

        return binding.root
    }

    private fun bindErrors() {
        model.error.observe(viewLifecycleOwner, { error ->
            when (error) {
                null -> null
                is UnauthenticatedRemoteError.Network -> {
                    binding.mainError.text = getString(R.string.network_error)
                    binding.mainErrorDetail.text = error.cause.localizedMessage

                }
                is UnauthenticatedRemoteError.Server -> {
                    binding.mainError.text = getString(R.string.server_error)
                    binding.mainErrorDetail.text = error.type
                }
                is UnauthenticatedRemoteError.Api -> {
                    val response = error.response
                    binding.mainError.text = response.message
                    binding.mainErrorDetail.text = null
                }
            }!!
        })
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