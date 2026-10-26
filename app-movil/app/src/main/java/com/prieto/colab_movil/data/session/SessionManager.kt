package com.prieto.colab_movil.data.session

/**
 * Guarda en memoria el token JWT obtenido al iniciar sesión.
 * Se usa para añadir el header Authorization a las peticiones autenticadas.
 */
object SessionManager {
    var token: String? = null
}
