package com.prieto.colab_movil.ui.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
private val FieldBg = Color(0xFFFFFFFF)
private val FieldBorder = Color(0xFFE3E4F2)
private val ErrorBg = Color(0xFFFFD9D9)
private val ErrorRed = Color(0xFFB3141B)
private val ErrorFieldBg = Color(0xFFFFF4F4)
private val SuccessGreen = Color(0xFF1E8E4E)
private val TextDark = Color(0xFF1B1B2F)
private val TextGray = Color(0xFF5E6075)
private val ChipBg = Color(0xFFE7E8F6)

/** Nivel de seguridad: 0 (vacía) a 3 (alta). */
private fun passwordStrength(p: String): Int {
    if (p.isEmpty()) return 0
    val hasLetter = p.any { it.isLetter() }
    val hasDigit = p.any { it.isDigit() }
    val hasSpecial = p.any { !it.isLetterOrDigit() }
    var score = 0
    if (p.length >= 6 && hasLetter && hasDigit) score++
    if (p.length >= 8) score++
    if (p.length >= 10 && hasSpecial) score++
    return score.coerceAtLeast(if (p.isNotEmpty()) 1 else 0)
}

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onRegisterSuccess: (token: String) -> Unit = {},
    onLoginClick: () -> Unit = {},
    onRecoverPasswordClick: () -> Unit = {},
    onTermsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    val nameValid = name.trim().split(" ").filter { it.isNotBlank() }.size >= 2
    val strength = passwordStrength(password)
    val canSubmit = nameValid && email.endsWith("@tecsup.edu.pe") && strength >= 1 && acceptTerms && uiState !is RegisterUiState.Loading

    val isEmailRegistered = (uiState as? RegisterUiState.Error)?.isEmailRegistered == true
    val errorMessage = (uiState as? RegisterUiState.Error)?.message ?: ""

    LaunchedEffect(uiState) {
        if (uiState is RegisterUiState.Success) {
            val token = (uiState as RegisterUiState.Success).token
            onRegisterSuccess(token)
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
            ) { Text("C", color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(8.dp))
            Text("Colab", fontWeight = FontWeight.SemiBold, color = TextDark)
        }

        Spacer(Modifier.height(20.dp))
        Text("Crear cuenta", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(6.dp))
        Text(
            "Únete a la comunidad de desarrollo de proyectos de software en TECSUP.",
            color = TextGray,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(16.dp))

        // ---------- Banner: error o correo ya registrado ----------
        AnimatedVisibility(visible = uiState is RegisterUiState.Error) {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorBg)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.Error, contentDescription = null,
                            tint = ErrorRed, modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isEmailRegistered) "Correo ya registrado" else "Error de registro",
                                color = ErrorRed, fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (isEmailRegistered) "El correo ya está registrado. Si ya eres estudiante de TECSUP, intenta iniciar sesión o recuperar tu clave."
                                else errorMessage,
                                color = ErrorRed, fontSize = 13.sp, lineHeight = 18.sp
                            )
                        }
                    }
                    if (isEmailRegistered) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.padding(start = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.clickable(onClick = onLoginClick),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Iniciar sesión", color = Color(0xFF5A1015), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.ArrowForward, null, tint = Color(0xFF5A1015), modifier = Modifier.size(14.dp))
                            }
                            Text(
                                "Recuperar contraseña",
                                color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable(onClick = onRecoverPasswordClick)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ---------- Nombre y apellidos ----------
        LabelRow("Nombre y Apellidos") { Text("Obligatorio", color = TextGray, fontSize = 12.sp) }
        Spacer(Modifier.height(6.dp))
        TextField(
            value = name,
            onValueChange = { name = it; viewModel.resetState() },
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, FieldBorder), RoundedCornerShape(12.dp)),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Person, null, tint = TextGray) },
            trailingIcon = {
                if (nameValid) Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen)
            },
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(FieldBg),
            enabled = uiState !is RegisterUiState.Loading
        )

        Spacer(Modifier.height(16.dp))

        // ---------- Correo ----------
        LabelRow("Correo institucional TECSUP") {
            Text(
                "@tecsup.edu.pe",
                color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ChipBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        TextField(
            value = email,
            onValueChange = { email = it; viewModel.resetState() },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, if (isEmailRegistered) ErrorRed else FieldBorder),
                    RoundedCornerShape(12.dp)
                ),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Filled.Email, null, tint = if (isEmailRegistered) ErrorRed else TextGray)
            },
            trailingIcon = {
                if (isEmailRegistered) Icon(Icons.Filled.Warning, null, tint = ErrorRed)
            },
            textStyle = LocalTextStyleWithColor(if (isEmailRegistered) ErrorRed else TextDark),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(if (isEmailRegistered) ErrorFieldBg else FieldBg),
            enabled = uiState !is RegisterUiState.Loading
        )
        if (isEmailRegistered) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(ErrorRed))
                Spacer(Modifier.width(6.dp))
                Text("Esta cuenta ya cuenta con un perfil activo", color = ErrorRed, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------- Contraseña ----------
        LabelRow("Contraseña") { Text("Obligatorio", color = TextGray, fontSize = 12.sp) }
        Spacer(Modifier.height(6.dp))
        TextField(
            value = password,
            onValueChange = { password = it; viewModel.resetState() },
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, FieldBorder), RoundedCornerShape(12.dp)),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Lock, null, tint = TextGray) },
            trailingIcon = {
                Row(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChipBg)
                        .clickable { passwordVisible = !passwordVisible }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        null, tint = Primary, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (passwordVisible) "Ocultar" else "Mostrar",
                        color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(FieldBg),
            enabled = uiState !is RegisterUiState.Loading
        )

        Spacer(Modifier.height(12.dp))

        // ---------- Nivel de seguridad ----------
        if (password.isNotEmpty()) {
            val (label, barColor) = when (strength) {
                1 -> "Baja" to Color(0xFFE5484D)
                2 -> "Media" to SuccessGreen
                else -> "Alta" to SuccessGreen
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Nivel de seguridad: $label",
                    color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
                Text("$strength / 3", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i < strength) barColor else Color(0xFFDCDDEE))
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, null, tint = TextGray, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Mínimo 6 caracteres (incluye letras y números)",
                color = TextGray, fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---------- Términos ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = acceptTerms,
                onCheckedChange = { acceptTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = Primary),
                enabled = uiState !is RegisterUiState.Loading
            )
            Text(
                buildTermsText(),
                fontSize = 12.sp, lineHeight = 17.sp, color = TextDark,
                modifier = Modifier.clickable(onClick = onTermsClick)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---------- Botón ----------
        Button(
            onClick = {
                viewModel.registro(name, email, password)
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                disabledContainerColor = Primary.copy(alpha = 0.45f),
                disabledContentColor = Color.White
            )
        ) {
            if (uiState is RegisterUiState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Crear cuenta", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("¿Ya tienes cuenta? ", color = TextGray)
            Text(
                "Inicia sesión",
                color = Primary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onLoginClick)
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------- Helpers ----------
@Composable
private fun LabelRow(label: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 14.sp)
        trailing()
    }
}

@Composable
private fun fieldColors(container: Color) = TextFieldDefaults.colors(
    focusedContainerColor = container,
    unfocusedContainerColor = container,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = Primary
)

@Composable
private fun LocalTextStyleWithColor(color: Color) =
    LocalTextStyle.current.copy(color = color)

private fun buildTermsText() = androidx.compose.ui.text.buildAnnotatedString {
    append("Acepto los ")
    pushStyle(
        androidx.compose.ui.text.SpanStyle(
            color = Primary,
            fontWeight = FontWeight.SemiBold
        )
    )
    append("Términos y Condiciones")
    pop()
    append(" y la política de integridad académica de TECSUP.")
}

@Preview(showBackground = true, widthDp = 360, heightDp = 860)
@Composable
fun RegisterScreenPreview() {
    MaterialTheme { RegisterScreen() }
}
