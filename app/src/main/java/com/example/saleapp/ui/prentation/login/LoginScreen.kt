package com.example.saleapp.ui.prentation.login

import androidx.collection.emptyLongSet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun LoginScreen(viewModel: LoginViewModel= viewModel(),onLoginSuccess:()->Unit){
    val userId by viewModel.userId.collectAsState()
    val password by viewModel.password.collectAsState()


    Column (modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
        ) {

        Text(
            text = "Login",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        OutlinedTextField(value = userId,
            onValueChange = viewModel::onUserIdChange,
            label = { Text("User Id") }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        OutlinedTextField(value = password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") }
        )
        Modifier.padding(25.dp)
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Button(onClick = {
            if(viewModel.validateLogin()){
                onLoginSuccess()
            }
            else{
                //login failed
            }


        }
        ) { Text(text = "Login") }




    }



}


