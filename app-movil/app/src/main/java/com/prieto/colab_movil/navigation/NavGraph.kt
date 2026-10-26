package com.prieto.colab_movil.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prieto.colab_movil.data.session.SessionManager
import com.prieto.colab_movil.ui.login.LoginScreen
import com.prieto.colab_movil.ui.perfil.PerfilScreen
import com.prieto.colab_movil.ui.register.RegisterScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Perfil : Screen("perfil")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { token ->
                    SessionManager.token = token
                    navController.navigate(Screen.Perfil.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                },
                onForgotPasswordClick = {
                    Toast.makeText(context, "Recuperar contraseña", Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { token ->
                    Toast.makeText(context, "¡Cuenta creada exitosamente!", Toast.LENGTH_LONG).show()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                },
                onRecoverPasswordClick = {
                    Toast.makeText(context, "Recuperar contraseña", Toast.LENGTH_SHORT).show()
                },
                onTermsClick = {
                    Toast.makeText(context, "Términos y condiciones TECSUP", Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable(Screen.Perfil.route) {
            PerfilScreen()
        }
    }
}
