package com.backpackingmap.backpackingmap.setup.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.backpackingmap.backpackingmap.net.RegisterResponseError
import com.backpackingmap.backpackingmap.repository.RemoteError
import com.backpackingmap.backpackingmap.repository.Repository
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    val finished = MutableLiveData(false)
    val error = MutableLiveData<RemoteError<RegisterResponseError>>()

    val email = MutableLiveData("")
    val hideEmailError = MutableLiveData(true)
    val password = MutableLiveData("")
    val hidePasswordError = MutableLiveData(true)

    fun submit() {
        val email = email.value!!
        val password = password.value!!
        viewModelScope.launch {
            when (val response = Repository.register(email, password)) {
                null -> finished.value = true
                else -> {
                    error.value = response
                    hideEmailError.value = false
                    hidePasswordError.value = false
                }
            }
        }
    }

    fun hideEmailError() {
        hideEmailError.value = true
    }

    fun hidePasswordError() {
        hidePasswordError.value = true
    }
}