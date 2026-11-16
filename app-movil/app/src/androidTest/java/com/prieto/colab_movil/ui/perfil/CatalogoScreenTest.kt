package com.prieto.colab_movil.ui.perfil

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prieto.colab_movil.data.model.HabilidadDto
import com.prieto.colab_movil.data.model.PerfilResponseDto
import com.prieto.colab_movil.data.repository.PerfilRepository
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogoScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun buscaSeleccionaNivelYAnadeSinDuplicar() {
        val repo = Repositorio()
        val pendiente = CompletableDeferred<Result<Unit>>()
        repo.pendiente = pendiente
        val vm = CatalogoViewModel(repo)
        compose.setContent { MaterialTheme { CatalogoScreen(onBack = {}, viewModel = vm) } }
        compose.waitUntil(5_000) { vm.uiState.value is CatalogoUiState.Success }
        compose.onNodeWithText("Servicios y lógica de aplicaciones.").assertIsDisplayed()
        compose.onNodeWithText("Buscar habilidades").performTextInput("java")
        compose.onNodeWithText("React").assertDoesNotExist()
        compose.onNodeWithText("Java").performClick()
        compose.onNodeWithText("Añadir habilidad a mi perfil").assertIsNotEnabled()
        compose.onNodeWithText("Avanzado").performClick()
        compose.onNodeWithText("Añadir habilidad a mi perfil").performClick()
        compose.onNodeWithText("Guardando…").assertIsNotEnabled()
        compose.runOnIdle {
            vm.agregar(repo.catalogo.first(), NivelHabilidad.BASICO)
            assertEquals(1, repo.envios)
            assertEquals("AVANZADO", repo.nivel)
            repo.perfil = listOf(repo.catalogo.first().copy(nivel = repo.nivel))
            pendiente.complete(Result.success(Unit))
        }
        compose.waitUntil(5_000) { (vm.uiState.value as? CatalogoUiState.Success)?.perfil?.isNotEmpty() == true }
        compose.onNodeWithText("En tu perfil").assertIsDisplayed()
    }

    @Test
    fun falloInciertoSinPerfilImpideReenvioHastaConsultar() {
        val repo = Repositorio()
        val vm = CatalogoViewModel(repo)
        compose.waitUntil(5_000) { vm.uiState.value is CatalogoUiState.Success }
        compose.runOnIdle { repo.fallarPerfil = true; repo.fallarEnvio = true; vm.agregar(repo.catalogo.first(), NivelHabilidad.BASICO) }
        compose.waitUntil(5_000) { vm.uiState.value is CatalogoUiState.Error }
        assertTrue((vm.uiState.value as CatalogoUiState.Error).mensaje.contains("confirmar"))
        compose.runOnIdle { vm.agregar(repo.catalogo.first(), NivelHabilidad.BASICO) }
        assertEquals(1, repo.envios)
    }

    private class Repositorio : PerfilRepository {
        val catalogo = listOf(HabilidadDto(1, "Java", "Backend", "java"), HabilidadDto(2, "React", "Frontend"))
        var perfil = emptyList<HabilidadDto>()
        var nivel: String? = null
        var envios = 0
        var fallarPerfil = false
        var fallarEnvio = false
        var pendiente: CompletableDeferred<Result<Unit>>? = null
        override suspend fun listarCatalogo() = Result.success(catalogo)
        override suspend fun obtenerPerfil(): Result<PerfilResponseDto> = if (fallarPerfil) Result.failure(Exception("Error")) else Result.success(PerfilResponseDto("a@tecsup.edu.pe", "Ana", perfil))
        override suspend fun agregarHabilidad(habilidadId: Long, nivel: String): Result<Unit> {
            envios++
            this.nivel = nivel
            return pendiente?.await() ?: if (fallarEnvio) Result.failure(Exception("Error")) else Result.success(Unit)
        }
        override suspend fun eliminarHabilidad(habilidadId: Long): Result<Unit> = error("No se usa")
    }
}