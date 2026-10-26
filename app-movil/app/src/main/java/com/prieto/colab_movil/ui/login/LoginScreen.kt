package com.prieto.colab_movil.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// ---------- Colores ----------
private val Primary = Color(0xFF3F2FD0)
private val ScreenBg = Color(0xFFF7F7FF)
private val FieldBg = Color(0xFFEEEFFB)
private val ErrorBg = Color(0xFFFFD6D6)
private val ErrorText = Color(0xFFB3141B)
private val SuccessGreen = Color(0xFF2E9E5B)
private val TextDark = Color(0xFF1B1B2F)
private val TextGray = Color(0xFF5E6075)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (token: String) -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var keepSession by remember { mutableStateOf(true) }

    val isEmailValid = email.endsWith("@tecsup.edu.pe") && email.length > "@tecsup.edu.pe".length

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            val token = (uiState as LoginUiState.Success).token
            onLoginSuccess(token)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ---------- Header ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text("C", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Text("Colab", fontWeight = FontWeight.SemiBold, color = TextDark)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Bienvenido a Colab",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Planifica y gestiona tus proyectos académicos de software en TECSUP.",
            color = TextGray,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )

        Spacer(Modifier.height(20.dp))

        // ---------- Banner de error / información ----------
        val showError = uiState is LoginUiState.Error
        val errorMessage = (uiState as? LoginUiState.Error)?.message ?: ""

        AnimatedVisibility(visible = showError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorBg)
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = ErrorText,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Acceso denegado", color = ErrorText, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        errorMessage,
                        color = ErrorText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                IconButton(onClick = { viewModel.resetState() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = ErrorText)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ---------- Tarjeta del formulario ----------
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {

                // Correo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Correo institucional", fontWeight = FontWeight.SemiBold, color = TextDark)
                    Text(
                        "@tecsup.edu.pe",
                        color = Primary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null, tint = TextGray) },
                    placeholder = { Text("usuario@tecsup.edu.pe") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    enabled = uiState !is LoginUiState.Loading
                )
                if (isEmailValid) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircleOutline,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Correo institucional TECSUP válido",
                            color = SuccessGreen,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Contraseña
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Contraseña", fontWeight = FontWeight.SemiBold, color = TextDark)
                    Text(
                        "¿Olvidaste tu contraseña?",
                        color = Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onForgotPasswordClick)
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextGray) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Mostrar u ocultar contraseña",
                                tint = TextGray
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    enabled = uiState !is LoginUiState.Loading
                )

                Spacer(Modifier.height(12.dp))

                // Mantener sesión
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = keepSession,
                        onCheckedChange = { keepSession = it },
                        colors = CheckboxDefaults.colors(checkedColor = Primary),
                        enabled = uiState !is LoginUiState.Loading
                    )
                    Text(
                        "Mantener sesión activa en este equipo",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Botón
                Button(
                    onClick = { viewModel.login(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = uiState !is LoginUiState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Iniciar sesión", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------- Registro ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("¿No tienes una cuenta aún? ", color = TextGray)
            Text(
                "Regístrate aquí",
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onRegisterClick)
            )
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FieldBg,
    unfocusedContainerColor = FieldBg,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = Primary
)

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun LoginScreenPreview() {
    MaterialTheme { LoginScreen() }
}
