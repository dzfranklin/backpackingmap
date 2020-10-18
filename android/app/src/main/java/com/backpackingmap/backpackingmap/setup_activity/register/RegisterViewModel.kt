package com.backpackingmap.backpackingmap.setup_activity.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.backpackingmap.backpackingmap.net.auth.RegisterResponseError
import com.backpackingmap.backpackingmap.repo.UnauthenticatedRemoteError
import com.backpackingmap.backpackingmap.repo.UnauthenticatedRepo
import kotlinx.coroutines.launch

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = UnauthenticatedRepo.fromApplication(application)

    val finished = MutableLiveData(false)
    val error = MutableLiveData<UnauthenticatedRemoteError<RegisterResponseError>>()

    val email = MutableLiveData("")
    val hideEmailError = MutableLiveData(true)
    val password = MutableLiveData("")
    val hidePasswordError = MutableLiveData(true)

    fun submit() {
        val email = email.value!!
        val password = password.value!!
        viewModelScope.launch {
            when (val response = repo.register(email, password)) {
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