package com.prieto.colab_movil.ui.perfil

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prieto.colab_movil.data.model.HabilidadDto
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabilidadesPerfilTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun permiteExpandirEliminarYMostrarAvisoDelCatalogo() {
        var eliminado: Long? = null
        compose.setContent {
            MaterialTheme {
                ContenidoPerfilRedisenado(
                    PerfilUiState.Success("Alexis", "alexis@tecsup.edu.pe", listOf(HabilidadDto(7, "Kotlin", "Mobile"))),
                    onEliminar = { eliminado = it }, onCerrarSesion = {}, paddingValues = PaddingValues(0.dp)
                )
            }
        }
        compose.onNodeWithText("1 habilidades").assertIsDisplayed()
        compose.onNodeWithText("Ver todas ›").performClick()
        compose.onNodeWithText("Mobile").assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()
        compose.runOnIdle { assertEquals(7L, eliminado) }
        compose.onNodeWithText("Explorar catálogo de habilidades").performScrollTo().performClick()
        compose.onNodeWithText("La pantalla del catálogo estará disponible próximamente.").assertIsDisplayed()
    }
}