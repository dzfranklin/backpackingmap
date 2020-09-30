package com.backpackingmap.backpackingmap.setup.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

class RegisterViewModel : ViewModel() {
    val email = MutableLiveData("email@")
    val emailError = MutableLiveData<String>()

    val password = MutableLiveData("")
    val passwordError = MutableLiveData<String>()

    init {
        Timber.i("viewModel init")
    }

    fun onSubmit() {
        emailError.value = "Email eeeeeror"
        passwordError.value = "Password eeeeeror"
    }
}