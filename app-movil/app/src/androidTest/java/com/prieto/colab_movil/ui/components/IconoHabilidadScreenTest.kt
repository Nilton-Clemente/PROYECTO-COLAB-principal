package com.prieto.colab_movil.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prieto.colab_movil.data.model.HabilidadDto
import com.prieto.colab_movil.ui.perfil.SeccionHabilidades
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconoHabilidadScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sinIdentificadorMuestraInicial() {
        compose.setContent { MaterialTheme { IconoHabilidad("Java", null, Modifier.size(48.dp)) } }
        compose.onNodeWithTag("icono-alternativo").assertIsDisplayed()
    }

    @Test
    fun identificadorInvalidoMuestraInicial() {
        compose.setContent { MaterialTheme { IconoHabilidad("Java", "../java", Modifier.size(48.dp)) } }
        compose.onNodeWithTag("icono-alternativo").assertIsDisplayed()
    }

    @Test
    fun archivoInexistenteTerminaMostrandoInicial() {
        compose.setContent { MaterialTheme { IconoHabilidad("Java", "no-existe", Modifier.size(48.dp)) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("icono-error").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("icono-alternativo").assertIsDisplayed()
    }

    @Test
    fun listaExpandidaIncluyeIconoYConservaDatos() {
        compose.setContent {
            MaterialTheme {
                SeccionHabilidades(listOf(HabilidadDto(1, "Java", "Backend")), true, {}, {})
            }
        }
        compose.onNodeWithTag("icono-alternativo").assertIsDisplayed()
        compose.onNodeWithTag("icono-habilidad-java").assertDoesNotExist()
    }
}