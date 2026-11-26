package com.prieto.colab_movil.ui.proyectos

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prieto.colab_movil.data.model.RequerimientoFuncionalDto
import com.prieto.colab_movil.data.model.RequerimientoNoFuncionalDto
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequerimientosProyectoScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun mostrar(
        funcionales: List<RequerimientoFuncionalDto> = emptyList(),
        noFuncionales: List<RequerimientoNoFuncionalDto> = emptyList()
    ) {
        compose.setContent {
            MaterialTheme { RequerimientosProyecto(funcionales, noFuncionales) }
        }
    }

    @Test
    fun separaLosTiposSinRepetirTitulosNiInventarPrioridades() {
        mostrar(
            listOf(
                RequerimientoFuncionalDto("RF-10", "Registrar productos"),
                RequerimientoFuncionalDto("RF-20", "Consultar productos")
            ),
            listOf(RequerimientoNoFuncionalDto("RNF-30", "Cifrar credenciales", "Seguridad"))
        )
        compose.onAllNodesWithText("Requerimientos funcionales (RF)").assertCountEquals(1)
        compose.onAllNodesWithText("Requerimientos no funcionales (RNF)").assertCountEquals(1)
        listOf("RF-10", "RF-20", "RNF-30", "Registrar productos", "Consultar productos", "Cifrar credenciales", "Seguridad")
            .forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        listOf("Requerimiento Funcional", "Requerimiento No Funcional", "Alta Prioridad", "Media Prioridad", "Rendimiento")
            .forEach { compose.onNodeWithText(it).assertDoesNotExist() }
    }

    @Test
    fun soloFuncionalesNoMuestraGrupoNoFuncional() {
        mostrar(funcionales = listOf(RequerimientoFuncionalDto(null, "Registrar productos")))
        compose.onNodeWithText("Requerimientos funcionales (RF)").assertIsDisplayed()
        compose.onNodeWithText("RF-01").assertIsDisplayed()
        compose.onNodeWithText("Requerimientos no funcionales (RNF)").assertDoesNotExist()
    }

    @Test
    fun soloNoFuncionalesSinCategoriaNoInventaClasificaciones() {
        mostrar(noFuncionales = listOf(
            RequerimientoNoFuncionalDto(null, "Responder en menos de dos segundos", null),
            RequerimientoNoFuncionalDto("RNF-09", "Admitir conexiones simultáneas", " ")
        ))
        compose.onNodeWithText("Requerimientos no funcionales (RNF)").assertIsDisplayed()
        compose.onNodeWithText("RNF-01").assertIsDisplayed()
        compose.onNodeWithText("RNF-09").assertIsDisplayed()
        compose.onNodeWithText("Responder en menos de dos segundos").assertIsDisplayed()
        compose.onNodeWithText("Admitir conexiones simultáneas").assertIsDisplayed()
        compose.onNodeWithText("Requerimientos funcionales (RF)").assertDoesNotExist()
        compose.onNodeWithText("Calidad y Rendimiento").assertDoesNotExist()
        compose.onNodeWithText(" ").assertDoesNotExist()
    }

    @Test
    fun sinRequerimientosNoMuestraGruposVacios() {
        mostrar()
        compose.onNodeWithText("Requerimientos funcionales (RF)").assertDoesNotExist()
        compose.onNodeWithText("Requerimientos no funcionales (RNF)").assertDoesNotExist()
    }
}