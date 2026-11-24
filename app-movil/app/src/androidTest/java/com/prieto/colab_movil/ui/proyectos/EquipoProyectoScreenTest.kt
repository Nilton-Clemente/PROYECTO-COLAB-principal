package com.prieto.colab_movil.ui.proyectos

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EquipoProyectoScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun integrantesConservanRolYPendienteSinCheckNiEtiquetaActivo() {
        val equipo = listOf(
            IntegranteResponseDto(1, "creador@tecsup.edu.pe", "Creador", "CREADOR", "ACTIVO"),
            IntegranteResponseDto(2, "activo@tecsup.edu.pe", "Integrante activo", "INTEGRANTE", "ACTIVO"),
            IntegranteResponseDto(3, "pendiente@tecsup.edu.pe", "Integrante pendiente", "INTEGRANTE", "PENDIENTE")
        )
        compose.setContent { EquipoProyectoContent(equipoDto = equipo, puedeInvitar = false, onInvitar = {}) }

        compose.onNodeWithText("Creador", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Integrante activo", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Integrante pendiente", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("Propietario", useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodesWithText("Pendiente", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("Activo", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(" ✓", useUnmergedTree = true).assertDoesNotExist()
    }
}