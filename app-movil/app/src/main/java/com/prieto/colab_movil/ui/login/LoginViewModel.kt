package com.prieto.colab_movil.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.AuthRepository
import com.prieto.colab_movil.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, contrasena: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = contrasena.trim()

        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
            _uiState.value = LoginUiState.Error("Por favor completa todos los campos.")
            return
        }

        if (!trimmedEmail.endsWith("@tecsup.edu.pe")) {
            _uiState.value = LoginUiState.Error("El correo debe pertenecer al dominio @tecsup.edu.pe.")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            val result = authRepository.login(trimmedEmail, trimmedPassword)
            result.onSuccess { authResponse ->
                _uiState.value = LoginUiState.Success(token = authResponse.token)
            }.onFailure { exception ->
                _uiState.value = LoginUiState.Error(
                    message = exception.message ?: "Ocurrió un error inesperado al iniciar sesión."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
