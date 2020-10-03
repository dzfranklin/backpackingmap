package com.backpackingmap.backpackingmap.setup_activity.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.backpackingmap.backpackingmap.net.auth.CreateSessionResponseError
import com.backpackingmap.backpackingmap.repo.RemoteError
import com.backpackingmap.backpackingmap.repo.Repo
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = Repo.fromApplication(application)

    val finished = MutableLiveData(false)
    val error = MutableLiveData<RemoteError<CreateSessionResponseError>>()

    val email = MutableLiveData("")
    val password = MutableLiveData("")

    fun submit() {
        val email = email.value!!
        val password = password.value!!
        viewModelScope.launch {
            when (val response = repo.login(email, password)) {
                null -> finished.value = true
                else -> error.value = response
            }
        }
    }
}