package com.backpackingmap.backpackingmap.setup.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.backpackingmap.backpackingmap.net.AuthApi
import com.backpackingmap.backpackingmap.net.AuthTokens
import com.backpackingmap.backpackingmap.net.RegisterRequest
import com.backpackingmap.backpackingmap.net.RegisterRequestUser
import kotlinx.coroutines.launch
import timber.log.Timber

class RegisterViewModel : ViewModel() {
    private val _result = MutableLiveData<AuthTokens>()
    val result: LiveData<AuthTokens> = _result

    val email = MutableLiveData("")
    private val _emailError = MutableLiveData<String>()
    val emailError: LiveData<String> = _emailError

    val password = MutableLiveData("")
    private val _passwordError = MutableLiveData<String>()
    val passwordError: LiveData<String> = _passwordError

    fun submit() {
        val email = email.value!!
        val password = password.value!!
        viewModelScope.launch {
            val request = RegisterRequest(RegisterRequestUser(email, password))
            val response = AuthApi.service.register(request)

            Timber.i("Got response: $response")

            if (response.error != null) {
                val errors = response.error.field_errors
                if (errors.email != null) {
                    _emailError.value = errors.email.joinToString()
                }
                if (errors.password != null) {
                    _passwordError.value = errors.password.joinToString()
                }
            } else if (response.data != null) {
                _result.value = response.data
            } else {
                throw IllegalStateException("Response has neither error nor data")
            }
        }
    }

    fun clearPasswordError() {
        _passwordError.value = null
    }

    fun clearEmailError() {
        _emailError.value = null
    }
}