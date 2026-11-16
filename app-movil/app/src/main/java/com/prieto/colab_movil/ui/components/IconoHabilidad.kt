package com.prieto.colab_movil.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import androidx.compose.runtime.remember
import okhttp3.OkHttpClient
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.prieto.colab_movil.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val identificadorSeguro = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

private object CargadorIconos {
    @Volatile private var instancia: ImageLoader? = null

    fun obtener(contexto: Context): ImageLoader = instancia ?: synchronized(this) {
        instancia ?: ImageLoader.Builder(contexto.applicationContext).components {
            add(OkHttpNetworkFetcherFactory(callFactory = {
                OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build()
            }))
        }.build().also { instancia = it }
    }
}

internal fun urlIconoHabilidad(baseUrl: String, icono: String?): String? {
    if (icono == null || icono.length > 64 || !identificadorSeguro.matches(icono)) return null
    val origen = baseUrl.toHttpUrlOrNull() ?: return null
    if (origen.username.isNotEmpty() || origen.password.isNotEmpty()) return null
    return origen.newBuilder().encodedPath("/iconos/devicon/$icono.svg").query(null).fragment(null).build().toString()
}

@Composable
fun IconoHabilidad(
    nombre: String,
    icono: String?,
    modifier: Modifier = Modifier,
    tamanoInicial: TextUnit = 25.sp
) {
    val url = urlIconoHabilidad(BuildConfig.BASE_URL, icono)
    val contexto = LocalContext.current.applicationContext
    val cargador = remember(contexto) { CargadorIconos.obtener(contexto) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (url == null) {
            InicialHabilidad(nombre, tamanoInicial)
        } else {
            SubcomposeAsyncImage(
                model = url,
                imageLoader = cargador,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().testTag("icono-habilidad-$icono"),
                loading = { InicialHabilidad(nombre, tamanoInicial) },
                error = { Box(Modifier.testTag("icono-error")) { InicialHabilidad(nombre, tamanoInicial) } },
                success = { SubcomposeAsyncImageContent(modifier = Modifier.testTag("icono-cargado")) }
            )
        }
    }
}

@Composable
private fun InicialHabilidad(nombre: String, tamano: TextUnit) {
    Text(nombre.trim().take(1).uppercase().ifEmpty { "?" }, fontSize = tamano,
        color = Color(0xFF3525CD), fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag("icono-alternativo"))
}