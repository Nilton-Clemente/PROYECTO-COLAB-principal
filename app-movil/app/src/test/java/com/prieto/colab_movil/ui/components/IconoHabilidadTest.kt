package com.prieto.colab_movil.ui.components

import com.google.gson.Gson
import com.prieto.colab_movil.data.model.HabilidadDto
import com.prieto.colab_movil.data.model.TecnologiaDto
import org.junit.Assert.*
import org.junit.Test

class IconoHabilidadTest {
    @Test
    fun construyeRutaControladaYRechazaRutasArbitrarias() {
        assertEquals("http://localhost:8081/iconos/devicon/java.svg", urlIconoHabilidad("http://localhost:8081/", "java"))
        assertEquals("https://ejemplo.test/iconos/devicon/spring-boot.svg", urlIconoHabilidad("https://ejemplo.test/base/", "spring-boot"))
        assertNull(urlIconoHabilidad("http://usuario:clave@ejemplo.test/", "java"))
        assertNull(urlIconoHabilidad("file:///tmp/", "java"))
        listOf(null, "", "../java", "java.svg", "JAVA", "http://externo", "a/b", "a?x=1", "a".repeat(65)).forEach {
            assertNull(urlIconoHabilidad("http://localhost:8081/", it))
        }
    }

    @Test
    fun leeIconosOpcionalesSinRomperRespuestasAnteriores() {
        val gson = Gson()
        assertEquals("java", gson.fromJson("""{"id":1,"nombre":"Java","area":"Backend","icono":"java"}""", HabilidadDto::class.java).icono)
        assertNull(gson.fromJson("""{"id":1,"nombre":"Java","area":"Backend"}""", HabilidadDto::class.java).icono)
        assertEquals("react", gson.fromJson("""{"id":2,"nombre":"React","icono":"react"}""", TecnologiaDto::class.java).icono)
        assertNull(gson.fromJson("""{"id":2,"nombre":"React"}""", TecnologiaDto::class.java).icono)
    }
}