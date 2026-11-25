package com.prieto.colab_movil.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColabActualizarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun gestoYAccionAccesibleRespetanHabilitacion() {
        var llamadas = 0
        var habilitado by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                ColabActualizar(false, habilitado, { llamadas++ }, Modifier.testTag("actualizar")) {
                    LazyColumn(Modifier.fillMaxSize()) { item { Text("Contenido") } }
                }
            }
        }
        compose.onNodeWithTag("actualizar").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(1, llamadas) }
        val accion = compose.onNodeWithTag("actualizar").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
            .first { it.label == "Actualizar" }
        compose.runOnIdle { accion.action() }
        compose.runOnIdle { assertEquals(2, llamadas); habilitado = false }
        compose.onNodeWithTag("actualizar").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(2, llamadas) }
    }
}