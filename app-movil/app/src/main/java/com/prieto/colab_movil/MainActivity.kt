package com.prieto.colab_movil

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.prieto.colab_movil.ui.login.LoginScreen
import com.prieto.colab_movil.ui.theme.ColabmovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColabmovilTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginSuccess = { token ->
                            Toast.makeText(
                                this,
                                "¡Inicio de sesión exitoso! Token recibido",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onRegisterClick = {
                            Toast.makeText(
                                this,
                                "Navegar a Registro",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onForgotPasswordClick = {
                            Toast.makeText(
                                this,
                                "Recuperar contraseña",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}
