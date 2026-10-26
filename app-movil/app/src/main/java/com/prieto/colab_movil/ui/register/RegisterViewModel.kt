package com.prieto.colab_movil.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.AuthRepository
import com.prieto.colab_movil.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun registro(nombre: String, email: String, contrasena: String) {
        val trimmedNombre = nombre.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = contrasena.trim()

        if (trimmedNombre.isEmpty() || trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
            _uiState.value = RegisterUiState.Error("Por favor completa todos los campos obligatorios.")
            return
        }

        if (!trimmedEmail.endsWith("@tecsup.edu.pe")) {
            _uiState.value = RegisterUiState.Error("El correo debe pertenecer al dominio institucional @tecsup.edu.pe.")
            return
        }

        _uiState.value = RegisterUiState.Loading

        viewModelScope.launch {
            val result = authRepository.registro(trimmedNombre, trimmedEmail, trimmedPassword)
            result.onSuccess { authResponse ->
                _uiState.value = RegisterUiState.Success(token = authResponse.token)
            }.onFailure { exception ->
                if (exception.message == "EMAIL_ALREADY_REGISTERED") {
                    _uiState.value = RegisterUiState.Error(
                        message = "El correo ya está registrado.",
                        isEmailRegistered = true
                    )
                } else {
                    _uiState.value = RegisterUiState.Error(
                        message = exception.message ?: "Ocurrió un error inesperado al crear la cuenta."
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
