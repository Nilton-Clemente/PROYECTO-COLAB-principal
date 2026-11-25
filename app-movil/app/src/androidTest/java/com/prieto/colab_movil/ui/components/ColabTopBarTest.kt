package com.prieto.colab_movil.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prieto.colab_movil.ui.invitaciones.InvitacionesScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColabTopBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun muestraTituloYEjecutaLasAcciones() {
        var perfiles = 0
        var notificaciones = 0
        compose.setContent {
            MaterialTheme {
                ColabTopBar(onNotificaciones = { notificaciones++ }, onPerfil = { perfiles++ })
            }
        }

        compose.onNodeWithText("Colab").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mi perfil").assertHasClickAction().performClick()
        compose.onNodeWithContentDescription("Notificaciones").assertHasClickAction().performClick()
        compose.runOnIdle {
            assertEquals(1, perfiles)
            assertEquals(1, notificaciones)
        }
    }

    @Test
    fun iconoSuperiorDeInvitacionesAbrePerfil() {
        var perfiles = 0
        compose.setContent {
            MaterialTheme { InvitacionesScreen(onPerfilClick = { perfiles++ }) }
        }

        compose.onNodeWithContentDescription("Mi perfil").assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(1, perfiles) }
    }
}