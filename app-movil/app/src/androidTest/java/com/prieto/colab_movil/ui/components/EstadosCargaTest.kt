package com.prieto.colab_movil.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EstadosCargaTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun muestraIndicadorDeCarga() {
        compose.setContent { MaterialTheme { EstadoCarga() } }
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun muestraErrorGeneralYEjecutaReintento() {
        var reintentos = 0
        compose.setContent { MaterialTheme { EstadoError(onReintentar = { reintentos++ }) } }
        compose.onNodeWithText("No se pudieron cargar los datos. Inténtalo de nuevo.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(1, reintentos) }
    }
}