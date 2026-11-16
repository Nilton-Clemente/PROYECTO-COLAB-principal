package com.prieto.colab_movil.ui.perfil

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prieto.colab_movil.data.model.HabilidadDto
import com.prieto.colab_movil.data.model.PerfilResponseDto
import com.prieto.colab_movil.data.repository.PerfilRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerfilErrorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun errorNoOfreceCerrarSesionYReintentaLaCarga() {
        val repository = RepositorioConError()
        val vm = PerfilViewModel(repository)
        compose.setContent { MaterialTheme { PerfilScreen(viewModel = vm) } }
        compose.waitUntil(5_000) { vm.uiState.value is PerfilUiState.Error }
        compose.onNodeWithText("No se pudieron cargar los datos. Inténtalo de nuevo.").assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").assertDoesNotExist()
        compose.onNodeWithText("Reintentar").performClick()
        compose.waitUntil(5_000) { repository.consultas >= 2 && vm.uiState.value is PerfilUiState.Error }
        compose.runOnIdle { assertEquals(2, repository.consultas) }
    }

    private class RepositorioConError : PerfilRepository {
        var consultas = 0
        override suspend fun obtenerPerfil(): Result<PerfilResponseDto> {
            consultas++
            return Result.failure(Exception("Detalle técnico"))
        }
        override suspend fun listarCatalogo(): Result<List<HabilidadDto>> = error("No se usa")
        override suspend fun agregarHabilidad(habilidadId: Long, nivel: String): Result<Unit> = error("No se usa")
        override suspend fun eliminarHabilidad(habilidadId: Long): Result<Unit> = error("No se usa")
    }
}