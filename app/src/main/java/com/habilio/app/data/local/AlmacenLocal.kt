package com.habilio.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

// ============================================================================================
// ACTA #24 y #25: ALMACENAMIENTO LOCAL
// Todo se guarda en el almacenamiento privado de la app. El manifiesto tiene allowBackup="false"
// y las reglas de respaldo excluyen todos los dominios (Acta #19), así que nada de esto viaja a
// copias de seguridad en la nube ni a otro teléfono.
// ============================================================================================

/**
 * Copia local de las últimas consultas (GET) para poder mostrar información sin conexión.
 *
 * - Las respuestas públicas (lista de especialistas, portafolios, reseñas) van a `cacheDir/habilio_publico`.
 * - Las privadas (solicitudes de la cuenta, con direcciones) van a `cacheDir/habilio_privado` y se borran
 *   al cerrar sesión con [borrarDatosPrivados].
 * - Se conservan como máximo [MAX_ARCHIVOS] respuestas por carpeta; las más viejas se eliminan.
 * El sistema puede vaciar `cacheDir` si el teléfono se queda sin espacio: es una copia de apoyo, no la fuente.
 */
object CacheLocal {
    private const val MAX_ARCHIVOS = 40
    private var dirPublico: File? = null
    private var dirPrivado: File? = null

    fun iniciar(context: Context) {
        dirPublico = File(context.cacheDir, "habilio_publico").apply { mkdirs() }
        dirPrivado = File(context.cacheDir, "habilio_privado").apply { mkdirs() }
    }

    private fun nombre(url: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.take(40) + ".json"
    }

    @Synchronized
    fun guardar(url: String, cuerpo: String, privado: Boolean = false) {
        val dir = (if (privado) dirPrivado else dirPublico) ?: return
        try {
            File(dir, nombre(url)).writeText(cuerpo, Charsets.UTF_8)
            val archivos = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
            archivos.drop(MAX_ARCHIVOS).forEach { it.delete() }
        } catch (_: Exception) {
            // Sin espacio o sin permiso: la app sigue funcionando, solo que sin copia local.
        }
    }

    @Synchronized
    fun leer(url: String, privado: Boolean = false): String? {
        val dir = (if (privado) dirPrivado else dirPublico) ?: return null
        return try {
            File(dir, nombre(url)).takeIf { it.exists() }?.readText(Charsets.UTF_8)
        } catch (_: Exception) { null }
    }

    /** Al cerrar sesión: elimina toda la información privada de la cuenta guardada en el teléfono. */
    @Synchronized
    fun borrarDatosPrivados() {
        dirPrivado?.listFiles()?.forEach { it.delete() }
    }
}

/**
 * Borrador de un formulario (solicitud, trabajo del portafolio, edición de perfil). Se guarda mientras la
 * persona escribe y se recupera si falla la conexión o se cierra la app. Por privacidad nunca se guardan
 * contraseñas ni fotos; los valores se guardan como texto en SharedPreferences privadas.
 */
class BorradorLocal(context: Context, private val clave: String) {
    private val prefs = context.applicationContext.getSharedPreferences("habilio_borradores", Context.MODE_PRIVATE)

    fun guardar(vararg datos: Pair<String, Any>) {
        val obj = JSONObject()
        datos.forEach { (k, v) -> obj.put(k, v) }
        obj.put("_guardado", System.currentTimeMillis())
        prefs.edit().putString(clave, obj.toString()).apply()
    }

    private fun json(): JSONObject? = try {
        prefs.getString(clave, null)?.let { JSONObject(it) }
    } catch (_: Exception) { null }

    /** Valor de texto guardado, o null si no hay borrador o no tiene ese campo. */
    fun leer(campo: String): String? = json()?.takeIf { it.has(campo) }?.optString(campo)

    fun leerBool(campo: String, defecto: Boolean): Boolean =
        json()?.takeIf { it.has(campo) }?.optBoolean(campo, defecto) ?: defecto

    fun existe(): Boolean = json() != null

    fun borrar() {
        prefs.edit().remove(clave).apply()
    }
}

/**
 * Clave para que reintentar un envío NO lo duplique (idempotencia). Mientras el contenido sea el mismo,
 * [para] devuelve siempre el mismo UUID; si el contenido cambia, genera uno nuevo. Tras un envío exitoso
 * se llama a [liberar] para que el siguiente envío use una clave nueva.
 */
class ClaveIdempotente {
    private var contenido: String? = null
    private var clave: String? = null

