package com.prieto.colab_movil.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColabBarraSecundariaTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun muestraTituloYPermiteVolver() {
        var regresos = 0
        compose.setContent { MaterialTheme { ColabBarraSecundaria("Mis tareas", { regresos++ }) } }
        compose.onNodeWithText("Mis tareas").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithContentDescription("Mi perfil").assertDoesNotExist()
        compose.onNodeWithContentDescription("Actualizar tareas").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, regresos) }
    }

    @Test
    fun volverDeshabilitadoNoEjecutaLaAccion() {
        var regresos = 0
        compose.setContent { MaterialTheme { ColabBarraSecundaria("Editar", { regresos++ }, habilitado = false) } }
        compose.onNodeWithContentDescription("Volver").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, regresos) }
    }
}