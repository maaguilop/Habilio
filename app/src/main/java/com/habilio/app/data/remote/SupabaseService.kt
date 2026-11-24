package com.habilio.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

object SupabaseService {
    private const val PROJECT_URL = "https://zuqiuzdkobbomtoonace.supabase.co"
    private const val REST_URL = "$PROJECT_URL/rest/v1"
    private const val AUTH_URL = "$PROJECT_URL/auth/v1"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp1cWl1emRrb2Jib210b29uYWNlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTM1ODQ0NDEsImV4cCI6MjEwOTE2MDQ0MX0.0hCBoRTfYOYJmtUX6mgmGXm0eQTf1pbfk6anIGUONPg"

    private const val PREFIJO_CANCELADA = "[CANCELADA] "
    private const val PREFIJO_ESPERANDO = "[POR_CONFIRMAR] "

    private var tokenSesionActual: String? = null

    fun cerrarSesionToken() {
        tokenSesionActual = null
    }

    suspend fun marcarSolicitudEvaluada(solicitudId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply { put("fue_evaluada", true) }
                peticionHttp("$REST_URL/solicitudes?id=eq.$solicitudId", "PATCH", payload.toString()).first in 200..299
            } catch (_: Exception) {
                false
            }
        }

    /**
     * Acta #9: procesa la imagen elegida y MIDE lo que hizo.
     *  - Orientación: lee el giro EXIF (las fotos de celular suelen venir "de lado") y la endereza.
     *  - Resolución: nunca decodifica más de ~2560 px (evita falta de memoria) y entrega máximo 1280 px.
     *  - Compresión: JPEG empezando en calidad 80 y bajando hasta que pese menos de ~800 KB.
     * Devuelve los bytes finales y un [InformeImagen] para mostrar al usuario antes de publicar.
     */
    private class ImagenProcesada(val bytes: ByteArray, val informe: InformeImagen)

    private fun leerGiroExif(bytes: ByteArray): Int = try {
        val exif = android.media.ExifInterface(java.io.ByteArrayInputStream(bytes))
        when (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (_: Exception) { 0 }

    private fun procesarImagen(context: Context, uri: Uri): ImagenProcesada? {
        return try {
            val originales = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(originales, 0, originales.size, limites)
            val anchoOrig = limites.outWidth
            val altoOrig = limites.outHeight
            if (anchoOrig <= 0 || altoOrig <= 0) return null

            var muestra = 1
            while (max(anchoOrig, altoOrig) / muestra > 2560) muestra *= 2
            val decodificada = BitmapFactory.decodeByteArray(
                originales, 0, originales.size, BitmapFactory.Options().apply { inSampleSize = muestra }
            ) ?: return null

            val giro = leerGiroExif(originales)
            val enderezada = if (giro != 0) {
                val m = android.graphics.Matrix().apply { postRotate(giro.toFloat()) }
                Bitmap.createBitmap(decodificada, 0, 0, decodificada.width, decodificada.height, m, true)
                    .also { if (it != decodificada) decodificada.recycle() }
            } else decodificada

            val maxLado = 1280
            val lado = max(enderezada.width, enderezada.height)
            val finalBmp = if (lado > maxLado) {
                val escala = maxLado.toFloat() / lado.toFloat()
                Bitmap.createScaledBitmap(
                    enderezada, (enderezada.width * escala).toInt().coerceAtLeast(1),
                    (enderezada.height * escala).toInt().coerceAtLeast(1), true
                ).also { if (it != enderezada) enderezada.recycle() }
            } else enderezada

            var calidad = 80
            var bytes: ByteArray
            while (true) {
                val salida = ByteArrayOutputStream()
                finalBmp.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
                bytes = salida.toByteArray()
                if (bytes.size <= 800_000 || calidad <= 50) break
                calidad -= 10
            }
            val informe = InformeImagen(
                anchoOriginal = if (giro == 90 || giro == 270) altoOrig else anchoOrig,
                altoOriginal = if (giro == 90 || giro == 270) anchoOrig else altoOrig,
                bytesOriginales = originales.size.toLong(),
                anchoFinal = finalBmp.width, altoFinal = finalBmp.height,
                bytesFinales = bytes.size.toLong(), giroGrados = giro, calidadJpeg = calidad
            )
            finalBmp.recycle()
            ImagenProcesada(bytes, informe)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun comprimirImagenJpeg(context: Context, uri: Uri): ByteArray? = procesarImagen(context, uri)?.bytes

    /** Vista previa del resultado del procesamiento, para que la persona vea peso, resolución y giro antes de publicar. */
    suspend fun analizarImagen(context: Context, uri: Uri): InformeImagen? =
        withContext(Dispatchers.IO) { procesarImagen(context, uri)?.informe }

    suspend fun subirFotoStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bytesComprimidos = comprimirImagenJpeg(context, uri) ?: return@withContext null

            val nombreArchivo = "${UUID.randomUUID()}.jpg"
            val urlStorage = "$PROJECT_URL/storage/v1/object/evidencias/$nombreArchivo"
            val bearer = tokenSesionActual ?: ANON_KEY

            val conn = (URL(urlStorage).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 20000
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $bearer")
                setRequestProperty("Content-Type", "image/jpeg")
                doOutput = true
            }

            conn.outputStream.use { it.write(bytesComprimidos) }

            if (conn.responseCode in 200..299) {
                "$PROJECT_URL/storage/v1/object/public/evidencias/$nombreArchivo"
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun peticionHttp(
        urlCompleta: String,
        metodo: String = "GET",
        cuerpoJson: String? = null,
        preferReturn: Boolean = true
    ): Pair<Int, String> {
        val bearer = tokenSesionActual ?: ANON_KEY
        val conn = (URL(urlCompleta).openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("apikey", ANON_KEY)
            setRequestProperty("Authorization", "Bearer $bearer")
            setRequestProperty("Content-Type", "application/json")
            if (preferReturn) setRequestProperty("Prefer", "return=representation")
        }
        if (cuerpoJson != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(cuerpoJson.toByteArray(Charsets.UTF_8)) }
        }
        val codigo = conn.responseCode
        val stream = if (codigo in 200..299) conn.inputStream else conn.errorStream
        val respuesta = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText) ?: ""
        return codigo to respuesta
    }

    private fun normalizarCorreo(identificador: String, telefonoFallback: String = ""): String {
        val limpio = identificador.trim()
        if (limpio.contains("@")) return limpio.lowercase()
        val soloDigitos = limpio.ifBlank { telefonoFallback }.filter { it.isDigit() }
        return "${soloDigitos.ifBlank { "usuario" }}@habilio.app"
    }

    private fun limpiarUrlJson(obj: JSONObject, campo: String): String? {
        if (obj.isNull(campo)) return null
        val valor = obj.optString(campo, "").trim()
        return if (valor.isBlank() || valor.equals("null", ignoreCase = true)) null else valor
    }

    private fun parsearPerfil(obj: JSONObject) = PerfilUsuario(
        id = obj.optString("id", ""),
        rol = obj.optString("rol", "cliente"),
        nombre = obj.optString("nombre", "Usuario"),
        telefono = obj.optString("telefono", ""),
        correo = obj.optString("correo", ""),
        zona = obj.optString("zona", "Pénjamo, Gto.").ifBlank { "Pénjamo, Gto." },
        direccion = obj.optString("direccion", "Zona Centro, Pénjamo, Gto.").ifBlank { "Zona Centro, Pénjamo, Gto." },
        nombreTaller = obj.optString("nombre_taller", ""),
        oficio = obj.optString("oficio", "Albañilería"),
        especialidad = obj.optString("especialidad", ""),
        modalidad = obj.optString("modalidad", "A Domicilio"),
        descripcion = obj.optString("descripcion", ""),
        esPremium = obj.optBoolean("es_premium", false),
        calificacion = obj.optDouble("calificacion", 0.0).let { if (it.isNaN()) 0.0 else it },
        fotoUrl = limpiarUrlJson(obj, "foto_url") ?: "",
        rangoEdad = limpiarUrlJson(obj, "rango_edad") ?: "",
        aniosExperiencia = obj.optInt("anios_experiencia", 0),
        disponibilidad = limpiarUrlJson(obj, "disponibilidad") ?: "",
        precioDesde = obj.optDouble("precio_desde", 0.0).let { if (it.isNaN()) 0.0 else it },
        visibleTalentoMujeres = obj.optBoolean("visible_talento_mujeres", false),
        autorizacionEstado = limpiarUrlJson(obj, "autorizacion_estado") ?: ""
    )

    /** Fecha y hora UTC en formato ISO 8601 para registrar la aceptación del aviso de privacidad (RNF-05). */
    private fun marcaDeTiempoActual(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())

    suspend fun registrarUsuario(
        rol: String,
        nombre: String,
        telefono: String,
        correoInput: String,
        contrasena: String,
        zona: String = "Pénjamo, Gto.",
        direccion: String = "Zona Centro, Pénjamo, Gto.",
        nombreTaller: String = "",
        oficio: String = "",
        especialidad: String = "",
        modalidad: String = "",
        fotoUrl: String? = null,
        rangoEdad: String = RangoEdad.MAYOR,
        autorizacionTutor: Boolean = false
    ): Result<PerfilUsuario> = withContext(Dispatchers.IO) {
        try {
            val digitosTel = telefono.filter { it.isDigit() }
            if (digitosTel.length < 10) {
                return@withContext Result.failure(Exception("Ingresa un número de teléfono válido de 10 dígitos."))
            }

            // RF-05 / RNF-05: reglas de edad antes de crear cualquier cuenta
            if (rangoEdad == RangoEdad.MENOR_15) {
                return@withContext Result.failure(
                    Exception("Habilio no permite crear cuentas a menores de 15 años.")
                )
            }
            if (rol == "trabajador" && rangoEdad == RangoEdad.MENOR_15_17) {
                if (!autorizacionTutor) {
                    return@withContext Result.failure(
                        Exception("Las personas de 15 a 17 años que ofrecen servicios necesitan la autorización de su madre, padre o persona tutora.")
                    )
                }
                if (oficio in OFICIOS_DE_RIESGO) {
                    return@withContext Result.failure(
                        Exception("Por ley, $oficio solo puede ejercerse desde los 18 años. Elige otro oficio.")
                    )
                }
            }

            val correoFinal = normalizarCorreo(correoInput.ifBlank { digitosTel }, digitosTel)
            val authPayload = JSONObject().apply {
                put("email", correoFinal)
                put("password", contrasena)
            }
            val (authCode, authResp) = peticionHttp("$AUTH_URL/signup", "POST", authPayload.toString(), false)

            var userId = ""
            if (authCode in 200..299 && authResp.isNotBlank()) {
                val jsonAuth = JSONObject(authResp)
                tokenSesionActual = jsonAuth.optString("access_token", "").ifBlank { null }
                userId = jsonAuth.optJSONObject("user")?.optString("id").orEmpty()
                    .ifBlank { jsonAuth.optString("id", "") }
            } else {
                val (loginCode, loginResp) = peticionHttp(
                    "$AUTH_URL/token?grant_type=password",
                    "POST",
                    authPayload.toString(),
                    false
                )
                if (loginCode in 200..299 && loginResp.isNotBlank()) {
                    val jsonLogin = JSONObject(loginResp)
                    tokenSesionActual = jsonLogin.optString("access_token", "").ifBlank { null }
                    userId = jsonLogin.optJSONObject("user")?.optString("id").orEmpty()
                } else {
                    return@withContext Result.failure(
                        Exception("Este correo o teléfono ya está registrado con otra contraseña.")
                    )
                }
            }

            if (userId.isBlank()) {
                return@withContext Result.failure(Exception("No se pudo crear la cuenta en Supabase Auth."))
            }

            val descripcionGenerada = if (rol == "trabajador") {
                val detalleEsp = if (especialidad.isNotBlank()) " Especialidad: ${especialidad.trim()}." else ""
                "Especialista en $oficio.$detalleEsp Atención $modalidad en $zona."
            } else ""

            val perfilPayload = JSONObject().apply {
                put("id", userId)
                put("rol", rol)
                put("nombre", nombre.trim())
                put("telefono", digitosTel)
                put("correo", correoFinal)
                put("zona", zona.trim().ifBlank { "Pénjamo, Gto." })
                put("direccion", direccion.trim().ifBlank { "Zona Centro, Pénjamo, Gto." })
                if (!fotoUrl.isNullOrBlank()) put("foto_url", fotoUrl)
                if (rol == "trabajador") {
                    put("nombre_taller", nombreTaller.trim())
                    put("oficio", oficio)
                    put("especialidad", especialidad.trim())
                    put("modalidad", modalidad)
                    put("descripcion", descripcionGenerada)
                    put("es_premium", false)
                    put("calificacion", 0.0)
                }
            }

            // Constancia de edad y aviso de privacidad (RF-05, RNF-05)
            val datosLegales = JSONObject().apply {
                put("rango_edad", if (rol == "cliente") RangoEdad.MAYOR else rangoEdad)
                put("aviso_privacidad_aceptado_en", marcaDeTiempoActual())
                if (rol == "trabajador") put("autorizacion_tutor", rangoEdad == RangoEdad.MENOR_15_17 && autorizacionTutor)
            }
            val perfilCompleto = JSONObject(perfilPayload.toString()).also { p ->
                datosLegales.keys().forEach { k -> p.put(k, datosLegales.get(k)) }
            }

            var (code, resp) = peticionHttp("$REST_URL/perfiles", "POST", perfilCompleto.toString())
            // Si la base aún no tiene las columnas nuevas, no se bloquea el registro: se reintenta sin ellas
            // (ejecuta el script SQL de la v0.2 para que la constancia quede guardada).
            if (code == 400 && (resp.contains("rango_edad") || resp.contains("aviso_privacidad") ||
                        resp.contains("autorizacion_tutor"))) {
                val reintento = peticionHttp("$REST_URL/perfiles", "POST", perfilPayload.toString())
                code = reintento.first
                resp = reintento.second
            }
            if (code in 200..299 && resp.isNotBlank()) {
                val arr = JSONArray(resp)
                if (arr.length() > 0) return@withContext Result.success(parsearPerfil(arr.getJSONObject(0)))
            }
            Result.failure(Exception("No se pudo guardar el perfil (Código $code): $resp"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza los datos del perfil existente (Cliente o Trabajador) en Supabase.
     */
    suspend fun actualizarPerfil(
        id: String,
        rol: String,
        nombre: String,
        telefono: String,
        zona: String,
        direccion: String,
        nombreTaller: String = "",
        oficio: String = "",
        especialidad: String = "",
        modalidad: String = "",
        fotoUrl: String? = null,
        rangoEdadActual: String = ""
    ): Result<PerfilUsuario> = withContext(Dispatchers.IO) {
        try {
            val digitosTel = telefono.filter { it.isDigit() }
            if (digitosTel.length < 10) {
                return@withContext Result.failure(Exception("El teléfono debe tener al menos 10 dígitos."))
            }
            if (rol == "trabajador" && rangoEdadActual == RangoEdad.MENOR_15_17 && oficio in OFICIOS_DE_RIESGO) {
                return@withContext Result.failure(
                    Exception("Por ley, $oficio solo puede ejercerse desde los 18 años. Elige otro oficio.")
                )
            }

            val payload = JSONObject().apply {
                put("nombre", nombre.trim())
                put("telefono", digitosTel)
                put("zona", zona.trim().ifBlank { "Pénjamo, Gto." })
                put("direccion", direccion.trim().ifBlank { "Zona Centro, Pénjamo, Gto." })
                if (!fotoUrl.isNullOrBlank()) {
                    put("foto_url", fotoUrl)
                }
                if (rol == "trabajador") {
                    val detalleEsp = if (especialidad.isNotBlank()) " Especialidad: ${especialidad.trim()}." else ""
                    val nuevaDescripcion = "Especialista en $oficio.$detalleEsp Atención $modalidad en $zona."
                    put("nombre_taller", nombreTaller.trim())
                    put("oficio", oficio)
                    put("especialidad", especialidad.trim())
                    put("modalidad", modalidad)
                    put("descripcion", nuevaDescripcion)
                }
            }

            val (code, resp) = peticionHttp("$REST_URL/perfiles?id=eq.$id", "PATCH", payload.toString())
            if (code in 200..299 && resp.isNotBlank()) {
                val arr = JSONArray(resp)
                if (arr.length() > 0) {
                    return@withContext Result.success(parsearPerfil(arr.getJSONObject(0)))
                }
            }
            Result.failure(Exception("No se pudo actualizar el perfil (Código $code)."))
        } catch (e: Exception) {
            Result.failure(Exception("Error de red al actualizar el perfil."))
        }
    }

    suspend fun iniciarSesion(identificador: String, contrasena: String): Result<PerfilUsuario> =
        withContext(Dispatchers.IO) {
            try {
                val limpio = identificador.trim()
                var correoParaAuth = normalizarCorreo(limpio)
                var perfilEncontrado: PerfilUsuario? = null

                val filtro = if (limpio.contains("@")) {
                    "correo=eq.${URLEncoder.encode(limpio.lowercase(), "UTF-8")}"
                } else {
                    "telefono=eq.${URLEncoder.encode(limpio.filter { it.isDigit() }, "UTF-8")}"
                }
                val (codeBusqueda, respBusqueda) = peticionHttp("$REST_URL/perfiles?$filtro&select=*")
                if (codeBusqueda in 200..299 && respBusqueda.isNotBlank()) {
                    val arr = JSONArray(respBusqueda)
                    if (arr.length() > 0) {
                        perfilEncontrado = parsearPerfil(arr.getJSONObject(0))
                        if (perfilEncontrado.correo.contains("@")) {
                            correoParaAuth = perfilEncontrado.correo.trim().lowercase()
                        }
                    }
                }

                val authPayload = JSONObject().apply {
                    put("email", correoParaAuth)
                    put("password", contrasena)
                }
                var (authCode, authResp) = peticionHttp(
                    "$AUTH_URL/token?grant_type=password",
                    "POST",
                    authPayload.toString(),
                    false
                )

                if (authCode !in 200..299 && perfilEncontrado != null) {
                    val correoTelefono = normalizarCorreo(perfilEncontrado.telefono)
                    if (correoTelefono != correoParaAuth) {
                        val segundoPayload = JSONObject().apply {
                            put("email", correoTelefono)
                            put("password", contrasena)
                        }
                        val segundoIntento = peticionHttp(
                            "$AUTH_URL/token?grant_type=password",
                            "POST",
                            segundoPayload.toString(),
                            false
                        )
                        authCode = segundoIntento.first
                        authResp = segundoIntento.second
                    }
                }

                if (authCode !in 200..299 || authResp.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Correo/teléfono o contraseña incorrectos. Verifica tus datos.")
                    )
                }

                val jsonAuth = JSONObject(authResp)
                tokenSesionActual = jsonAuth.optString("access_token", "").ifBlank { null }
                val uid = jsonAuth.optJSONObject("user")?.optString("id").orEmpty()

                if (uid.isNotBlank()) {
                    val (cPerfil, rPerfil) = peticionHttp("$REST_URL/perfiles?id=eq.$uid&select=*")
                    if (cPerfil in 200..299 && rPerfil.isNotBlank()) {
                        val arr = JSONArray(rPerfil)
                        if (arr.length() > 0) {
                            return@withContext Result.success(parsearPerfil(arr.getJSONObject(0)))
                        }
                    }
                }

                if (perfilEncontrado != null) {
                    return@withContext Result.success(perfilEncontrado)
                }

                Result.failure(Exception("No se encontró el perfil asociado a esta cuenta."))
            } catch (e: Exception) {
                Result.failure(Exception("Error de conexión al verificar credenciales."))
            }
        }

    suspend fun obtenerTrabajadores(): List<PerfilUsuario> = withContext(Dispatchers.IO) {
        try {
            val (code, resp) = peticionHttp("$REST_URL/perfiles?rol=eq.trabajador&order=creado_en.desc")
            if (code in 200..299 && resp.isNotBlank()) {
                val arr = JSONArray(resp)
                return@withContext List(arr.length()) { i -> parsearPerfil(arr.getJSONObject(i)) }
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun obtenerEvidencias(): List<EvidenciaItem> = withContext(Dispatchers.IO) {
        try {
            val (code, resp) = peticionHttp("$REST_URL/evidencias?order=creado_en.desc")
            if (code in 200..299 && resp.isNotBlank()) {
                val arr = JSONArray(resp)
                return@withContext List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    EvidenciaItem(
                        id = o.optString("id"),
                        trabajadorId = o.optString("trabajador_id"),
                        titulo = o.optString("titulo"),
                        ubicacion = o.optString("ubicacion", "Pénjamo, Gto."),
                        fechaPublicacion = o.optString("fecha_publicacion", "Reciente"),
                        fotoUrl = limpiarUrlJson(o, "foto_url"),
                        grupoId = limpiarUrlJson(o, "grupo_id"),
                        etiqueta = limpiarUrlJson(o, "etiqueta") ?: EtiquetaFoto.UNICA,
                        descripcion = limpiarUrlJson(o, "descripcion") ?: "",
                        tipoServicio = limpiarUrlJson(o, "tipo_servicio") ?: "",
                        fechaTrabajo = limpiarUrlJson(o, "fecha_trabajo") ?: "",
                        consentimientoImagenes = o.optBoolean("consentimiento_imagenes", false)
                    )
                }
            }
        } catch (_: Exception) {}
        emptyList()
    }

    /**
     * Publica un trabajo del portafolio (RF-02): una foto sola o un conjunto Antes / Después.
     * Sube todas las fotos (comprimidas, RNF-06) y después inserta todas las filas en una sola petición,
     * así un trabajo nunca queda publicado a medias. Máximo 4 fotos por trabajo.
     */
    suspend fun publicarTrabajo(
        context: Context,
        trabajadorId: String,
        titulo: String,
        ubicacion: String,
        fotos: List<FotoNueva>,
        descripcion: String = "",
        tipoServicio: String = "",
        fechaTrabajo: String = "",
        consentimientoImagenes: Boolean = false,
        onProgreso: (subidas: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Acta #8: sin consentimiento de quien aparece o es dueño del lugar no se publica ninguna imagen.
            if (!consentimientoImagenes) return@withContext false
            val fotosAPublicar = fotos.take(MAX_FOTOS_POR_TRABAJO)
            if (fotosAPublicar.isEmpty()) return@withContext false

            val urls = mutableListOf<Pair<String, String>>()
            onProgreso(0, fotosAPublicar.size)
            for ((indice, foto) in fotosAPublicar.withIndex()) {
                // Acta #9: cada foto se reintenta hasta 3 veces antes de dar el trabajo por fallido.
                var url: String? = null
                var intento = 0
                while (url == null && intento < 3) {
                    url = subirFotoStorage(context, foto.uri)
                    intento++
                }
                if (url == null) return@withContext false
                urls.add(foto.etiqueta to url)
                onProgreso(indice + 1, fotosAPublicar.size)
            }

            val grupoId = UUID.randomUUID().toString()
            val lugar = ubicacion.ifBlank { "Pénjamo, Gto." }
            val filas = JSONArray()
            urls.forEach { (etiqueta, url) ->
                filas.put(JSONObject().apply {
                    put("trabajador_id", trabajadorId)
                    put("titulo", titulo.trim())
                    put("ubicacion", lugar)
                    put("fecha_publicacion", "Publicado en $lugar")
                    put("foto_url", url)
                    put("grupo_id", grupoId)
                    put("etiqueta", etiqueta)
                    put("descripcion", descripcion.trim())
                    put("tipo_servicio", tipoServicio.trim())
                    put("fecha_trabajo", fechaTrabajo.trim())
                    put("consentimiento_imagenes", true)
                })
            }

            var respuestaEv = peticionHttp("$REST_URL/evidencias", "POST", filas.toString())
            // Si la base aún no tiene las columnas nuevas se reintenta sin ellas (ejecuta el script SQL v0.3).
            if (respuestaEv.first == 400) {
                val filasBasicas = JSONArray()
                for (i in 0 until filas.length()) {
                    val f = JSONObject(filas.getJSONObject(i).toString())
                    listOf("descripcion", "tipo_servicio", "fecha_trabajo", "consentimiento_imagenes").forEach { f.remove(it) }
                    filasBasicas.put(f)
                }
                respuestaEv = peticionHttp("$REST_URL/evidencias", "POST", filasBasicas.toString())
            }
            val exito = respuestaEv.first in 200..299
            if (exito) {
                val updatePayload = JSONObject().apply { put("es_premium", true) }
                peticionHttp("$REST_URL/perfiles?id=eq.$trabajadorId", "PATCH", updatePayload.toString())
            }
            exito
        } catch (_: Exception) {
            false
        }
    }

    suspend fun obtenerResenas(trabajadorId: String? = null): List<ResenaItem> = withContext(Dispatchers.IO) {
        try {
            val query = if (trabajadorId != null) {
                "$REST_URL/resenas?trabajador_id=eq.$trabajadorId&order=creado_en.desc"
            } else {
                "$REST_URL/resenas?order=creado_en.desc"
            }
            val (code, resp) = peticionHttp(query)
            if (code in 200..299 && resp.isNotBlank()) {
                val arr = JSONArray(resp)
                return@withContext List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    ResenaItem(
                        id = o.optString("id"),
                        trabajadorId = o.optString("trabajador_id"),
                        autorNombre = o.optString("autor_nombre", "Cliente"),
                        comentario = o.optString("comentario", ""),
                        estrellas = o.optInt("estrellas", 5),
                        fechaTexto = o.optString("fecha_texto", "Reciente • Servicio concluido"),
                        oculta = o.optBoolean("oculta", false),
                        solicitudId = limpiarUrlJson(o, "solicitud_id") ?: ""
                    )
                }.filter { !it.oculta }
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun publicarResena(
        trabajadorId: String,
        autorNombre: String,
        comentario: String,
        estrellas: Int,
        solicitudId: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Acta #14: solo se puede reseñar un servicio concluido y una sola vez.
            if (solicitudId.isNotBlank()) {
                val (cs, rs) = peticionHttp("$REST_URL/solicitudes?id=eq.$solicitudId&select=estado,fue_evaluada")
                if (cs in 200..299 && rs.isNotBlank()) {
                    val a = JSONArray(rs)
                    if (a.length() == 0) return@withContext false
                    val o = a.getJSONObject(0)
                    if (o.optString("estado") != "completado" || o.optBoolean("fue_evaluada", false)) {
                        return@withContext false
                    }
                }
            }
            val payload = JSONObject().apply {
                put("trabajador_id", trabajadorId)
                put("autor_nombre", autorNombre.trim())
                put("comentario", comentario.trim())
                put("estrellas", estrellas)
                put("fecha_texto", "Hace un momento • Servicio concluido")
                if (solicitudId.isNotBlank()) put("solicitud_id", solicitudId)
            }
            var (code, _) = peticionHttp("$REST_URL/resenas", "POST", payload.toString())
            if (code == 400 && solicitudId.isNotBlank()) {
                payload.remove("solicitud_id")
                code = peticionHttp("$REST_URL/resenas", "POST", payload.toString()).first
            }
            if (code in 200..299) {
                val todas = obtenerResenas(trabajadorId)
                if (todas.isNotEmpty()) {
                    val redondeado = Math.round(todas.map { it.estrellas }.average() * 10.0) / 10.0
                    val updatePayload = JSONObject().apply { put("calificacion", redondeado) }
                    peticionHttp("$REST_URL/perfiles?id=eq.$trabajadorId", "PATCH", updatePayload.toString())
                }
                return@withContext true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun crearSolicitud(
        clienteId: String,
        trabajadorId: String,
        oficioEtiqueta: String,
        descripcion: String,
        ubicacion: String,
        monto: Double,
        esUrgente: Boolean = false,
        fotosProblema: List<String> = emptyList()
    ): Result<SolicitudItem> = withContext(Dispatchers.IO) {
        try {
            // Acta #10: datos mínimos obligatorios, validados también aquí (no solo en la pantalla).
            if (descripcion.trim().length < 15) {
                return@withContext Result.failure(Exception("Describe el problema con al menos 15 caracteres."))
            }
            if (ubicacion.trim().length < 5) {
                return@withContext Result.failure(Exception("Indica la dirección o colonia donde se hará el trabajo."))
            }
            val payload = JSONObject().apply {
                put("cliente_id", clienteId)
                put("trabajador_id", trabajadorId)
                put("oficio_etiqueta", oficioEtiqueta)
                put("descripcion", descripcion.trim())
                put("ubicacion", ubicacion.trim())
                put("monto", monto)
                put("es_urgente", esUrgente)
                put("estado", "pendiente")
                if (fotosProblema.isNotEmpty()) put("fotos_problema", JSONArray(fotosProblema))
            }
            var (code, resp) = peticionHttp("$REST_URL/solicitudes", "POST", payload.toString())
            if (code == 400 && payload.has("fotos_problema")) {
                payload.remove("fotos_problema")
                val r2 = peticionHttp("$REST_URL/solicitudes", "POST", payload.toString())
                code = r2.first; resp = r2.second
            }
            if (code in 200..299 && resp.isNotBlank()) {
                val arr = JSONArray(resp)
                if (arr.length() > 0) {
                    val o = arr.getJSONObject(0)
                    return@withContext Result.success(
                        SolicitudItem(
                            id = o.optString("id"),
                            clienteId = o.optString("cliente_id"),
                            trabajadorId = o.optString("trabajador_id"),
                            oficioEtiqueta = o.optString("oficio_etiqueta"),
                            descripcion = o.optString("descripcion"),
                            ubicacion = o.optString("ubicacion"),
                            monto = o.optDouble("monto", monto),
                            esUrgente = o.optBoolean("es_urgente", false),
                            estado = o.optString("estado", "pendiente"),
                            fotosProblema = fotosDeJson(o.optJSONArray("fotos_problema")),
                            creadoEn = o.optString("creado_en", "")
                        )
                    )
                }
            }
            Result.failure(Exception("No se pudo crear la solicitud: $resp"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerSolicitudesUsuario(usuarioId: String, esTrabajador: Boolean): List<SolicitudItem> =
        withContext(Dispatchers.IO) {
            try {
                val campoFiltro = if (esTrabajador) "trabajador_id" else "cliente_id"
                val (code, resp) = peticionHttp("$REST_URL/solicitudes?$campoFiltro=eq.$usuarioId&order=creado_en.desc")
                val (codePerfiles, respPerfiles) = peticionHttp("$REST_URL/perfiles?select=id,nombre,oficio")

                val mapaNombres = mutableMapOf<String, Pair<String, String>>()
                if (codePerfiles in 200..299 && respPerfiles.isNotBlank()) {
                    val arrP = JSONArray(respPerfiles)
                    for (i in 0 until arrP.length()) {
                        val p = arrP.getJSONObject(i)
                        mapaNombres[p.optString("id")] =
                            p.optString("nombre", "Usuario") to p.optString("oficio", "Especialista")
                    }
                }

                if (code in 200..299 && resp.isNotBlank()) {
                    val arr = JSONArray(resp)
                    return@withContext List(arr.length()) { i ->
                        val o = arr.getJSONObject(i)
                        val cId = o.optString("cliente_id")
                        val tId = o.optString("trabajador_id")
                        val estadoCrudo = o.optString("estado", "pendiente")
                        val descCruda = o.optString("descripcion", "")

                        val (estadoReal, descLimpia) = when {
                            descCruda.startsWith(PREFIJO_CANCELADA) -> {
                                "cancelada" to descCruda.removePrefix(PREFIJO_CANCELADA)
                            }
                            descCruda.startsWith(PREFIJO_ESPERANDO) && estadoCrudo == "en_ejecucion" -> {
                                "esperando_confirmacion" to descCruda.removePrefix(PREFIJO_ESPERANDO)
                            }
                            else -> {
                                estadoCrudo to descCruda.removePrefix(PREFIJO_ESPERANDO).removePrefix(PREFIJO_CANCELADA)
                            }
                        }

                        SolicitudItem(
                            id = o.optString("id"),
                            clienteId = cId,
                            trabajadorId = tId,
                            oficioEtiqueta = o.optString("oficio_etiqueta", "Servicio General"),
                            descripcion = descLimpia,
                            ubicacion = o.optString("ubicacion", "Pénjamo, Gto."),
                            monto = o.optDouble("monto", 0.0),
                            esUrgente = o.optBoolean("es_urgente", false),
                            estado = estadoReal,
                            fueEvaluada = o.optBoolean("fue_evaluada", false),
                            nombreCliente = mapaNombres[cId]?.first ?: "Cliente Local",
                            nombreTrabajador = mapaNombres[tId]?.first ?: "Especialista",
                            oficioTrabajador = mapaNombres[tId]?.second ?: o.optString("oficio_etiqueta"),
                            fotosProblema = fotosDeJson(o.optJSONArray("fotos_problema")),
                            creadoEn = o.optString("creado_en", "")
                        )
                    }
                }
            } catch (_: Exception) {}
            emptyList()
        }

    /** Cambia el estado y deja el evento correspondiente en el historial, avisando a la otra parte (Acta #12/#13). */
    suspend fun cambiarEstadoConEvento(solicitud: SolicitudItem, actorRol: String, nuevoEstado: String): Boolean {
        val ok = actualizarEstadoSolicitud(solicitud.id, nuevoEstado, solicitud.descripcion)
        if (ok) {
            val tipo = when (nuevoEstado) {
                "en_ejecucion" -> TipoEvento.ACEPTADA
                "rechazado" -> TipoEvento.RECHAZADA
                "esperando_confirmacion" -> TipoEvento.TERMINADA
                "completado" -> TipoEvento.CONFIRMADA
                "cancelada" -> TipoEvento.CANCELADA
                else -> null
            }
            if (tipo != null) {
                val destino = if (actorRol == "cliente") solicitud.trabajadorId else solicitud.clienteId
                registrarEvento(solicitud.id, actorRol, tipo, "", destino)
            }
        }
        return ok
    }

    suspend fun actualizarEstadoSolicitud(
        solicitudId: String,
        nuevoEstado: String,
        descripcionActual: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val descBase = descripcionActual
                ?.removePrefix(PREFIJO_ESPERANDO)
                ?.removePrefix(PREFIJO_CANCELADA)

            val payloadDirecto = JSONObject().apply {
                put("estado", nuevoEstado)
                if (descBase != null && (nuevoEstado == "completado" || nuevoEstado == "en_ejecucion")) {
                    put("descripcion", descBase)
                }
            }
            val (codeDirecto, _) = peticionHttp(
                "$REST_URL/solicitudes?id=eq.$solicitudId",
                "PATCH",
                payloadDirecto.toString()
            )
            if (codeDirecto in 200..299) return@withContext true

            val descActual = descBase ?: run {
                val (c, r) = peticionHttp("$REST_URL/solicitudes?id=eq.$solicitudId&select=descripcion")
                if (c in 200..299 && r.isNotBlank()) {
                    val arr = JSONArray(r)
                    if (arr.length() > 0) {
                        arr.getJSONObject(0).optString("descripcion", "")
                            .removePrefix(PREFIJO_ESPERANDO)
                            .removePrefix(PREFIJO_CANCELADA)
                    } else ""
                } else ""
            }

            val payloadFallback = when (nuevoEstado) {
                "cancelada" -> JSONObject().apply {
                    put("estado", "rechazado")
                    put("descripcion", "$PREFIJO_CANCELADA$descActual")
                }
                "esperando_confirmacion" -> JSONObject().apply {
                    put("estado", "en_ejecucion")
                    put("descripcion", "$PREFIJO_ESPERANDO$descActual")
                }
                else -> return@withContext false
            }

            peticionHttp(
                "$REST_URL/solicitudes?id=eq.$solicitudId",
                "PATCH",
                payloadFallback.toString()
            ).first in 200..299
        } catch (_: Exception) {
            false
        }
    }

    // ------------------------------------------------------------------------
    // ACTA #10: SUBIDA DE FOTOS DEL PROBLEMA
    // ------------------------------------------------------------------------
    private fun fotosDeJson(arr: JSONArray?): List<String> =
        if (arr == null) emptyList() else List(arr.length()) { arr.optString(it) }.filter { it.startsWith("http") }

    /** Sube las fotos del problema (máx. 3), con 3 reintentos por foto. Devuelve las URLs o null si alguna falla. */
    suspend fun subirFotosProblema(context: Context, uris: List<Uri>, onProgreso: (Int, Int) -> Unit = { _, _ -> }): List<String>? =
        withContext(Dispatchers.IO) {
            val urls = mutableListOf<String>()
            val lista = uris.take(3)
            onProgreso(0, lista.size)
            for ((i, uri) in lista.withIndex()) {
                var url: String? = null
                var intento = 0
                while (url == null && intento < 3) { url = subirFotoStorage(context, uri); intento++ }
                urls.add(url ?: return@withContext null)
                onProgreso(i + 1, lista.size)
            }
            urls
        }

    // ------------------------------------------------------------------------
    // ACTA #12 y #13: HISTORIAL DE EVENTOS Y AVISOS DENTRO DE LA APP
    // ------------------------------------------------------------------------
    /** Registra un evento en la línea de tiempo. Si la tabla aún no existe, falla en silencio (la app sigue funcionando). */
    suspend fun registrarEvento(
        solicitudId: String, actorRol: String, tipo: String, detalle: String = "", destinatarioId: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val p = JSONObject().apply {
                put("solicitud_id", solicitudId)
                put("actor_rol", actorRol)
                put("tipo", tipo)
                put("detalle", detalle.trim())
                if (destinatarioId.isNotBlank()) put("destinatario_id", destinatarioId)
                put("leido", false)
            }
            peticionHttp("$REST_URL/eventos_solicitud", "POST", p.toString()).first in 200..299
        } catch (_: Exception) { false }
    }

    suspend fun obtenerEventos(solicitudId: String): List<EventoSolicitud> = withContext(Dispatchers.IO) {
        try {
            val (c, r) = peticionHttp("$REST_URL/eventos_solicitud?solicitud_id=eq.$solicitudId&order=creado_en.asc")
            if (c in 200..299 && r.isNotBlank()) return@withContext parsearEventos(JSONArray(r))
        } catch (_: Exception) {}
        emptyList()
    }

    /** Avisos sin leer dirigidos a una persona (la pantalla los muestra como notificaciones internas). */
    suspend fun obtenerAvisosPendientes(usuarioId: String): List<EventoSolicitud> = withContext(Dispatchers.IO) {
        try {
            val (c, r) = peticionHttp("$REST_URL/eventos_solicitud?destinatario_id=eq.$usuarioId&leido=eq.false&order=creado_en.desc&limit=30")
            if (c in 200..299 && r.isNotBlank()) return@withContext parsearEventos(JSONArray(r))
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun marcarAvisosLeidos(usuarioId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            peticionHttp("$REST_URL/eventos_solicitud?destinatario_id=eq.$usuarioId&leido=eq.false", "PATCH",
                JSONObject().apply { put("leido", true) }.toString()).first in 200..299
        } catch (_: Exception) { false }
    }

    private fun parsearEventos(arr: JSONArray) = List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        EventoSolicitud(
            id = o.optString("id"), solicitudId = o.optString("solicitud_id"),
            actorRol = o.optString("actor_rol"), tipo = o.optString("tipo"),
            detalle = o.optString("detalle", ""), destinatarioId = o.optString("destinatario_id", ""),
            leido = o.optBoolean("leido", false), creadoEn = o.optString("creado_en", "")
        )
    }

    // ------------------------------------------------------------------------
    // ACTA #13: CANCELACIÓN CON MOTIVO Y PLAZOS
    // ------------------------------------------------------------------------
    val MOTIVOS_CANCELACION_CLIENTE = listOf(
        "Ya resolví el problema por otro medio", "El precio no me convenció", "Cambié la fecha o necesidad",
        "No hubo respuesta a tiempo", "Otro motivo"
    )
    val MOTIVOS_CANCELACION_TRABAJADOR = listOf(
        "No cuento con disponibilidad", "El trabajo no corresponde a mi oficio", "Zona fuera de mi cobertura",
        "No pudimos acordar el presupuesto", "Otro motivo"
    )
    /** Horas después de aceptada una solicitud en que aún puede cancelarse sin pasar por un desacuerdo formal. */
    const val PLAZO_CANCELACION_LIBRE_HORAS = 24L

    /** Cancela una solicitud guardando motivo, avisando a la otra parte y dejando el evento en el historial. */
    suspend fun cancelarSolicitud(
        solicitud: SolicitudItem, actorRol: String, motivo: String, comentario: String
    ): Boolean = withContext(Dispatchers.IO) {
        val motivoCompleto = if (comentario.isBlank()) motivo else "$motivo — ${comentario.trim()}"
        val ok = actualizarEstadoSolicitud(solicitud.id, "cancelada", solicitud.descripcion)
        if (ok) {
            // Se intenta guardar el motivo en su columna; si no existe todavía, queda en el historial de eventos.
            peticionHttp("$REST_URL/solicitudes?id=eq.${solicitud.id}", "PATCH",
                JSONObject().apply { put("motivo_cancelacion", motivoCompleto) }.toString())
            val destino = if (actorRol == "cliente") solicitud.trabajadorId else solicitud.clienteId
            registrarEvento(solicitud.id, actorRol, TipoEvento.CANCELADA, motivoCompleto, destino)
        }
        ok
    }

    /** True si la solicitud aún está dentro del plazo de cancelación libre (o nunca fue aceptada). */
    fun dentroDePlazoCancelacion(solicitud: SolicitudItem, eventos: List<EventoSolicitud>): Boolean {
        if (solicitud.estado == "pendiente") return true
        val aceptada = eventos.firstOrNull { it.tipo == TipoEvento.ACEPTADA } ?: return true
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val t = f.parse(aceptada.creadoEn.take(19))?.time ?: return true
            System.currentTimeMillis() - t <= PLAZO_CANCELACION_LIBRE_HORAS * 3600_000L
        } catch (_: Exception) { true }
    }

    // ------------------------------------------------------------------------
    // ACTA #11: PRESUPUESTOS (propuesta -> aceptación de la otra parte -> control de cambios)
    // ------------------------------------------------------------------------
    suspend fun obtenerPresupuestos(solicitudId: String): List<PresupuestoItem> = withContext(Dispatchers.IO) {
        try {
            val (c, r) = peticionHttp("$REST_URL/presupuestos?solicitud_id=eq.$solicitudId&order=creado_en.asc")
            if (c in 200..299 && r.isNotBlank()) {
                val arr = JSONArray(r)
                return@withContext List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    PresupuestoItem(
                        id = o.optString("id"), solicitudId = o.optString("solicitud_id"),
                        monto = o.optDouble("monto", 0.0), descripcion = o.optString("descripcion", ""),
                        condiciones = o.optString("condiciones", ""), propuestoPorRol = o.optString("propuesto_por_rol"),
                        estado = o.optString("estado", "pendiente"), creadoEn = o.optString("creado_en", "")
                    )
                }
            }
        } catch (_: Exception) {}
        emptyList()
    }

    /**
     * Propone un presupuesto. Reglas de control (Acta #11):
     *  - monto > 0, descripción y condiciones obligatorias;
     *  - solo se puede proponer mientras el servicio no esté concluido ni cancelado;
     *  - una propuesta nueva reemplaza a la pendiente anterior (queda en el historial como "reemplazado").
     */
    suspend fun proponerPresupuesto(
        solicitud: SolicitudItem, rolQuienPropone: String, monto: Double, descripcion: String, condiciones: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (monto <= 0.0) return@withContext Result.failure(Exception("El monto debe ser mayor a cero."))
            if (descripcion.trim().length < 10) return@withContext Result.failure(Exception("Describe qué incluye el presupuesto."))
            if (condiciones.trim().length < 5) return@withContext Result.failure(Exception("Indica las condiciones (materiales, forma de pago, tiempos)."))
            if (solicitud.estado in listOf("completado", "cancelada", "rechazado")) {
                return@withContext Result.failure(Exception("Este servicio ya no admite cambios de presupuesto."))
            }
            peticionHttp("$REST_URL/presupuestos?solicitud_id=eq.${solicitud.id}&estado=eq.pendiente", "PATCH",
                JSONObject().apply { put("estado", "reemplazado") }.toString())
            val p = JSONObject().apply {
                put("solicitud_id", solicitud.id)
                put("monto", monto)
                put("descripcion", descripcion.trim())
                put("condiciones", condiciones.trim())
                put("propuesto_por_rol", rolQuienPropone)
                put("estado", "pendiente")
            }
            val (c, r) = peticionHttp("$REST_URL/presupuestos", "POST", p.toString())
            if (c !in 200..299) return@withContext Result.failure(Exception("No se pudo guardar el presupuesto (Código $c): $r"))
            val destino = if (rolQuienPropone == "cliente") solicitud.trabajadorId else solicitud.clienteId
            registrarEvento(solicitud.id, rolQuienPropone, TipoEvento.PRESUPUESTO_PROPUESTO,
                "Propuesta de $${"%.2f".format(monto)} MXN", destino)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** La parte que NO propuso acepta o rechaza. Al aceptar, el monto de la solicitud se actualiza al monto acordado. */
    suspend fun responderPresupuesto(
        solicitud: SolicitudItem, presupuesto: PresupuestoItem, rolQuienResponde: String, acepta: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (presupuesto.propuestoPorRol == rolQuienResponde) {
                return@withContext Result.failure(Exception("Quien propone no puede aceptar su propia propuesta."))
            }
            val nuevo = if (acepta) "aceptado" else "rechazado"
            val (c, _) = peticionHttp("$REST_URL/presupuestos?id=eq.${presupuesto.id}&estado=eq.pendiente", "PATCH",
                JSONObject().apply { put("estado", nuevo) }.toString())
            if (c !in 200..299) return@withContext Result.failure(Exception("No se pudo registrar tu respuesta."))
            if (acepta) {
                peticionHttp("$REST_URL/solicitudes?id=eq.${solicitud.id}", "PATCH",
                    JSONObject().apply { put("monto", presupuesto.monto) }.toString())
            }
            registrarEvento(solicitud.id, rolQuienResponde,
                if (acepta) TipoEvento.PRESUPUESTO_ACEPTADO else TipoEvento.PRESUPUESTO_RECHAZADO,
                "$${"%.2f".format(presupuesto.monto)} MXN", if (rolQuienResponde == "cliente") solicitud.trabajadorId else solicitud.clienteId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ------------------------------------------------------------------------
    // ACTA #12: DESACUERDOS FORMALES
    // ------------------------------------------------------------------------
    val MOTIVOS_DESACUERDO = listOf(
        "El trabajo no quedó como se acordó", "El precio final es distinto al acordado",
        "No se presentó o llegó tarde", "Daños o materiales faltantes", "Otro motivo"
    )

    suspend fun obtenerDesacuerdos(solicitudId: String): List<DesacuerdoItem> = withContext(Dispatchers.IO) {
        try {
            val (c, r) = peticionHttp("$REST_URL/desacuerdos?solicitud_id=eq.$solicitudId&order=creado_en.asc")
            if (c in 200..299 && r.isNotBlank()) {
                val arr = JSONArray(r)
                return@withContext List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    DesacuerdoItem(
                        id = o.optString("id"), solicitudId = o.optString("solicitud_id"),
                        abiertoPorRol = o.optString("abierto_por_rol"), motivo = o.optString("motivo"),
                        descripcion = o.optString("descripcion", ""), respuesta = o.optString("respuesta", ""),
                        estado = o.optString("estado", "abierto"), creadoEn = o.optString("creado_en", "")
                    )
                }
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun abrirDesacuerdo(solicitud: SolicitudItem, rol: String, motivo: String, descripcion: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (descripcion.trim().length < 20) {
                    return@withContext Result.failure(Exception("Cuenta qué pasó con al menos 20 caracteres."))
                }
                val p = JSONObject().apply {
                    put("solicitud_id", solicitud.id); put("abierto_por_rol", rol)
                    put("motivo", motivo); put("descripcion", descripcion.trim()); put("estado", "abierto")
                }
                val (c, r) = peticionHttp("$REST_URL/desacuerdos", "POST", p.toString())
                if (c !in 200..299) return@withContext Result.failure(Exception("No se pudo registrar el desacuerdo (Código $c): $r"))
                registrarEvento(solicitud.id, rol, TipoEvento.DESACUERDO_ABIERTO, motivo,
                    if (rol == "cliente") solicitud.trabajadorId else solicitud.clienteId)
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    /** La otra parte responde; después cualquiera puede marcarlo resuelto o escalarlo a revisión del equipo Habilio. */
    suspend fun actualizarDesacuerdo(
        solicitud: SolicitudItem, desacuerdo: DesacuerdoItem, rol: String, nuevoEstado: String, respuesta: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val p = JSONObject().apply {
                put("estado", nuevoEstado)
                if (respuesta.isNotBlank()) put("respuesta", respuesta.trim())
            }
            val ok = peticionHttp("$REST_URL/desacuerdos?id=eq.${desacuerdo.id}", "PATCH", p.toString()).first in 200..299
            if (ok) {
                val tipo = when (nuevoEstado) {
                    "respondido" -> TipoEvento.DESACUERDO_RESPONDIDO
                    "resuelto" -> TipoEvento.DESACUERDO_RESUELTO
                    else -> TipoEvento.DESACUERDO_ESCALADO
                }
                registrarEvento(solicitud.id, rol, tipo, respuesta,
                    if (rol == "cliente") solicitud.trabajadorId else solicitud.clienteId)
            }
            ok
        } catch (_: Exception) { false }
    }

    // ------------------------------------------------------------------------
    // ACTA #14: REPORTE Y MODERACIÓN DE RESEÑAS
    // ------------------------------------------------------------------------
    /** Criterios públicos con los que se revisa una reseña reportada. */
    val CRITERIOS_MODERACION = listOf(
        "Reseña falsa: la persona nunca recibió el servicio",
        "Lenguaje ofensivo, discriminatorio o amenazas",
        "Datos personales publicados (teléfono, dirección)",
        "Publicidad o contenido ajeno al servicio",
        "Contenido sexual o que acose a alguien"
    )

    /** Guarda el reporte. Con 3 reportes distintos la reseña se oculta automáticamente hasta que el equipo la revise. */
    suspend fun reportarResena(resenaId: String, reportanteId: String, criterio: String, detalle: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val p = JSONObject().apply {
                    put("resena_id", resenaId); put("reportante_id", reportanteId)
                    put("criterio", criterio); put("detalle", detalle.trim()); put("estado", "pendiente")
                }
                val (c, _) = peticionHttp("$REST_URL/reportes_resena", "POST", p.toString())
                if (c !in 200..299) return@withContext false
                val (c2, r2) = peticionHttp("$REST_URL/reportes_resena?resena_id=eq.$resenaId&select=reportante_id")
                if (c2 in 200..299 && r2.isNotBlank()) {
                    val a = JSONArray(r2)
                    val distintos = (0 until a.length()).map { a.getJSONObject(it).optString("reportante_id") }.toSet()
                    if (distintos.size >= 3) {
                        peticionHttp("$REST_URL/resenas?id=eq.$resenaId", "PATCH",
                            JSONObject().apply { put("oculta", true) }.toString())
                    }
                }
                true
            } catch (_: Exception) { false }
        }

    // ------------------------------------------------------------------------
    // ACTA #3: CONTROL DE CÓDIGO DE AUTORIZACIÓN (#4 también)
    // ------------------------------------------------------------------------
    /**
     * Acta #4: la autorización de una persona menor de 18 años deja de ser un simple "sí". Se pide el nombre
     * y teléfono de la persona responsable y se guarda como PENDIENTE de verificación. Mientras no cambie a
     * "verificada", el perfil no muestra datos de contacto y no puede recibir solicitudes.
     */
    suspend fun registrarAutorizacionTutor(
        perfilId: String, nombreTutor: String, telefonoTutor: String, parentesco: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val p = JSONObject().apply {
                put("perfil_id", perfilId); put("nombre_tutor", nombreTutor.trim())
                put("telefono_tutor", telefonoTutor.filter { it.isDigit() }); put("parentesco", parentesco)
                put("estado", "pendiente"); put("aceptado_en", marcaDeTiempoActual())
            }
            val ok = peticionHttp("$REST_URL/autorizaciones_tutor", "POST", p.toString()).first in 200..299
            if (ok) peticionHttp("$REST_URL/perfiles?id=eq.$perfilId", "PATCH",
                JSONObject().apply { put("autorizacion_estado", "pendiente") }.toString())
            ok
        } catch (_: Exception) { false }
    }

    suspend fun actualizarDatosProfesionales(
        perfilId: String, aniosExperiencia: Int, disponibilidad: String, precioDesde: Double, visibleTalentoMujeres: Boolean,
        descripcion: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val p = JSONObject().apply {
                if (descripcion != null) put("descripcion", descripcion.trim())
                put("anios_experiencia", aniosExperiencia); put("disponibilidad", disponibilidad)
                put("precio_desde", precioDesde); put("visible_talento_mujeres", visibleTalentoMujeres)
            }
            var r = peticionHttp("$REST_URL/perfiles?id=eq.$perfilId", "PATCH", p.toString())
            if (r.first == 400 && descripcion != null) {
                // Aún sin las columnas nuevas (script SQL v0.3): al menos se conserva la presentación.
                r = peticionHttp("$REST_URL/perfiles?id=eq.$perfilId", "PATCH",
                    JSONObject().apply { put("descripcion", descripcion.trim()) }.toString())
            }
            r.first in 200..299
        } catch (_: Exception) { false }
    }
}
