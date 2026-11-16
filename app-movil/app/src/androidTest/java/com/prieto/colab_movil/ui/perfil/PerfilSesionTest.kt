package com.prieto.colab_movil.ui.perfil

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerfilSesionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun cerrarSesionEjecutaLaAccion() {
        var cierres = 0
        compose.setContent {
            MaterialTheme {
                PerfilEstudiante(nombre = "Alexis", email = "alexis@tecsup.edu.pe", onCerrarSesion = { cierres++ })
            }
        }

        compose.onNodeWithText("Alexis").assertIsDisplayed()
        compose.onNodeWithText("✉  alexis@tecsup.edu.pe").assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, cierres) }
    }
}