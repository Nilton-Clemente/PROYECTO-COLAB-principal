package com.prieto.colab_movil.ui.perfil

import org.junit.Assert.assertEquals
import org.junit.Test

class DescripcionAreaTest {
    @Test
    fun describeAreasConocidasYUsaAlternativaParaOtras() {
        assertEquals("Desarrollo de aplicaciones móviles.", descripcionArea(" Móvil "))
        assertEquals("Desarrollo de aplicaciones móviles.", descripcionArea("Movil"))
        assertEquals("Habilidades de esta categoría.", descripcionArea("Área nueva"))
    }
}