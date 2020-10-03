package com.backpackingmap.backpackingmap.setup_activity.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.backpackingmap.backpackingmap.net.RegisterResponseError
import com.backpackingmap.backpackingmap.repo.RemoteError
import com.backpackingmap.backpackingmap.repo.Repo
import kotlinx.coroutines.launch

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = Repo.fromApplication(application)

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