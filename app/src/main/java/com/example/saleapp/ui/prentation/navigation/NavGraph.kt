package com.example.saleapp.ui.prentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.saleapp.ui.prentation.login.LoginScreen

@Composable
fun AppNavigation(navController: NavHostController= rememberNavController()){
    var navController = rememberNavController()
    NavHost(navController=navController, startDestination = "login"){
        composable("login"){
            LoginScreen()
        }

    }


}