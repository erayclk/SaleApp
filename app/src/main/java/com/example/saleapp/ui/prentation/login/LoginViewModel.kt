package com.example.saleapp.ui.presentation.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {
    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    fun validateLogin(): Boolean {
        val id = _userId.value.toIntOrNull() ?: return false
        return password.value == "ABC${id}${id + 1}"
    }

    fun onUserIdChange(newUserId: String) {
        _userId.value = newUserId
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }
}
