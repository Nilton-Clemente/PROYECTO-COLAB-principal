package com.prieto.colab_movil.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
class ColabBottomBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun muestraSeleccionIndicadorYEjecutaAcciones() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                ColabBottomBar(
                    seleccionada = SeccionPrincipal.PROYECTOS,
                    onProyectos = { clicks.add("proyectos") },
                    onInvitaciones = { clicks.add("invitaciones") },
                    onPerfil = { clicks.add("perfil") },
                    hayInvitaciones = true
                )
            }
        }
        compose.onNodeWithText("Proyectos").assertIsSelected().performClick()
        compose.onNodeWithText("Invitaciones").assertIsNotSelected().performClick()
        compose.onNodeWithText("Mi Perfil").assertIsNotSelected().performClick()
        compose.onNodeWithContentDescription("Invitaciones pendientes", useUnmergedTree = true).assertExists()
        compose.runOnIdle { assertEquals(listOf("proyectos", "invitaciones", "perfil"), clicks) }
    }

    @Test
    fun seleccionaPerfilYNoMuestraIndicadorSiNoHayInvitaciones() {
        compose.setContent {
            MaterialTheme {
                ColabBottomBar(SeccionPrincipal.PERFIL, onProyectos = {}, onInvitaciones = {}, onPerfil = {})
            }
        }
        compose.onNodeWithText("Mi Perfil").assertIsSelected()
        compose.onNodeWithText("Proyectos").assertIsNotSelected()
        compose.onNodeWithContentDescription("Invitaciones pendientes", useUnmergedTree = true).assertDoesNotExist()
    }
}