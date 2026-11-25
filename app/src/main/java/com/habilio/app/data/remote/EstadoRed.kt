package com.habilio.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * ACTA #24: estado de la conexión que comparte toda la app.
 *
 * - [hayRed] lo actualiza [MonitorDeConexion] con los avisos del sistema (ConnectivityManager).
 * - [fallos] cuenta las peticiones que fallaron; la pantalla compara el valor antes y después de una carga
 *   para saber si algo no se pudo actualizar (y así NO reemplaza datos buenos por una lista vacía).
 * - [copiaLocal] indica que lo que se ve viene de la copia guardada en el teléfono ([CacheLocal]).
 * - [ultimoMensaje] es el último error explicado en lenguaje claro, para mostrarlo al usuario.
 *
 * Las propiedades observables son estado de Compose: la interfaz se redibuja sola cuando cambian.
 */
object EstadoRed {
    var hayRed by mutableStateOf(true)
    var copiaLocal by mutableStateOf(false)

    @Volatile var fallos: Int = 0
        private set

    @Volatile var ultimoMensaje: String? = null

    /** Se llama al empezar una recarga: limpia lo que se mostró en la carga anterior. */
    fun iniciarCiclo() {
        copiaLocal = false
        ultimoMensaje = null
    }

    @Synchronized
    fun registrarFallo(e: Exception) {
        fallos++
        ultimoMensaje = mensajeDeRed(e)
    }
}

/** true si el error se debe a la red (sin internet, tiempo agotado, servidor inaccesible). */
fun esFalloDeRed(e: Throwable): Boolean {
    var actual: Throwable? = e
    while (actual != null) {
        if (actual is IOException) return true
        actual = actual.cause
    }
    return false
}

/** Explica un error de red en palabras sencillas. */
fun mensajeDeRed(e: Throwable): String = when {
    e is UnknownHostException || e.cause is UnknownHostException ->
        "No hay conexión a internet."
    e is SocketTimeoutException || e.cause is SocketTimeoutException ->
        "La conexión está muy lenta y el servidor tardó demasiado en responder."
    e is ConnectException || e.cause is ConnectException ->
        "No se pudo conectar con el servidor de Habilio."
    e is SSLException ->
        "No se pudo establecer una conexión segura. Revisa la fecha y hora del teléfono."
    e.message?.startsWith("HTTP 5") == true ->
        "El servidor de Habilio tuvo un problema. Inténtalo en unos minutos."
    esFalloDeRed(e) -> "Se perdió la conexión."
    else -> "Ocurrió un error inesperado."
}

/** Explica una respuesta HTTP de error indicando qué se intentaba hacer (por ejemplo "subir la foto"). */
fun mensajeDeCodigoHttp(codigo: Int, accion: String): String = when (codigo) {
    400 -> "Algún dato no es válido y no se pudo $accion. Revisa la información."
    401 -> "Tu sesión expiró. Vuelve a iniciar sesión para $accion."
    403 -> "No tienes permiso para $accion."
    404 -> "No se encontró la información necesaria para $accion."
    409 -> "Esa información ya estaba registrada."
    413 -> "El archivo es demasiado grande para $accion."
    429 -> "Demasiados intentos seguidos. Espera un momento antes de volver a $accion."
    in 500..599 -> "El servidor de Habilio tuvo un problema al $accion. Inténtalo en unos minutos."
    else -> "No se pudo $accion (código $codigo)."
}
