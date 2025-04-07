package com.example.saleapp.ui.prentation.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

class LoginViewModel:ViewModel() {
    private  val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    fun validateLogin():Boolean{

        val id = _userId.value.toIntOrNull() ?: return false
        if (password.value=="ABC${id}${id+1}") return true else return false

    }

    fun onUserIdChange(newUserId:String){_userId.value=newUserId}
    fun onPasswordChange(newPassword:String){_password.value=newPassword}



}