    @Synchronized
    fun para(contenidoActual: String): String {
        if (contenidoActual != contenido || clave == null) {
            contenido = contenidoActual
            clave = UUID.randomUUID().toString()
        }
        return clave!!
    }

    @Synchronized
    fun liberar() {
        contenido = null
        clave = null
    }
}

/**
 * Cola de operaciones de seguridad (reportes de conducta, bloqueos y desbloqueos) que no se pudieron enviar
 * por falta de conexión. Se reintentan solas en la siguiente recarga ([SupabaseService.reenviarPendientesSeguridad]).
 * Un reporte nunca se pierde.
 */
object ColaPendientes {
    private const val PREFS = "habilio_pendientes"
    private const val CLAVE = "cola"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun leer(context: Context): List<JSONObject> = try {
        val arr = JSONArray(prefs(context).getString(CLAVE, "[]"))
        List(arr.length()) { arr.getJSONObject(it) }
    } catch (_: Exception) { emptyList() }

    @Synchronized
    fun reemplazar(context: Context, operaciones: List<JSONObject>) {
        val arr = JSONArray()
        operaciones.forEach { arr.put(it) }
        prefs(context).edit().putString(CLAVE, arr.toString()).apply()
    }

    @Synchronized
    fun agregar(context: Context, operacion: JSONObject) {
        val actuales = leer(context).toMutableList()
        // Un bloqueo y un desbloqueo pendientes de la misma persona se anulan: solo cuenta el último.
        val tipo = operacion.optString("tipo")
        if (tipo == "bloquear" || tipo == "desbloquear") {
            actuales.removeAll {
                (it.optString("tipo") == "bloquear" || it.optString("tipo") == "desbloquear") &&
                    it.optString("usuario") == operacion.optString("usuario") &&
                    it.optString("bloqueado") == operacion.optString("bloqueado")
            }
        }
        actuales.add(operacion.put("_creado", System.currentTimeMillis()))
        reemplazar(context, actuales)
    }

    /** Ids de personas que [usuarioId] desbloqueó sin conexión (aún no confirmado por el servidor). */
    fun desbloqueosPendientes(context: Context, usuarioId: String): List<String> =
        leer(context).filter { it.optString("tipo") == "desbloquear" && it.optString("usuario") == usuarioId }
            .map { it.optString("bloqueado") }
}

/**
 * Lista de personas bloqueadas guardada en el teléfono (por cuenta). Se aplica al instante, aun sin conexión,
 * y se sincroniza con la tabla `bloqueos` del servidor.
 */
object BloqueosLocales {
    private const val PREFS = "habilio_bloqueos"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun leer(context: Context, usuarioId: String): List<UsuarioBloqueado> = try {
        val arr = JSONArray(prefs(context).getString(usuarioId, "[]"))
        List(arr.length()) {
            val o = arr.getJSONObject(it)
            UsuarioBloqueado(o.optString("id"), o.optString("nombre", "Persona bloqueada"))
        }.filter { it.id.isNotBlank() }
    } catch (_: Exception) { emptyList() }

    @Synchronized
    private fun escribir(context: Context, usuarioId: String, lista: List<UsuarioBloqueado>): List<UsuarioBloqueado> {
        val arr = JSONArray()
        lista.distinctBy { it.id }.forEach { arr.put(JSONObject().put("id", it.id).put("nombre", it.nombre)) }
        prefs(context).edit().putString(usuarioId, arr.toString()).apply()
        return lista.distinctBy { it.id }
    }

    fun agregar(context: Context, usuarioId: String, persona: UsuarioBloqueado): List<UsuarioBloqueado> =
        escribir(context, usuarioId, leer(context, usuarioId).filter { it.id != persona.id } + persona)

    fun quitar(context: Context, usuarioId: String, bloqueadoId: String): List<UsuarioBloqueado> =
        escribir(context, usuarioId, leer(context, usuarioId).filter { it.id != bloqueadoId })

    /**
     * Une la lista del servidor con la local: lo bloqueado en cualquiera de los dos lados cuenta, excepto lo que
     * se desbloqueó sin conexión (todavía pendiente de enviar).
     */
    fun unirConServidor(
        context: Context, usuarioId: String, delServidor: List<String>, desbloqueosPendientes: List<String>
    ): List<UsuarioBloqueado> {
        val locales = leer(context, usuarioId)
        val nombres = locales.associate { it.id to it.nombre }
        val ids = (locales.map { it.id } + delServidor).distinct().filter { it !in desbloqueosPendientes }
        return escribir(context, usuarioId, ids.map { UsuarioBloqueado(it, nombres[it] ?: "Persona bloqueada") })
    }
}
