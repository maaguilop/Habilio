package com.habilio.app

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.UUID
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#0E1013")),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#0E1013"))
        )
        setContent {
            HabilioApp()
        }
    }
}

// ============================================================================
// 1. SISTEMA DE DISEÑO Y TOKENS DE COLOR
// ============================================================================
object HabilioTheme {
    val FondoPrincipal = Color(0xFF0E1013)
    val SuperficieTarjeta = Color(0xFF181B20)
    val SuperficieInput = Color(0xFF121418)
    val SuperficieElevada = Color(0xFF22262E)
    val BordeTarjeta = Color(0xFF2A2F3A)

    val AmarilloPrimario = Color(0xFFF5B811)
    val FondoAmarilloSuave = Color(0xFF332910)
    val TextoSobreAmarillo = Color(0xFF111315)

    val VerdeExito = Color(0xFF12C97D)
    val FondoVerdeSuave = Color(0xFF132A23)

    val AzulVerificado = Color(0xFF3898FF)
    val FondoAzulSuave = Color(0xFF132235)

    val RojoUrgente = Color(0xFFFF6B6B)
    val FondoRojoSuave = Color(0xFF2D181B)

    val TextoPrimario = Color(0xFFF4F6F8)
    val TextoSecundario = Color(0xFFB0B8C4)
    val TextoPlaceholder = Color(0xFF8A94A6)
}

object AppConfig {
    var logoGlobal: Any = R.drawable.logo_habilio
}

// ============================================================================
// 2. MODELOS DE DATOS Y CLIENTE REST DE SUPABASE
// ============================================================================
data class PerfilUsuario(
    val id: String,
    val rol: String,
    val nombre: String,
    val telefono: String,
    val correo: String,
    val zona: String,
    val direccion: String,
    val nombreTaller: String,
    val oficio: String,
    val especialidad: String,
    val modalidad: String,
    val descripcion: String,
    val esPremium: Boolean,
    val calificacion: Double,
    val fotoUrl: String
)

data class EvidenciaItem(
    val id: String,
    val trabajadorId: String,
    val titulo: String,
    val ubicacion: String,
    val fechaPublicacion: String,
    val fotoUrl: String? = null
)

data class ResenaItem(
    val id: String,
    val trabajadorId: String,
    val autorNombre: String,
    val comentario: String,
    val estrellas: Int,
    val fechaTexto: String
)

data class SolicitudItem(
    val id: String,
    val clienteId: String,
    val trabajadorId: String,
    val oficioEtiqueta: String,
    val descripcion: String,
    val ubicacion: String,
    val monto: Double,
    val esUrgente: Boolean,
    val estado: String,
    val fueEvaluada: Boolean = false,
    val nombreCliente: String = "Cliente Habilio",
    val nombreTrabajador: String = "Especialista Habilio",
    val oficioTrabajador: String = "Oficio Verificado"
)

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
     * Comprime y redimensiona la imagen seleccionada a un máximo de 1280px y calidad JPEG 75% (S-05).
     */
    private fun comprimirImagenJpeg(context: android.content.Context, uri: Uri): ByteArray? {
        return try {
            val bytesOriginales = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            val bitmapOriginal = BitmapFactory.decodeByteArray(bytesOriginales, 0, bytesOriginales.size)
                ?: return bytesOriginales

            val maxLado = 1280
            val ancho = bitmapOriginal.width
            val alto = bitmapOriginal.height
            val bitmapEscalado = if (max(ancho, alto) > maxLado) {
                val escala = maxLado.toFloat() / max(ancho, alto).toFloat()
                val nuevoAncho = (ancho * escala).toInt().coerceAtLeast(1)
                val nuevoAlto = (alto * escala).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmapOriginal, nuevoAncho, nuevoAlto, true)
            } else {
                bitmapOriginal
            }

            val salida = ByteArrayOutputStream()
            bitmapEscalado.compress(Bitmap.CompressFormat.JPEG, 75, salida)
            if (bitmapEscalado != bitmapOriginal) {
                bitmapEscalado.recycle()
            }
            bitmapOriginal.recycle()
            salida.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun subirFotoStorage(context: android.content.Context, uri: Uri): String? = withContext(Dispatchers.IO) {
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
        fotoUrl = limpiarUrlJson(obj, "foto_url") ?: ""
    )

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
        fotoUrl: String? = null
    ): Result<PerfilUsuario> = withContext(Dispatchers.IO) {
        try {
            val digitosTel = telefono.filter { it.isDigit() }
            if (digitosTel.length < 10) {
                return@withContext Result.failure(Exception("Ingresa un número de teléfono válido de 10 dígitos."))
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

            // No afirmar "verificado" ni otorgar 5.0 o Premium sin evidencia previa
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

            val (code, resp) = peticionHttp("$REST_URL/perfiles", "POST", perfilPayload.toString())
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
        fotoUrl: String? = null
    ): Result<PerfilUsuario> = withContext(Dispatchers.IO) {
        try {
            val digitosTel = telefono.filter { it.isDigit() }
            if (digitosTel.length < 10) {
                return@withContext Result.failure(Exception("El teléfono debe tener al menos 10 dígitos."))
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
                        fotoUrl = limpiarUrlJson(o, "foto_url")
                    )
                }
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun publicarEvidencia(
        trabajadorId: String,
        titulo: String,
        ubicacion: String,
        fotoUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("trabajador_id", trabajadorId)
                put("titulo", titulo.trim())
                put("ubicacion", ubicacion.ifBlank { "Pénjamo, Gto." })
                put("fecha_publicacion", "Evidencia real • $ubicacion")
                if (!fotoUrl.isNullOrBlank()) put("foto_url", fotoUrl)
            }
            val exito = peticionHttp("$REST_URL/evidencias", "POST", payload.toString()).first in 200..299
            if (exito) {
                // Al subir evidencia fotográfica real, el perfil acredita experiencia comprobable
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
                        fechaTexto = o.optString("fecha_texto", "Reciente • Servicio concluido")
                    )
                }
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun publicarResena(
        trabajadorId: String,
        autorNombre: String,
        comentario: String,
        estrellas: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("trabajador_id", trabajadorId)
                put("autor_nombre", autorNombre.trim())
                put("comentario", comentario.trim())
                put("estrellas", estrellas)
                put("fecha_texto", "Hace un momento • Servicio concluido")
            }
            val (code, _) = peticionHttp("$REST_URL/resenas", "POST", payload.toString())
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
        monto: Double
    ): Result<SolicitudItem> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("cliente_id", clienteId)
                put("trabajador_id", trabajadorId)
                put("oficio_etiqueta", oficioEtiqueta)
                put("descripcion", descripcion.trim())
                put("ubicacion", ubicacion.trim())
                put("monto", monto)
                put("es_urgente", true)
                put("estado", "pendiente")
            }
            val (code, resp) = peticionHttp("$REST_URL/solicitudes", "POST", payload.toString())
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
                            estado = o.optString("estado", "pendiente")
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
                            oficioTrabajador = mapaNombres[tId]?.second ?: o.optString("oficio_etiqueta")
                        )
                    }
                }
            } catch (_: Exception) {}
            emptyList()
        }

    /**
     * Actualiza el estado de una solicitud. Si la tabla SQL tiene activo el CHECK constraint original
     * (solo pendiente, en_ejecucion, completado, rechazado), aplica respaldo automático sin perder el estado.
     */
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
}

// ============================================================================
// 3. RUTAS Y CONTROLADOR CENTRAL DE NAVEGACIÓN Y ESTADO
// ============================================================================
enum class RutaHabilio {
    INICIO_ROL,
    LOGIN_CLIENTE,
    REGISTRO_CLIENTE,
    LOGIN_TRABAJADOR,
    REGISTRO_TRABAJADOR,
    BUSCADOR_CLIENTE,
    DETALLE_ESPECIALISTA,
    PERFIL_CLIENTE,
    SOLICITUDES_TRABAJADOR,
    TALLER_TRABAJADOR,
    EDITAR_PERFIL,
    SEGUIMIENTO_SOLICITUD
}

val OFICIOS_HABILIO = listOf(
    Triple("Albañilería", "Obra y acabados", Icons.Default.Construction),
    Triple("Plomería", "Fugas y tubos", Icons.Default.Plumbing),
    Triple("Electricidad", "110v / 220v", Icons.Default.Bolt),
    Triple("Carpintería", "Madera y clósets", Icons.Default.Carpenter),
    Triple("Herrería", "Puertas y rejas", Icons.Default.Hardware),
    Triple("Pintura", "Interiores y fachadas", Icons.Default.FormatPaint),
    Triple("Costura", "Prendas y arreglos", Icons.Default.ContentCut)
)

val MODALIDADES_HABILIO = listOf(
    "🚗 A Domicilio" to "Me traslado a la casa o negocio del cliente en la región.",
    "🏠 En mi Taller o Local" to "Tengo ubicación física.",
    "🔄 Ambos (Taller y a domicilio)" to "Flexibilidad total según lo que el cliente requiera."
)

@Composable
fun HabilioApp() {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var pantallaActual by remember { mutableStateOf(RutaHabilio.INICIO_ROL) }

    var usuarioActual by remember { mutableStateOf<PerfilUsuario?>(null) }
    var listaTrabajadores by remember { mutableStateOf<List<PerfilUsuario>>(emptyList()) }
    var trabajadorSeleccionado by remember { mutableStateOf<PerfilUsuario?>(null) }
    var solicitudEnSeguimiento by remember { mutableStateOf<SolicitudItem?>(null) }
    var todasLasEvidencias by remember { mutableStateOf<List<EvidenciaItem>>(emptyList()) }
    var todasLasResenas by remember { mutableStateOf<List<ResenaItem>>(emptyList()) }
    var solicitudesUsuario by remember { mutableStateOf<List<SolicitudItem>>(emptyList()) }
    var cargandoDatos by remember { mutableStateOf(false) }

    var mostrarModalCompra by remember { mutableStateOf(false) }
    var solicitudRecienConfirmada by remember { mutableStateOf<SolicitudItem?>(null) }
    var mostrarModalCalificar by remember { mutableStateOf(false) }
    var evidenciaEnPantallaCompleta by remember { mutableStateOf<EvidenciaItem?>(null) }

    var solicitudAEvaluar by remember { mutableStateOf<SolicitudItem?>(null) }

    fun recargarDatosGlobales() {
        scope.launch {
            cargandoDatos = true
            listaTrabajadores = SupabaseService.obtenerTrabajadores()
            todasLasEvidencias = SupabaseService.obtenerEvidencias()
            todasLasResenas = SupabaseService.obtenerResenas()
            usuarioActual?.let { user ->
                solicitudesUsuario = SupabaseService.obtenerSolicitudesUsuario(
                    usuarioId = user.id,
                    esTrabajador = user.rol == "trabajador"
                )
                trabajadorSeleccionado?.let { sel ->
                    trabajadorSeleccionado = listaTrabajadores.find { it.id == sel.id } ?: sel
                }
                solicitudEnSeguimiento?.let { seg ->
                    solicitudEnSeguimiento = solicitudesUsuario.find { it.id == seg.id } ?: seg
                }
            }
            cargandoDatos = false
        }
    }

    fun cerrarSesionYVolverInicio() {
        SupabaseService.cerrarSesionToken()
        usuarioActual = null
        pantallaActual = RutaHabilio.INICIO_ROL
    }

    BackHandler(enabled = pantallaActual != RutaHabilio.INICIO_ROL) {
        when (pantallaActual) {
            RutaHabilio.LOGIN_CLIENTE, RutaHabilio.LOGIN_TRABAJADOR -> pantallaActual = RutaHabilio.INICIO_ROL
            RutaHabilio.REGISTRO_CLIENTE -> pantallaActual = RutaHabilio.LOGIN_CLIENTE
            RutaHabilio.REGISTRO_TRABAJADOR -> pantallaActual = RutaHabilio.LOGIN_TRABAJADOR
            RutaHabilio.DETALLE_ESPECIALISTA, RutaHabilio.PERFIL_CLIENTE -> pantallaActual = RutaHabilio.BUSCADOR_CLIENTE
            RutaHabilio.SEGUIMIENTO_SOLICITUD -> pantallaActual = RutaHabilio.PERFIL_CLIENTE
            RutaHabilio.TALLER_TRABAJADOR -> pantallaActual = RutaHabilio.SOLICITUDES_TRABAJADOR
            RutaHabilio.EDITAR_PERFIL -> {
                pantallaActual = if (usuarioActual?.rol == "trabajador") {
                    RutaHabilio.TALLER_TRABAJADOR
                } else {
                    RutaHabilio.PERFIL_CLIENTE
                }
            }
            RutaHabilio.BUSCADOR_CLIENTE, RutaHabilio.SOLICITUDES_TRABAJADOR -> cerrarSesionYVolverInicio()
            RutaHabilio.INICIO_ROL -> {}
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = HabilioTheme.FondoPrincipal,
            surface = HabilioTheme.SuperficieTarjeta,
            primary = HabilioTheme.AmarilloPrimario,
            onPrimary = HabilioTheme.TextoSobreAmarillo,
            onBackground = HabilioTheme.TextoPrimario,
            onSurface = HabilioTheme.TextoPrimario
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = HabilioTheme.FondoPrincipal) {
            when (pantallaActual) {
                RutaHabilio.INICIO_ROL -> PantallaInicioRol(
                    alElegirCliente = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                    alElegirTrabajador = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR }
                )

                RutaHabilio.LOGIN_CLIENTE -> PantallaLoginUnificada(
                    esModoTrabajador = false,
                    alIrARegistro = { pantallaActual = RutaHabilio.REGISTRO_CLIENTE },
                    alLoginExitoso = { perfil ->
                        usuarioActual = perfil
                        recargarDatosGlobales()
                        pantallaActual = if (perfil.rol == "trabajador") {
                            RutaHabilio.SOLICITUDES_TRABAJADOR
                        } else {
                            RutaHabilio.BUSCADOR_CLIENTE
                        }
                    },
                    alCambiarRolOpuesto = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR },
                    alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.REGISTRO_CLIENTE -> PantallaRegistroCliente(
                    alIrALogin = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                    alRegistroExitoso = { perfil ->
                        usuarioActual = perfil
                        recargarDatosGlobales()
                        pantallaActual = RutaHabilio.BUSCADOR_CLIENTE
                    },
                    alCambiarATrabajador = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR },
                    alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.LOGIN_TRABAJADOR -> PantallaLoginUnificada(
                    esModoTrabajador = true,
                    alIrARegistro = { pantallaActual = RutaHabilio.REGISTRO_TRABAJADOR },
                    alLoginExitoso = { perfil ->
                        usuarioActual = perfil
                        recargarDatosGlobales()
                        pantallaActual = if (perfil.rol == "cliente") {
                            RutaHabilio.BUSCADOR_CLIENTE
                        } else {
                            RutaHabilio.SOLICITUDES_TRABAJADOR
                        }
                    },
                    alCambiarRolOpuesto = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                    alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.REGISTRO_TRABAJADOR -> PantallaRegistroTrabajador(
                    alIrALogin = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR },
                    alRegistroExitoso = { perfil ->
                        usuarioActual = perfil
                        recargarDatosGlobales()
                        pantallaActual = RutaHabilio.SOLICITUDES_TRABAJADOR
                    },
                    alCambiarACliente = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                    alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.BUSCADOR_CLIENTE -> PantallaBuscadorCliente(
                    trabajadores = listaTrabajadores,
                    evidencias = todasLasEvidencias,
                    resenas = todasLasResenas,
                    cargando = cargandoDatos,
                    alRefrescar = { recargarDatosGlobales() },
                    alVerEspecialista = { especialista ->
                        trabajadorSeleccionado = especialista
                        pantallaActual = RutaHabilio.DETALLE_ESPECIALISTA
                    },
                    alClickFotoEvidencia = { ev ->
                        if (!ev.fotoUrl.isNullOrBlank()) evidenciaEnPantallaCompleta = ev
                    },
                    alIrAMiPerfil = {
                        recargarDatosGlobales()
                        pantallaActual = RutaHabilio.PERFIL_CLIENTE
                    },
                    alCambiarRol = { cerrarSesionYVolverInicio() }
                )

                RutaHabilio.DETALLE_ESPECIALISTA -> {
                    val esp = trabajadorSeleccionado
                    if (esp != null) {
                        val serviciosConEsteTrabajador = solicitudesUsuario.count {
                            it.trabajadorId == esp.id && it.estado == "completado"
                        }
                        val serviciosSinEvaluar = solicitudesUsuario.count {
                            it.trabajadorId == esp.id && it.estado == "completado" && !it.fueEvaluada
                        }

                        PantallaDetalleEspecialista(
                            especialista = esp,
                            evidencias = todasLasEvidencias.filter { it.trabajadorId == esp.id },
                            resenas = todasLasResenas.filter { it.trabajadorId == esp.id },
                            totalServicios = serviciosConEsteTrabajador,
                            puedeCalificar = serviciosSinEvaluar > 0,
                            alVolverAlListado = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                            alAbrirModalSolicitar = { mostrarModalCompra = true },
                            alAbrirModalCalificar = {
                                solicitudAEvaluar = solicitudesUsuario.firstOrNull {
                                    it.trabajadorId == esp.id && it.estado == "completado" && !it.fueEvaluada
                                }
                                mostrarModalCalificar = true
                            },
                            alClickFotoEvidencia = { ev ->
                                if (!ev.fotoUrl.isNullOrBlank()) evidenciaEnPantallaCompleta = ev
                            },
                            alIrAExplorar = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                            alIrAMiPerfil = {
                                recargarDatosGlobales()
                                pantallaActual = RutaHabilio.PERFIL_CLIENTE
                            }
                        )
                    } else {
                        pantallaActual = RutaHabilio.BUSCADOR_CLIENTE
                    }
                }

                RutaHabilio.PERFIL_CLIENTE -> PantallaPerfilCliente(
                    cliente = usuarioActual,
                    solicitudes = solicitudesUsuario,
                    alEditarPerfil = { pantallaActual = RutaHabilio.EDITAR_PERFIL },
                    alVerSeguimientoSolicitud = { solicitud ->
                        solicitudEnSeguimiento = solicitud
                        pantallaActual = RutaHabilio.SEGUIMIENTO_SOLICITUD
                    },
                    alLiberarPago = { solicitud ->
                        solicitudesUsuario = solicitudesUsuario.map {
                            if (it.id == solicitud.id) it.copy(estado = "completado") else it
                        }
                        scope.launch {
                            SupabaseService.actualizarEstadoSolicitud(
                                solicitudId = solicitud.id,
                                nuevoEstado = "completado",
                                descripcionActual = solicitud.descripcion
                            )
                            recargarDatosGlobales()
                        }
                    },
                    alCalificarTrabajador = { solicitud ->
                        listaTrabajadores.find { it.id == solicitud.trabajadorId }?.let {
                            trabajadorSeleccionado = it
                            solicitudAEvaluar = solicitud
                            mostrarModalCalificar = true
                        }
                    },
                    alIrAExplorar = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                    alCambiarRol = { cerrarSesionYVolverInicio() }
                )

                RutaHabilio.SEGUIMIENTO_SOLICITUD -> {
                    val solActual = solicitudesUsuario.find { it.id == solicitudEnSeguimiento?.id }
                        ?: solicitudEnSeguimiento
                    if (solActual != null) {
                        val espAsociado = listaTrabajadores.find { it.id == solActual.trabajadorId }
                        PantallaSeguimientoSolicitud(
                            solicitud = solActual,
                            especialista = espAsociado,
                            alVolver = { pantallaActual = RutaHabilio.PERFIL_CLIENTE },
                            alRefrescar = { recargarDatosGlobales() },
                            alCancelarPedido = {
                                val actualizada = solActual.copy(estado = "cancelada")
                                solicitudEnSeguimiento = actualizada
                                solicitudesUsuario = solicitudesUsuario.map {
                                    if (it.id == solActual.id) actualizada else it
                                }
                                scope.launch {
                                    SupabaseService.actualizarEstadoSolicitud(
                                        solicitudId = solActual.id,
                                        nuevoEstado = "cancelada",
                                        descripcionActual = solActual.descripcion
                                    )
                                    recargarDatosGlobales()
                                }
                            },
                            alConfirmarCierre = {
                                // El cliente confirma que el trabajo fue completado
                                val actualizada = solActual.copy(estado = "completado")
                                solicitudEnSeguimiento = actualizada
                                solicitudesUsuario = solicitudesUsuario.map {
                                    if (it.id == solActual.id) actualizada else it
                                }
                                scope.launch {
                                    SupabaseService.actualizarEstadoSolicitud(
                                        solicitudId = solActual.id,
                                        nuevoEstado = "completado",
                                        descripcionActual = solActual.descripcion
                                    )
                                    recargarDatosGlobales()
                                }
                            },
                            alCalificar = {
                                if (espAsociado != null) {
                                    trabajadorSeleccionado = espAsociado
                                    solicitudAEvaluar = solActual
                                    mostrarModalCalificar = true
                                }
                            },
                            alVerPerfilTrabajador = {
                                if (espAsociado != null) {
                                    trabajadorSeleccionado = espAsociado
                                    pantallaActual = RutaHabilio.DETALLE_ESPECIALISTA
                                }
                            }
                        )
                    } else {
                        pantallaActual = RutaHabilio.PERFIL_CLIENTE
                    }
                }

                RutaHabilio.SOLICITUDES_TRABAJADOR -> PantallaSolicitudesTrabajador(
                    trabajador = usuarioActual,
                    solicitudes = solicitudesUsuario,
                    cargando = cargandoDatos,
                    alRefrescar = { recargarDatosGlobales() },
                    alCambiarEstadoSolicitud = { solicitud, nuevoEstado ->
                        solicitudesUsuario = solicitudesUsuario.map {
                            if (it.id == solicitud.id) it.copy(estado = nuevoEstado) else it
                        }
                        scope.launch {
                            SupabaseService.actualizarEstadoSolicitud(
                                solicitudId = solicitud.id,
                                nuevoEstado = nuevoEstado,
                                descripcionActual = solicitud.descripcion
                            )
                            recargarDatosGlobales()
                        }
                    },
                    alIrAMiTaller = {
                        recargarDatosGlobales()
                        pantallaActual = RutaHabilio.TALLER_TRABAJADOR
                    },
                    alCambiarRol = { cerrarSesionYVolverInicio() }
                )

                RutaHabilio.TALLER_TRABAJADOR -> PantallaTallerTrabajador(
                    trabajador = usuarioActual,
                    evidencias = todasLasEvidencias.filter { it.trabajadorId == usuarioActual?.id },
                    solicitudes = solicitudesUsuario,
                    alEditarPerfil = { pantallaActual = RutaHabilio.EDITAR_PERFIL },
                    alPublicarEvidencia = { titulo, imagenUri ->
                        val t = usuarioActual ?: return@PantallaTallerTrabajador
                        scope.launch {
                            var urlFotoPublica: String? = null
                            if (imagenUri != null) {
                                urlFotoPublica = SupabaseService.subirFotoStorage(contexto, imagenUri)
                            }
                            SupabaseService.publicarEvidencia(
                                trabajadorId = t.id,
                                titulo = titulo,
                                ubicacion = t.zona,
                                fotoUrl = urlFotoPublica
                            )
                            recargarDatosGlobales()
                        }
                    },
                    alClickFotoEvidencia = { ev ->
                        if (!ev.fotoUrl.isNullOrBlank()) evidenciaEnPantallaCompleta = ev
                    },
                    alIrASolicitudes = {
                        recargarDatosGlobales()
                        pantallaActual = RutaHabilio.SOLICITUDES_TRABAJADOR
                    },
                    alCambiarRol = { cerrarSesionYVolverInicio() }
                )

                RutaHabilio.EDITAR_PERFIL -> {
                    val u = usuarioActual
                    if (u != null) {
                        PantallaEditarPerfil(
                            usuarioActual = u,
                            alGuardarExitoso = { perfilActualizado ->
                                usuarioActual = perfilActualizado
                                recargarDatosGlobales()
                                pantallaActual = if (perfilActualizado.rol == "trabajador") {
                                    RutaHabilio.TALLER_TRABAJADOR
                                } else {
                                    RutaHabilio.PERFIL_CLIENTE
                                }
                            },
                            alCancelar = {
                                pantallaActual = if (u.rol == "trabajador") {
                                    RutaHabilio.TALLER_TRABAJADOR
                                } else {
                                    RutaHabilio.PERFIL_CLIENTE
                                }
                            }
                        )
                    } else {
                        pantallaActual = RutaHabilio.INICIO_ROL
                    }
                }
            }

            if (mostrarModalCompra && trabajadorSeleccionado != null) {
                ModalSolicitarServicio(
                    trabajador = trabajadorSeleccionado!!,
                    direccionInicial = usuarioActual?.direccion ?: "Calle Hidalgo #114, Zona Centro, Pénjamo",
                    alCerrar = { mostrarModalCompra = false },
                    alConfirmarSolicitud = { descripcion, ubicacion, presupuesto ->
                        val clienteId = usuarioActual?.id ?: return@ModalSolicitarServicio
                        val trab = trabajadorSeleccionado ?: return@ModalSolicitarServicio
                        scope.launch {
                            SupabaseService.crearSolicitud(
                                clienteId = clienteId,
                                trabajadorId = trab.id,
                                oficioEtiqueta = trab.oficio,
                                descripcion = descripcion,
                                ubicacion = ubicacion,
                                monto = presupuesto
                            ).onSuccess { creada ->
                                mostrarModalCompra = false
                                solicitudRecienConfirmada = creada.copy(
                                    nombreTrabajador = trab.nombre,
                                    oficioTrabajador = trab.oficio
                                )
                                recargarDatosGlobales()
                            }
                        }
                    }
                )
            }

            if (solicitudRecienConfirmada != null) {
                ModalSolicitudConfirmada(
                    solicitud = solicitudRecienConfirmada!!,
                    alAceptarYVerEstatus = {
                        solicitudEnSeguimiento = solicitudRecienConfirmada
                        solicitudRecienConfirmada = null
                        recargarDatosGlobales()
                        pantallaActual = RutaHabilio.SEGUIMIENTO_SOLICITUD
                    },
                    alVolverAlInicio = {
                        solicitudRecienConfirmada = null
                        pantallaActual = RutaHabilio.BUSCADOR_CLIENTE
                    }
                )
            }

            if (mostrarModalCalificar && trabajadorSeleccionado != null) {
                ModalEvaluarServicio(
                    nombreClienteInicial = usuarioActual?.let { "${it.nombre} (${it.zona})" }
                        ?: "Cliente Verificado",
                    alCerrar = { mostrarModalCalificar = false },
                    alPublicarResena = { estrellas, autor, comentario ->
                        val trab = trabajadorSeleccionado ?: return@ModalEvaluarServicio
                        scope.launch {
                            SupabaseService.publicarResena(trab.id, autor, comentario, estrellas)

                            solicitudAEvaluar?.let { sol ->
                                SupabaseService.marcarSolicitudEvaluada(sol.id)

                                solicitudesUsuario = solicitudesUsuario.map {
                                    if (it.id == sol.id) it.copy(fueEvaluada = true) else it
                                }
                                if (solicitudEnSeguimiento?.id == sol.id) {
                                    solicitudEnSeguimiento = solicitudEnSeguimiento?.copy(fueEvaluada = true)
                                }
                            }

                            mostrarModalCalificar = false
                            solicitudAEvaluar = null

                            recargarDatosGlobales()
                        }
                    }
                )
            }

            if (evidenciaEnPantallaCompleta != null) {
                ModalVerFotoCompleta(
                    evidencia = evidenciaEnPantallaCompleta!!,
                    alCerrar = { evidenciaEnPantallaCompleta = null }
                )
            }
        }
    }
}

// ============================================================================
// 4. COMPONENTES REUTILIZABLES OPTIMIZADOS
// ============================================================================

@Composable
fun LogoGlobalHabilio(
    modifier: Modifier = Modifier,
    colorIcono: Color = HabilioTheme.AmarilloPrimario
) {
    when (val logo = AppConfig.logoGlobal) {
        is ImageVector -> {
            Icon(
                imageVector = logo,
                contentDescription = "Logo Habilio",
                tint = colorIcono,
                modifier = modifier
            )
        }
        else -> {
            AsyncImage(
                model = logo,
                contentDescription = "Logo Habilio",
                contentScale = ContentScale.FillBounds,
                modifier = modifier
            )
        }
    }
}

@Composable
fun coloresInputHabilio() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = HabilioTheme.SuperficieInput,
    unfocusedContainerColor = HabilioTheme.SuperficieInput,
    focusedBorderColor = HabilioTheme.AmarilloPrimario,
    unfocusedBorderColor = HabilioTheme.BordeTarjeta,
    focusedTextColor = HabilioTheme.TextoPrimario,
    unfocusedTextColor = HabilioTheme.TextoPrimario
)

@Composable
fun HabilioTarjeta(
    modifier: Modifier = Modifier,
    colorFondo: Color = HabilioTheme.SuperficieTarjeta,
    colorBorde: Color = HabilioTheme.BordeTarjeta,
    grosorBorde: Dp = 1.dp,
    radioEsquina: Dp = 16.dp,
    paddingInterno: Dp = 16.dp,
    alClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radioEsquina)
    val border = BorderStroke(grosorBorde, colorBorde)
    if (alClick != null) {
        Surface(
            onClick = alClick,
            shape = shape,
            color = colorFondo,
            border = border,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(paddingInterno), content = contenido)
        }
    } else {
        Surface(
            shape = shape,
            color = colorFondo,
            border = border,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(paddingInterno), content = contenido)
        }
    }
}

@Composable
fun ModalVerFotoCompleta(
    evidencia: EvidenciaItem,
    alCerrar: () -> Unit
) {
    Dialog(
        onDismissRequest = alCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = alCerrar)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = evidencia.titulo,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = HabilioTheme.TextoPrimario
                            )
                            Text(
                                text = "✓ ${evidencia.fechaPublicacion}",
                                fontSize = 12.sp,
                                color = HabilioTheme.VerdeExito
                            )
                        }
                        IconButton(
                            onClick = alCerrar,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(HabilioTheme.SuperficieElevada)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar vista completa",
                                tint = HabilioTheme.TextoPrimario
                            )
                        }
                    }

                    AsyncImage(
                        model = evidencia.fotoUrl,
                        contentDescription = evidencia.titulo,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp, max = 480.dp)
                            .background(Color.Black)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 ${evidencia.ubicacion}",
                            fontSize = 13.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                        TextButton(onClick = alCerrar) {
                            Text(
                                text = "Cerrar",
                                color = HabilioTheme.AmarilloPrimario,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabilioTopBar(
    subtituloUbicacion: String? = "Pénjamo, Gto.",
    modoTrabajador: Boolean = false,
    alAccionDerecha: (() -> Unit)? = null,
    iconoDerecha: ImageVector = Icons.Default.SwapHoriz
) {
    Surface(
        color = HabilioTheme.FondoPrincipal,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.SuperficieTarjeta)
                            .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        LogoGlobalHabilio(modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Habilio",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HabilioTheme.TextoPrimario,
                                maxLines = 1
                            )
                            if (modoTrabajador) {
                                Spacer(modifier = Modifier.width(8.dp))
                                HabilioBadge(
                                    texto = "● Modo trabajador",
                                    colorTexto = HabilioTheme.VerdeExito,
                                    colorFondo = HabilioTheme.FondoVerdeSuave
                                )
                            }
                        }
                        if (subtituloUbicacion != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtituloUbicacion,
                                fontSize = 13.sp,
                                color = HabilioTheme.TextoSecundario,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (alAccionDerecha != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = alAccionDerecha,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.SuperficieTarjeta)
                            .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = iconoDerecha,
                            contentDescription = "Cerrar sesión o cambiar rol",
                            tint = HabilioTheme.TextoSecundario
                        )
                    }
                }
            }
            HorizontalDivider(color = HabilioTheme.BordeTarjeta, thickness = 1.dp)
        }
    }
}

@Composable
fun HabilioBadge(
    texto: String,
    colorTexto: Color,
    colorFondo: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = colorFondo,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AvatarUsuario(
    tamano: Dp = 54.dp,
    esCircular: Boolean = false,
    mostrarPuntoDisponible: Boolean = false,
    icono: ImageVector = Icons.Default.Person,
    colorIcono: Color = HabilioTheme.TextoSecundario,
    colorFondo: Color = HabilioTheme.SuperficieElevada,
    fotoUrl: String? = null
) {
    val shape = if (esCircular) CircleShape else RoundedCornerShape(12.dp)
    Box(contentAlignment = Alignment.BottomEnd) {
        if (!fotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(tamano)
                    .clip(shape)
                    .background(colorFondo)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(tamano)
                    .clip(shape)
                    .background(colorFondo),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(tamano * 0.6f)
                )
            }
        }
        if (mostrarPuntoDisponible) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(HabilioTheme.VerdeExito)
                    .border(2.dp, HabilioTheme.SuperficieTarjeta, CircleShape)
            )
        }
    }
}

@Composable
fun EstadoCargaOVacio(
    cargando: Boolean,
    estaVacio: Boolean,
    mensajeVacio: String,
    subtituloVacio: String? = null,
    iconoVacio: ImageVector? = null
) {
    if (cargando) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = HabilioTheme.AmarilloPrimario)
        }
    } else if (estaVacio) {
        HabilioTarjeta(paddingInterno = 22.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (iconoVacio != null) {
                    Icon(
                        imageVector = iconoVacio,
                        contentDescription = null,
                        tint = HabilioTheme.AmarilloPrimario,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = mensajeVacio,
                    fontWeight = if (subtituloVacio != null) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (subtituloVacio != null) HabilioTheme.TextoPrimario else HabilioTheme.TextoSecundario,
                    textAlign = TextAlign.Center
                )
                if (subtituloVacio != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtituloVacio,
                        fontSize = 13.sp,
                        color = HabilioTheme.TextoSecundario,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun HabilioBarraInferior(
    primerTabSeleccionado: Boolean,
    etiquetaPrimero: String,
    iconoPrimero: ImageVector,
    alClickPrimero: () -> Unit,
    etiquetaSegundo: String,
    iconoSegundo: ImageVector,
    alClickSegundo: () -> Unit
) {
    val coloresItem = NavigationBarItemDefaults.colors(
        selectedIconColor = HabilioTheme.AmarilloPrimario,
        selectedTextColor = HabilioTheme.AmarilloPrimario,
        unselectedIconColor = HabilioTheme.TextoSecundario,
        unselectedTextColor = HabilioTheme.TextoSecundario,
        indicatorColor = HabilioTheme.SuperficieTarjeta
    )
    NavigationBar(
        containerColor = HabilioTheme.FondoPrincipal,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = primerTabSeleccionado,
            onClick = alClickPrimero,
            icon = { Icon(iconoPrimero, contentDescription = etiquetaPrimero) },
            label = { Text(etiquetaPrimero, fontWeight = FontWeight.SemiBold) },
            colors = coloresItem
        )
        NavigationBarItem(
            selected = !primerTabSeleccionado,
            onClick = alClickSegundo,
            icon = { Icon(iconoSegundo, contentDescription = etiquetaSegundo) },
            label = { Text(etiquetaSegundo, fontWeight = FontWeight.SemiBold) },
            colors = coloresItem
        )
    }
}

@Composable
fun EncabezadoModal(
    titulo: String,
    icono: ImageVector,
    alCerrar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = HabilioTheme.AmarilloPrimario,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = titulo,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.TextoPrimario
            )
        }
        IconButton(onClick = alCerrar, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = HabilioTheme.TextoSecundario
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = HabilioTheme.BordeTarjeta)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun EncabezadoAutenticacion(
    titulo: String,
    subtitulo: String,
    textoVolver: String = "Cambiar rol",
    alVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = alVolver) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = textoVolver,
                    tint = HabilioTheme.TextoSecundario,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(textoVolver, color = HabilioTheme.TextoSecundario, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HabilioTheme.SuperficieTarjeta)
                .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            LogoGlobalHabilio(modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(12.dp))

        HabilioBadge(
            texto = "📍 PÉNJAMO, GTO. Y REGIÓN",
            colorTexto = HabilioTheme.VerdeExito,
            colorFondo = HabilioTheme.FondoVerdeSuave
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = titulo,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HabilioTheme.TextoPrimario,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitulo,
            fontSize = 14.sp,
            color = HabilioTheme.TextoSecundario,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun SelectorModoAutenticacion(
    esLoginSeleccionado: Boolean,
    alSeleccionarLogin: () -> Unit,
    alSeleccionarRegistro: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HabilioTheme.SuperficieInput)
            .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        listOf(
            Triple("Iniciar Sesión", esLoginSeleccionado, alSeleccionarLogin),
            Triple("Crear Cuenta", !esLoginSeleccionado, alSeleccionarRegistro)
        ).forEach { (texto, activo, accion) ->
            Button(
                onClick = accion,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activo) HabilioTheme.AmarilloPrimario else Color.Transparent,
                    contentColor = if (activo) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoSecundario
                ),
                elevation = null
            ) {
                Text(texto, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun HabilioCampoTexto(
    etiqueta: String,
    etiquetaDerecha: String? = null,
    colorEtiquetaDerecha: Color = HabilioTheme.TextoSecundario,
    accionEtiquetaDerecha: (() -> Unit)? = null,
    valor: String,
    alCambiarValor: (String) -> Unit,
    placeholder: String,
    iconoInicial: ImageVector? = null,
    prefijoTexto: String? = null,
    esContrasena: Boolean = false
) {
    var mostrarContrasena by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = etiqueta,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HabilioTheme.TextoPrimario
            )
            if (etiquetaDerecha != null) {
                Text(
                    text = etiquetaDerecha,
                    fontSize = 12.sp,
                    color = colorEtiquetaDerecha,
                    fontWeight = FontWeight.Medium,
                    modifier = if (accionEtiquetaDerecha != null) {
                        Modifier
                            .clickable(role = Role.Button, onClick = accionEtiquetaDerecha)
                            .padding(vertical = 4.dp)
                    } else Modifier
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = valor,
            onValueChange = alCambiarValor,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = HabilioTheme.TextoPlaceholder,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = if (prefijoTexto != null) {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                    ) {
                        Text("🇲🇽", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = prefijoTexto,
                            color = HabilioTheme.TextoPrimario,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(HabilioTheme.BordeTarjeta)
                        )
                    }
                }
            } else if (iconoInicial != null) {
                {
                    Icon(
                        imageVector = iconoInicial,
                        contentDescription = null,
                        tint = HabilioTheme.TextoSecundario
                    )
                }
            } else null,
            trailingIcon = if (esContrasena) {
                {
                    IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                        Icon(
                            imageVector = if (mostrarContrasena) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = HabilioTheme.TextoSecundario
                        )
                    }
                }
            } else null,
            visualTransformation = if (esContrasena && !mostrarContrasena) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = coloresInputHabilio()
        )
    }
}

@Composable
fun BotonPrimarioHabilio(
    texto: String,
    alClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    mostrarFlecha: Boolean = false
) {
    Button(
        onClick = alClick,
        enabled = habilitado,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HabilioTheme.AmarilloPrimario,
            contentColor = HabilioTheme.TextoSobreAmarillo
        )
    ) {
        Text(
            text = texto,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        if (mostrarFlecha) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Selector reutilizable de foto de perfil (muestra URI local nueva, fotoUrl actual o avatar por defecto).
 */
@Composable
fun SelectorFotoPerfil(
    imagenUriLocal: Uri?,
    fotoUrlActual: String?,
    titulo: String,
    subtitulo: String,
    iconoPorDefecto: ImageVector = Icons.Default.Person,
    alClickElegirFoto: () -> Unit
) {
    HabilioTarjeta(paddingInterno = 18.dp, alClick = alClickElegirFoto) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                when {
                    imagenUriLocal != null -> {
                        AsyncImage(
                            model = imagenUriLocal,
                            contentDescription = "Nueva foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(HabilioTheme.SuperficieElevada)
                        )
                    }
                    !fotoUrlActual.isNullOrBlank() -> {
                        AsyncImage(
                            model = fotoUrlActual,
                            contentDescription = "Foto de perfil actual",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(HabilioTheme.SuperficieElevada)
                        )
                    }
                    else -> {
                        AvatarUsuario(
                            tamano = 78.dp,
                            esCircular = true,
                            icono = iconoPorDefecto,
                            colorIcono = HabilioTheme.AmarilloPrimario
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(HabilioTheme.AmarilloPrimario)
                        .border(2.dp, HabilioTheme.SuperficieTarjeta, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Cambiar fotografía",
                        tint = HabilioTheme.TextoSobreAmarillo,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitulo,
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// 5. VENTANAS MODALES (Solicitar Servicio, Confirmación y Evaluar Servicio)
// ============================================================================

@Composable
fun ModalSolicitarServicio(
    trabajador: PerfilUsuario,
    direccionInicial: String,
    alCerrar: () -> Unit,
    alConfirmarSolicitud: (descripcion: String, ubicacion: String, presupuesto: Double) -> Unit
) {
    var descripcion by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf(direccionInicial) }
    var presupuestoTexto by remember { mutableStateOf("0.00") }
    var enviando by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = alCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        HabilioTarjeta(
            radioEsquina = 24.dp,
            paddingInterno = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(
                titulo = "Solicitar Servicio",
                icono = Icons.Default.Handshake,
                alCerrar = alCerrar
            )

            HabilioTarjeta(
                colorFondo = HabilioTheme.SuperficieInput,
                radioEsquina = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Trabajador seleccionado", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trabajador.nombre,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HabilioTheme.TextoPrimario
                        )
                    }
                    HabilioBadge(
                        texto = "• Disponible",
                        colorTexto = HabilioTheme.VerdeExito,
                        colorFondo = HabilioTheme.FondoVerdeSuave
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Describe qué trabajo necesitas:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = coloresInputHabilio()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Ubicación del servicio:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = ubicacion,
                onValueChange = { ubicacion = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = coloresInputHabilio()
            )

            Spacer(modifier = Modifier.height(16.dp))

            HabilioTarjeta(
                colorFondo = HabilioTheme.SuperficieInput,
                radioEsquina = 14.dp,
                paddingInterno = 14.dp,
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Text("Presupuesto estimado (acuerdo directo)", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = presupuestoTexto,
                    onValueChange = { presupuestoTexto = it },
                    prefix = {
                        Text("$", color = HabilioTheme.AmarilloPrimario, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    },
                    suffix = {
                        Text("MXN", color = HabilioTheme.AmarilloPrimario, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(
                        color = HabilioTheme.AmarilloPrimario,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    val monto = presupuestoTexto.replace(",", "").toDoubleOrNull() ?: 850.0
                    enviando = true
                    alConfirmarSolicitud(descripcion, ubicacion, monto)
                },
                enabled = !enviando && descripcion.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabilioTheme.AmarilloPrimario,
                    contentColor = HabilioTheme.TextoSobreAmarillo
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (enviando) "Enviando..." else "Confirmar y Enviar Solicitud",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun ModalSolicitudConfirmada(
    solicitud: SolicitudItem,
    alAceptarYVerEstatus: () -> Unit,
    alVolverAlInicio: () -> Unit
) {
    val folioCorto = remember(solicitud.id) {
        val digitos = solicitud.id.filter { it.isDigit() }.take(4).padEnd(4, '8')
        "#HAB-$digitos"
    }

    Dialog(
        onDismissRequest = alVolverAlInicio,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        HabilioTarjeta(
            radioEsquina = 24.dp,
            paddingInterno = 22.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = alVolverAlInicio,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(HabilioTheme.SuperficieElevada)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = HabilioTheme.TextoSecundario,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(HabilioTheme.FondoVerdeSuave)
                        .border(2.dp, HabilioTheme.VerdeExito.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(HabilioTheme.VerdeExito),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmado",
                            tint = HabilioTheme.TextoSobreAmarillo,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "¡Solicitud Registrada!",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HabilioTheme.TextoPrimario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tu solicitud ha sido enviada a ${solicitud.nombreTrabajador}. El seguimiento del trabajo y tu calificación quedan registrados en Habilio.",
                    fontSize = 14.sp,
                    color = HabilioTheme.TextoSecundario,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AvatarUsuario(
                                tamano = 42.dp,
                                esCircular = true,
                                icono = Icons.Default.Handyman,
                                colorIcono = HabilioTheme.AmarilloPrimario
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(solicitud.nombreTrabajador, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(solicitud.oficioTrabajador, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                            }
                        }
                        HabilioBadge(
                            texto = "Activo",
                            colorTexto = HabilioTheme.VerdeExito,
                            colorFondo = HabilioTheme.FondoVerdeSuave
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = HabilioTheme.BordeTarjeta)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("FOLIO", fontSize = 11.sp, color = HabilioTheme.TextoSecundario, fontWeight = FontWeight.Bold)
                            Text(
                                text = folioCorto,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PRESUPUESTO ACORDADO", fontSize = 11.sp, color = HabilioTheme.TextoSecundario, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$${String.format("%.2f", solicitud.monto)} MXN",
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = HabilioTheme.AmarilloPrimario
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = HabilioTheme.BordeTarjeta)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = HabilioTheme.VerdeExito,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "El pago se realiza directamente al especialista al verificar el trabajo",
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                BotonPrimarioHabilio(
                    texto = "Aceptar y ver estatus",
                    alClick = alAceptarYVerEstatus,
                    mostrarFlecha = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = alVolverAlInicio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HabilioTheme.SuperficieElevada,
                        contentColor = HabilioTheme.TextoPrimario
                    )
                ) {
                    Text("Volver al Inicio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ModalEvaluarServicio(
    nombreClienteInicial: String,
    alCerrar: () -> Unit,
    alPublicarResena: (estrellas: Int, autor: String, comentario: String) -> Unit
) {
    var estrellasSeleccionadas by remember { mutableStateOf(5) }
    var autor by remember { mutableStateOf(nombreClienteInicial) }
    var comentario by remember { mutableStateOf("") }
    var publicando by remember { mutableStateOf(false) }

    val etiquetaEstrellas = when (estrellasSeleccionadas) {
        5 -> "5 Estrellas · Excelente trabajo comprobado"
        4 -> "4 Estrellas · Muy buen trabajo"
        3 -> "3 Estrellas · Trabajo regular"
        2 -> "2 Estrellas · Tuvo detalles por mejorar"
        else -> "1 Estrella · Mal servicio"
    }

    Dialog(
        onDismissRequest = alCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        HabilioTarjeta(
            radioEsquina = 24.dp,
            paddingInterno = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(
                titulo = "Evaluar Servicio",
                icono = Icons.Default.Star,
                alCerrar = alCerrar
            )

            Text(
                text = "Selecciona las estrellas para calificar el trabajo:",
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                for (i in 1..5) {
                    IconButton(onClick = { estrellasSeleccionadas = i }, modifier = Modifier.size(46.dp)) {
                        Icon(
                            imageVector = if (i <= estrellasSeleccionadas) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "$i estrellas",
                            tint = HabilioTheme.AmarilloPrimario,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = etiquetaEstrellas,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.AmarilloPrimario,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text("Tu nombre:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = autor,
                onValueChange = { autor = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = coloresInputHabilio()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Recomendación o comentario del servicio:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = coloresInputHabilio()
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    publicando = true
                    alPublicarResena(estrellasSeleccionadas, autor, comentario)
                },
                enabled = !publicando && comentario.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabilioTheme.VerdeExito,
                    contentColor = HabilioTheme.TextoSobreAmarillo
                )
            ) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (publicando) "Publicando..." else "Publicar reseña",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ============================================================================
// 6. PANTALLA 1: SELECCIÓN DE ROL
// ============================================================================
@Composable
fun PantallaInicioRol(
    alElegirCliente: () -> Unit,
    alElegirTrabajador: () -> Unit
) {
    Scaffold(
        topBar = { HabilioTopBar(subtituloUbicacion = null) },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.size(100.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LogoGlobalHabilio(modifier = Modifier.fillMaxSize())
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Habilio", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HabilioBadge(
                texto = "TALENTO LOCAL COMPROBABLE",
                colorTexto = HabilioTheme.AzulVerificado,
                colorFondo = HabilioTheme.FondoAzulSuave
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Demuestra tu oficio con hechos, no con un CV",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Elige cómo deseas ingresar a Habilio para conectar habilidades reales con oportunidades en tu comunidad.",
                fontSize = 14.sp,
                color = HabilioTheme.TextoSecundario,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            TarjetaSeleccionRol(
                titulo = "Cliente",
                descripcion = "Buscar trabajadores por oficio, revisar fotos de sus trabajos y contratar con confianza.",
                icono = Icons.Default.Search,
                colorIcono = HabilioTheme.AmarilloPrimario,
                colorFondoIcono = HabilioTheme.FondoAmarilloSuave,
                alClick = alElegirCliente
            )

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaSeleccionRol(
                titulo = "Trabajador",
                descripcion = "Responder preguntas sencillas, subir fotos de mis trabajos y recibir clientes.",
                icono = Icons.Default.Build,
                colorIcono = HabilioTheme.AzulVerificado,
                colorFondoIcono = HabilioTheme.FondoAzulSuave,
                alClick = alElegirTrabajador
            )
        }
    }
}

@Composable
private fun TarjetaSeleccionRol(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    colorIcono: Color,
    colorFondoIcono: Color,
    alClick: () -> Unit
) {
    HabilioTarjeta(paddingInterno = 18.dp, alClick = alClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorFondoIcono),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(descripcion, fontSize = 14.sp, color = HabilioTheme.TextoSecundario, lineHeight = 19.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = HabilioTheme.TextoSecundario
            )
        }
    }
}

// ============================================================================
// 7. PANTALLAS 2 Y 4 UNIFICADAS: LOGIN
// ============================================================================
@Composable
fun PantallaLoginUnificada(
    esModoTrabajador: Boolean,
    alIrARegistro: () -> Unit,
    alLoginExitoso: (PerfilUsuario) -> Unit,
    alCambiarRolOpuesto: () -> Unit,
    alVolverInicio: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var telefonoOCorreo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Bienvenido a Habilio",
            subtitulo = "Encuentra y contrata a maestros de oficios locales con evidencia real de trabajo.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = true,
            alSeleccionarLogin = {},
            alSeleccionarRegistro = alIrARegistro
        )

        Spacer(modifier = Modifier.height(20.dp))

        HabilioTarjeta(paddingInterno = 18.dp) {
            HabilioCampoTexto(
                etiqueta = "Teléfono móvil o Correo",
                etiquetaDerecha = "10 dígitos o correo",
                valor = telefonoOCorreo,
                alCambiarValor = { telefonoOCorreo = it },
                placeholder = "469 123 4567 o tu@correo.com",
                iconoInicial=Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(16.dp))
            HabilioCampoTexto(
                etiqueta = "Contraseña",
                valor = contrasena,
                alCambiarValor = { contrasena = it },
                placeholder = "••••••••••••",
                iconoInicial = Icons.Outlined.Lock,
                esContrasena = true
            )

            if (mensajeError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(mensajeError!!, color = HabilioTheme.RojoUrgente, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(22.dp))

            val textoBoton = if (cargando) {
                "Verificando credenciales..."
            } else if (esModoTrabajador) {
                "Continuar como trabajador"
            } else {
                "Continuar como Cliente"
            }

            BotonPrimarioHabilio(
                texto = textoBoton,
                habilitado = !cargando && telefonoOCorreo.isNotBlank() && contrasena.isNotBlank(),
                alClick = {
                    cargando = true
                    mensajeError = null
                    scope.launch {
                        val res = SupabaseService.iniciarSesion(telefonoOCorreo, contrasena)
                        cargando = false
                        res.onSuccess(alLoginExitoso).onFailure {
                            mensajeError = it.message ?: "Error al iniciar sesión"
                        }
                    }
                },
                mostrarFlecha = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (esModoTrabajador) {
            TarjetaCambioACliente(alClick = alCambiarRolOpuesto)
        } else {
            TarjetaCambioATrabajador(alClick = alCambiarRolOpuesto)
        }
    }
}

@Composable
private fun TarjetaCambioATrabajador(alClick: () -> Unit) {
    HabilioTarjeta(paddingInterno = 18.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = HabilioTheme.AmarilloPrimario, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("¿Haces trabajos de oficio?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Si eres albañil, plomero, electricista, costurera o carpintero en Guanajuato:",
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = alClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = HabilioTheme.SuperficieElevada,
                    contentColor = HabilioTheme.TextoPrimario
                )
            ) {
                Text("Ingresar como Trabajador", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TarjetaCambioACliente(alClick: () -> Unit) {
    HabilioTarjeta(paddingInterno = 16.dp, alClick = alClick) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("¿Buscas contratar servicios para tu hogar?", fontSize = 13.sp, color = HabilioTheme.TextoSecundario)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ingresar o registrarme como Cliente >",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.AmarilloPrimario
            )
        }
    }
}

// ============================================================================
// 8. PANTALLA 3: REGISTRO CLIENTE
// ============================================================================
@Composable
fun PantallaRegistroCliente(
    alIrALogin: () -> Unit,
    alRegistroExitoso: (PerfilUsuario) -> Unit,
    alCambiarATrabajador: () -> Unit,
    alVolverInicio: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    var imagenSeleccionada by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> imagenSeleccionada = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Bienvenido a Habilio",
            subtitulo = "Encuentra y contrata a maestros de oficios locales con evidencia real de trabajo.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = false,
            alSeleccionarLogin = alIrALogin,
            alSeleccionarRegistro = {}
        )

        Spacer(modifier = Modifier.height(18.dp))

        SelectorFotoPerfil(
            imagenUriLocal = imagenSeleccionada,
            fotoUrlActual = null,
            titulo = "Foto de Perfil",
            subtitulo = "Toca aquí para elegir una imagen desde tu galería (se comprime automáticamente).",
            iconoPorDefecto = Icons.Default.Person,
            alClickElegirFoto = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        HabilioTarjeta(paddingInterno = 18.dp) {
            HabilioCampoTexto(
                etiqueta = "Nombre Completo",
                etiquetaDerecha = "Requerido",
                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                valor = nombre,
                alCambiarValor = { nombre = it },
                placeholder = "Ej. Juan Pérez o Familia López",
                iconoInicial = Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Teléfono móvil (10 dígitos)",
                etiquetaDerecha = "Requerido",
                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                valor = telefono,
                alCambiarValor = { telefono = it },
                placeholder = "469 123 4567",
                iconoInicial=Icons.Outlined.Phone
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Correo electrónico",
                etiquetaDerecha = "Requerido",
                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                valor = correo,
                alCambiarValor = { correo = it },
                placeholder = "tu@correo.com",
                iconoInicial = Icons.Outlined.Email
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Crear Contraseña",
                valor = contrasena,
                alCambiarValor = { contrasena = it },
                placeholder = "Mínimo 6 caracteres",
                iconoInicial = Icons.Outlined.Lock,
                esContrasena = true
            )

            if (mensajeError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(mensajeError!!, color = HabilioTheme.RojoUrgente, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "🔒 Aviso de privacidad: Tus datos de contacto se utilizan únicamente dentro de Habilio para coordinar solicitudes de servicio en tu comunidad.",
                fontSize = 11.sp,
                color = HabilioTheme.TextoSecundario,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            BotonPrimarioHabilio(
                texto = if (cargando) "Creando cuenta..." else "Crear Cuenta de Cliente",
                habilitado = !cargando && nombre.isNotBlank() && telefono.filter { it.isDigit() }.length >= 10 && contrasena.length >= 6,
                alClick = {
                    cargando = true
                    mensajeError = null
                    scope.launch {
                        var urlFotoPublica: String? = null
                        if (imagenSeleccionada != null) {
                            urlFotoPublica = SupabaseService.subirFotoStorage(contexto, imagenSeleccionada!!)
                        }

                        val res = SupabaseService.registrarUsuario(
                            rol = "cliente",
                            nombre = nombre,
                            telefono = telefono,
                            correoInput = correo,
                            contrasena = contrasena,
                            fotoUrl = urlFotoPublica
                        )
                        cargando = false
                        res.onSuccess(alRegistroExitoso).onFailure {
                            mensajeError = it.message ?: "Error al registrar cliente"
                        }
                    }
                },
                mostrarFlecha = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        TarjetaCambioATrabajador(alClick = alCambiarATrabajador)
    }
}

// ============================================================================
// 9. PANTALLA 5: REGISTRO TRABAJADOR
// ============================================================================
@Composable
fun PantallaRegistroTrabajador(
    alIrALogin: () -> Unit,
    alRegistroExitoso: (PerfilUsuario) -> Unit,
    alCambiarACliente: () -> Unit,
    alVolverInicio: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }
    var nombreTaller by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var oficioSeleccionado by remember { mutableStateOf("Albañilería") }
    var especialidad by remember { mutableStateOf("") }
    var zonaServicio by remember { mutableStateOf("Pénjamo Centro") }
    var modalidadSeleccionada by remember { mutableStateOf(0) }
    var direccionTaller by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    var imagenSeleccionada by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> imagenSeleccionada = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Únete como Trabajador",
            subtitulo = "Publica tus trabajos con evidencia real, recibe solicitudes directas y haz crecer tu taller.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(18.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = false,
            alSeleccionarLogin = alIrALogin,
            alSeleccionarRegistro = {}
        )

        Spacer(modifier = Modifier.height(18.dp))

        SelectorFotoPerfil(
            imagenUriLocal = imagenSeleccionada,
            fotoUrlActual = null,
            titulo = "Foto de Perfil Profesional",
            subtitulo = "Toca para subir una foto de tu rostro o taller (se comprime automáticamente para ahorrar datos).",
            iconoPorDefecto = Icons.Default.Handyman,
            alClickElegirFoto = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text("👤 DATOS DE CONTACTO", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = HabilioTheme.AmarilloPrimario)
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Nombre Completo",
            valor = nombre,
            alCambiarValor = { nombre = it },
            placeholder = "Ej. Roberto Méndez",
            iconoInicial = Icons.Outlined.Person
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Nombre del Taller / Negocio",
            etiquetaDerecha = "(Opcional)",
            valor = nombreTaller,
            alCambiarValor = { nombreTaller = it },
            placeholder = "Ej. Herrería El Sol o Taller Méndez",
            iconoInicial = Icons.Outlined.Storefront
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Teléfono Celular (10 dígitos)",
            valor = telefono,
            alCambiarValor = { telefono = it },
            placeholder = "469 123 4567",
            iconoInicial = Icons.Outlined.Phone
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Correo Electrónico",
            valor = correo,
            alCambiarValor = { correo = it },
            placeholder = "maestro@correo.com",
            iconoInicial = Icons.Outlined.Email
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Crear Contraseña Segura",
            valor = contrasena,
            alCambiarValor = { contrasena = it },
            placeholder = "Mínimo 6 caracteres",
            iconoInicial = Icons.Outlined.Lock,
            esContrasena = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛠️ OFICIO PRINCIPAL", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = HabilioTheme.AmarilloPrimario)
            Text("Elige tu especialidad", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.AmarilloPrimario)
        }
        Spacer(modifier = Modifier.height(10.dp))

        OFICIOS_HABILIO.chunked(2).forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                fila.forEach { (tituloOficio, subOficio, iconoOficio) ->
                    val seleccionado = oficioSeleccionado == tituloOficio
                    Surface(
                        onClick = { oficioSeleccionado = tituloOficio },
                        shape = RoundedCornerShape(12.dp),
                        color = if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.SuperficieTarjeta,
                        border = BorderStroke(1.dp, if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeTarjeta),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconoOficio,
                                contentDescription = null,
                                tint = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.AmarilloPrimario,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = tituloOficio,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoPrimario
                                )
                                Text(
                                    text = subOficio,
                                    fontSize = 12.sp,
                                    color = if (seleccionado) HabilioTheme.TextoSobreAmarillo.copy(alpha = 0.8f) else HabilioTheme.TextoSecundario
                                )
                            }
                        }
                    }
                }
                if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Especialidad o Servicios Específicos",
            valor = especialidad,
            alCambiarValor = { especialidad = it },
            placeholder = "Ej. Instalación de vitropiso, boiler solar, soldadura..."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("📍 COBERTURA Y MODALIDAD DE TRABAJO", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = HabilioTheme.AmarilloPrimario)
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Zona de Servicio Habitual",
            valor = zonaServicio,
            alCambiarValor = { zonaServicio = it },
            placeholder = "Ej. Pénjamo Centro, Santa Ana Pacueco, Col. Juárez",
            iconoInicial = Icons.Outlined.Explore
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text("¿Cómo atiendes a tus clientes?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        MODALIDADES_HABILIO.forEachIndexed { indice, (tituloMod, descMod) ->
            val activo = modalidadSeleccionada == indice
            HabilioTarjeta(
                radioEsquina = 12.dp,
                paddingInterno = 14.dp,
                colorBorde = if (activo) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeTarjeta,
                alClick = { modalidadSeleccionada = indice },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = activo,
                        onClick = { modalidadSeleccionada = indice },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = HabilioTheme.AmarilloPrimario,
                            unselectedColor = HabilioTheme.TextoSecundario
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(tituloMod, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(descMod, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Dirección o Referencia del Taller",
            etiquetaDerecha = "(Opcional)",
            valor = direccionTaller,
            alCambiarValor = { direccionTaller = it },
            placeholder = "Ej. Calle Manuel Doblado #45, Col. Los Fresnos",
            iconoInicial = Icons.Outlined.Store
        )

        if (mensajeError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(mensajeError!!, color = HabilioTheme.RojoUrgente, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "🔒 Aviso de privacidad: Tus datos de perfil y fotografías de portafolio serán visibles para clientes de tu comunidad dentro de Habilio.",
            fontSize = 11.sp,
            color = HabilioTheme.TextoSecundario,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        BotonPrimarioHabilio(
            texto = if (cargando) "Registrando trabajador..." else "Registrarme como trabajador",
            habilitado = !cargando && nombre.isNotBlank() && telefono.filter { it.isDigit() }.length >= 10 && contrasena.length >= 6,
            alClick = {
                cargando = true
                mensajeError = null
                scope.launch {
                    var urlFotoPublica: String? = null
                    if (imagenSeleccionada != null) {
                        urlFotoPublica = SupabaseService.subirFotoStorage(contexto, imagenSeleccionada!!)
                    }

                    val res = SupabaseService.registrarUsuario(
                        rol = "trabajador",
                        nombre = nombre,
                        telefono = telefono,
                        correoInput = correo,
                        contrasena = contrasena,
                        zona = zonaServicio,
                        direccion = direccionTaller.ifBlank { zonaServicio },
                        nombreTaller = nombreTaller,
                        oficio = oficioSeleccionado,
                        especialidad = especialidad,
                        modalidad = MODALIDADES_HABILIO[modalidadSeleccionada].first,
                        fotoUrl = urlFotoPublica
                    )
                    cargando = false
                    res.onSuccess(alRegistroExitoso).onFailure {
                        mensajeError = it.message ?: "Error al registrar trabajador"
                    }
                }
            },
            mostrarFlecha = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        TarjetaCambioACliente(alClick = alCambiarACliente)
    }
}

// ============================================================================
// 10. NUEVA PANTALLA: EDITAR PERFIL (Para Cliente y Trabajador)
// ============================================================================
@Composable
fun PantallaEditarPerfil(
    usuarioActual: PerfilUsuario,
    alGuardarExitoso: (PerfilUsuario) -> Unit,
    alCancelar: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val esTrabajador = usuarioActual.rol == "trabajador"

    // Campos precargados con los datos actuales del usuario
    var nombre by remember(usuarioActual) { mutableStateOf(usuarioActual.nombre) }
    var telefono by remember(usuarioActual) { mutableStateOf(usuarioActual.telefono) }
    var zona by remember(usuarioActual) { mutableStateOf(usuarioActual.zona) }
    var direccion by remember(usuarioActual) { mutableStateOf(usuarioActual.direccion) }
    var nombreTaller by remember(usuarioActual) { mutableStateOf(usuarioActual.nombreTaller) }
    var oficioSeleccionado by remember(usuarioActual) {
        mutableStateOf(usuarioActual.oficio.ifBlank { "Albañilería" })
    }
    var especialidad by remember(usuarioActual) { mutableStateOf(usuarioActual.especialidad) }
    var modalidadSeleccionada by remember(usuarioActual.id, usuarioActual.modalidad) {
        val modGuardada = usuarioActual.modalidad.trim()
        val idx = when {
            modGuardada.contains("Ambos", ignoreCase = true) -> 2
            modGuardada.contains("Taller", ignoreCase = true) ||
                    modGuardada.contains("Local", ignoreCase = true) -> 1
            modGuardada.contains("Domicilio", ignoreCase = true) -> 0
            else -> MODALIDADES_HABILIO.indexOfFirst {
                it.first.equals(modGuardada, ignoreCase = true)
            }.coerceAtLeast(0)
        }
        mutableStateOf(idx)
    }

    var nuevaImagenUri by remember { mutableStateOf<Uri?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) nuevaImagenUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Editar Mi Perfil",
            subtitulo = "Actualiza tu fotografía o modifica tus datos de contacto y cobertura.",
            textoVolver = "Volver a mi perfil",
            alVolver = alCancelar
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorFotoPerfil(
            imagenUriLocal = nuevaImagenUri,
            fotoUrlActual = usuarioActual.fotoUrl,
            titulo = "Fotografía de Perfil Actual",
            subtitulo = "Toca la imagen para subir una foto nueva desde tu dispositivo.",
            iconoPorDefecto = if (esTrabajador) Icons.Default.Handyman else Icons.Default.Person,
            alClickElegirFoto = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        HabilioTarjeta(paddingInterno = 18.dp) {
            Text(
                text = "👤 DATOS GENERALES",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.AmarilloPrimario
            )
            Spacer(modifier = Modifier.height(12.dp))

            HabilioCampoTexto(
                etiqueta = "Nombre Completo",
                valor = nombre,
                alCambiarValor = { nombre = it },
                placeholder = "Tu nombre completo",
                iconoInicial = Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Teléfono móvil (10 dígitos)",
                valor = telefono,
                alCambiarValor = { telefono = it },
                placeholder = "469 123 4567",
                prefijoTexto = "+52"
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Zona o Colonia",
                valor = zona,
                alCambiarValor = { zona = it },
                placeholder = "Ej. Pénjamo Centro, Santa Ana Pacueco",
                iconoInicial = Icons.Outlined.Explore
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = if (esTrabajador) "Dirección o Referencia del Taller" else "Dirección de Domicilio",
                valor = direccion,
                alCambiarValor = { direccion = it },
                placeholder = "Ej. Calle Hidalgo #114, Zona Centro",
                iconoInicial = Icons.Outlined.LocationOn
            )
        }

        if (esTrabajador) {
            Spacer(modifier = Modifier.height(20.dp))

            HabilioTarjeta(paddingInterno = 18.dp) {
                Text(
                    text = "🛠️ DATOS DE TU TALLER Y OFICIO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HabilioTheme.AmarilloPrimario
                )
                Spacer(modifier = Modifier.height(12.dp))

                HabilioCampoTexto(
                    etiqueta = "Nombre del Taller / Negocio",
                    etiquetaDerecha = "(Opcional)",
                    valor = nombreTaller,
                    alCambiarValor = { nombreTaller = it },
                    placeholder = "Ej. Taller Méndez",
                    iconoInicial = Icons.Outlined.Storefront
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Oficio Principal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                OFICIOS_HABILIO.chunked(2).forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        fila.forEach { (tituloOficio, subOficio, iconoOficio) ->
                            val seleccionado = oficioSeleccionado == tituloOficio
                            Surface(
                                onClick = { oficioSeleccionado = tituloOficio },
                                shape = RoundedCornerShape(12.dp),
                                color = if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.SuperficieInput,
                                border = BorderStroke(
                                    1.dp,
                                    if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeTarjeta
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = iconoOficio,
                                        contentDescription = null,
                                        tint = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.AmarilloPrimario,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = tituloOficio,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoPrimario
                                        )
                                        Text(
                                            text = subOficio,
                                            fontSize = 10.sp,
                                            color = if (seleccionado) HabilioTheme.TextoSobreAmarillo.copy(alpha = 0.8f) else HabilioTheme.TextoSecundario
                                        )
                                    }
                                }
                            }
                        }
                        if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HabilioCampoTexto(
                    etiqueta = "Especialidad o Servicios Específicos",
                    valor = especialidad,
                    alCambiarValor = { especialidad = it },
                    placeholder = "Ej. Instalación de vitropiso, boiler solar..."
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Modalidad de Atención", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                MODALIDADES_HABILIO.forEachIndexed { indice, (tituloMod, descMod) ->
                    val activo = modalidadSeleccionada == indice
                    HabilioTarjeta(
                        radioEsquina = 12.dp,
                        paddingInterno = 12.dp,
                        colorFondo = HabilioTheme.SuperficieInput,
                        colorBorde = if (activo) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeTarjeta,
                        alClick = { modalidadSeleccionada = indice },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = activo,
                                onClick = { modalidadSeleccionada = indice },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = HabilioTheme.AmarilloPrimario,
                                    unselectedColor = HabilioTheme.TextoSecundario
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(tituloMod, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(descMod, fontSize = 11.sp, color = HabilioTheme.TextoSecundario)
                            }
                        }
                    }
                }
            }
        }

        if (mensajeError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(mensajeError!!, color = HabilioTheme.RojoUrgente, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(22.dp))

        BotonPrimarioHabilio(
            texto = if (guardando) "Guardando cambios..." else "Guardar Cambios",
            habilitado = !guardando && nombre.isNotBlank() && telefono.filter { it.isDigit() }.length >= 10,
            alClick = {
                guardando = true
                mensajeError = null
                scope.launch {
                    var urlFotoFinal: String? = usuarioActual.fotoUrl.ifBlank { null }
                    if (nuevaImagenUri != null) {
                        val subida = SupabaseService.subirFotoStorage(contexto, nuevaImagenUri!!)
                        if (subida != null) urlFotoFinal = subida
                    }

                    val res = SupabaseService.actualizarPerfil(
                        id = usuarioActual.id,
                        rol = usuarioActual.rol,
                        nombre = nombre,
                        telefono = telefono,
                        zona = zona,
                        direccion = direccion,
                        nombreTaller = nombreTaller,
                        oficio = oficioSeleccionado,
                        especialidad = especialidad,
                        modalidad = MODALIDADES_HABILIO[modalidadSeleccionada].first,
                        fotoUrl = urlFotoFinal
                    )
                    guardando = false
                    res.onSuccess(alGuardarExitoso).onFailure {
                        mensajeError = it.message ?: "Error al actualizar el perfil"
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = alCancelar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta)
        ) {
            Text("Cancelar", color = HabilioTheme.TextoSecundario, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ============================================================================
// 11. PANTALLA 6: BUSCADOR DE ESPECIALISTAS DINÁMICO (Filtro por oficio y zona)
// ============================================================================
@Composable
fun PantallaBuscadorCliente(
    trabajadores: List<PerfilUsuario>,
    evidencias: List<EvidenciaItem>,
    resenas: List<ResenaItem>,
    cargando: Boolean,
    alRefrescar: () -> Unit,
    alVerEspecialista: (PerfilUsuario) -> Unit,
    alClickFotoEvidencia: (EvidenciaItem) -> Unit,
    alIrAMiPerfil: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    var categoriaActiva by remember { mutableStateOf("Todos") }
    var zonaActiva by remember { mutableStateOf("Todas las zonas") }

    val categorias = listOf("Todos", "Albañilería", "Plomería", "Electricidad", "Costura", "Carpintería", "Herrería", "Pintura")

    val zonasDisponibles = listOf("Todas las zonas", "Pénjamo Centro", "Santa Ana Pacueco", "Col. Juárez", "Col. Lindavista")

    val trabajadoresFiltrados = remember(trabajadores, busqueda, categoriaActiva, zonaActiva) {
        trabajadores.filter { t ->
            val coincideCat = categoriaActiva == "Todos" ||
                    t.oficio.contains(categoriaActiva, ignoreCase = true) ||
                    t.especialidad.contains(categoriaActiva, ignoreCase = true)
            val coincideZona = zonaActiva == "Todas las zonas" ||
                    t.zona.contains(zonaActiva, ignoreCase = true) ||
                    t.direccion.contains(zonaActiva, ignoreCase = true)
            val coincideTexto = busqueda.isBlank() ||
                    t.nombre.contains(busqueda, ignoreCase = true) ||
                    t.oficio.contains(busqueda, ignoreCase = true) ||
                    t.especialidad.contains(busqueda, ignoreCase = true) ||
                    t.zona.contains(busqueda, ignoreCase = true) ||
                    t.descripcion.contains(busqueda, ignoreCase = true)
            coincideCat && coincideZona && coincideTexto
        }
    }

    val conEvidenciaReal = remember(trabajadoresFiltrados, evidencias) {
        trabajadoresFiltrados.count { t -> evidencias.any { it.trabajadorId == t.id } }
    }

    Scaffold(
        topBar = { HabilioTopBar(subtituloUbicacion = zonaActiva, alAccionDerecha = alCambiarRol) },
        bottomBar = {
            HabilioBarraInferior(
                primerTabSeleccionado = true,
                etiquetaPrimero = "Explorar",
                iconoPrimero = Icons.Default.Search,
                alClickPrimero = alRefrescar,
                etiquetaSegundo = "Mi Perfil",
                iconoSegundo = Icons.Outlined.Person,
                alClickSegundo = alIrAMiPerfil
            )
        },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("FILTRAR POR ZONA DE SERVICIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = HabilioTheme.RojoUrgente, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(zonaActiva, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                TextButton(onClick = alRefrescar) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = HabilioTheme.AmarilloPrimario, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Actualizar", color = HabilioTheme.AmarilloPrimario, fontSize = 13.sp)
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                zonasDisponibles.forEach { z ->
                    val sel = zonaActiva == z
                    AssistChip(
                        onClick = { zonaActiva = z },
                        label = { Text(z, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sel) HabilioTheme.FondoAzulSuave else HabilioTheme.SuperficieTarjeta,
                            labelColor = if (sel) HabilioTheme.AzulVerificado else HabilioTheme.TextoSecundario
                        ),
                        border = BorderStroke(1.dp, if (sel) HabilioTheme.AzulVerificado else HabilioTheme.BordeTarjeta)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Buscar albañil, costurera, plomero o colonia...", color = HabilioTheme.TextoPlaceholder, fontSize = 14.sp)
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HabilioTheme.TextoSecundario) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = HabilioTheme.SuperficieTarjeta,
                    unfocusedContainerColor = HabilioTheme.SuperficieTarjeta,
                    focusedBorderColor = HabilioTheme.AmarilloPrimario,
                    unfocusedBorderColor = HabilioTheme.BordeTarjeta
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categorias.forEach { cat ->
                    val seleccionado = categoriaActiva == cat
                    FilterChip(
                        selected = seleccionado,
                        onClick = { categoriaActiva = cat },
                        label = { Text(cat, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HabilioTheme.AmarilloPrimario,
                            selectedLabelColor = HabilioTheme.TextoSobreAmarillo,
                            containerColor = HabilioTheme.SuperficieTarjeta,
                            labelColor = HabilioTheme.TextoPrimario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = seleccionado,
                            borderColor = HabilioTheme.BordeTarjeta
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HabilioTarjeta(
                colorFondo = HabilioTheme.FondoAzulSuave,
                colorBorde = HabilioTheme.AzulVerificado.copy(alpha = 0.4f),
                radioEsquina = 10.dp,
                paddingInterno = 12.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = HabilioTheme.AzulVerificado, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${trabajadoresFiltrados.size} especialistas encontrados ($conEvidenciaReal con portafolio fotográfico)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EstadoCargaOVacio(
                cargando = cargando,
                estaVacio = trabajadoresFiltrados.isEmpty(),
                mensajeVacio = "No hay trabajadores registrados con estos filtros",
                subtituloVacio = "Prueba cambiando la zona o categoría, o registra un especialista nuevo.",
                iconoVacio = Icons.Default.PersonSearch
            )

            if (!cargando) {
                trabajadoresFiltrados.forEach { esp ->
                    TarjetaEspecialistaBuscador(
                        especialista = esp,
                        evidencias = evidencias.filter { it.trabajadorId == esp.id },
                        cantidadResenas = resenas.count { it.trabajadorId == esp.id },
                        alClickFotoEvidencia = alClickFotoEvidencia,
                        alVerDetalle = { alVerEspecialista(esp) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TarjetaEspecialistaBuscador(
    especialista: PerfilUsuario,
    evidencias: List<EvidenciaItem>,
    cantidadResenas: Int,
    alClickFotoEvidencia: (EvidenciaItem) -> Unit,
    alVerDetalle: () -> Unit
) {
    // La verificación y reputación dependen de evidencias reales y reseñas
    val tieneEvidenciaComprobable = evidencias.isNotEmpty()

    HabilioTarjeta {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            AvatarUsuario(mostrarPuntoDisponible = true, fotoUrl = especialista.fotoUrl)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(especialista.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (tieneEvidenciaComprobable) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Con evidencia comprobable",
                            tint = HabilioTheme.AzulVerificado,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = especialista.oficio +
                            if (especialista.especialidad.isNotBlank()) " • ${especialista.especialidad}" else "",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HabilioTheme.AmarilloPrimario,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("📍 ${especialista.zona}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (tieneEvidenciaComprobable) {
                    HabilioBadge(
                        texto = "✓ CON EVIDENCIA",
                        colorTexto = HabilioTheme.AmarilloPrimario,
                        colorFondo = HabilioTheme.FondoAmarilloSuave
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★", color = HabilioTheme.AmarilloPrimario, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    if (cantidadResenas > 0) {
                        Text(String.format("%.1f", especialista.calificacion), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("($cantidadResenas)", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    } else {
                        Text("Sin reseñas", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = construirResumenLimpio(especialista),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (index in 0 until 3) {
                val ev = evidencias.getOrNull(index)
                val tieneFoto = !ev?.fotoUrl.isNullOrBlank()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HabilioTheme.SuperficieElevada)
                        .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(8.dp))
                        .clickable(enabled = tieneFoto) {
                            if (ev != null) alClickFotoEvidencia(ev)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (tieneFoto && ev != null) {
                        AsyncImage(
                            model = ev.fotoUrl,
                            contentDescription = ev.titulo,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = if (ev != null) HabilioTheme.AmarilloPrimario else HabilioTheme.TextoSecundario
                            )
                            Text(
                                text = ev?.titulo ?: "Sin foto",
                                fontSize = 10.sp,
                                color = HabilioTheme.TextoSecundario,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✓ ${especialista.modalidad}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = HabilioTheme.TextoSecundario,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = alVerDetalle,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabilioTheme.AmarilloPrimario,
                    contentColor = HabilioTheme.TextoSobreAmarillo
                ),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text("Ver especialista", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }
}

private fun construirResumenLimpio(esp: PerfilUsuario): String {
    val tallerTexto = if (esp.nombreTaller.isNotBlank()) " (${esp.nombreTaller})" else ""
    return "Maestro especialista en ${esp.oficio}$tallerTexto con atención en ${esp.zona}."
}

// ============================================================================
// 12. PANTALLA 7: DETALLE DE ESPECIALISTA
// ============================================================================
@Composable
fun PantallaDetalleEspecialista(
    especialista: PerfilUsuario,
    evidencias: List<EvidenciaItem>,
    resenas: List<ResenaItem>,
    totalServicios: Int,
    puedeCalificar: Boolean,
    alVolverAlListado: () -> Unit,
    alAbrirModalSolicitar: () -> Unit,
    alAbrirModalCalificar: () -> Unit,
    alClickFotoEvidencia: (EvidenciaItem) -> Unit,
    alIrAExplorar: () -> Unit,
    alIrAMiPerfil: () -> Unit
) {
    val tieneEvidencia = evidencias.isNotEmpty()

    Scaffold(
        topBar = { HabilioTopBar(subtituloUbicacion = especialista.zona) },
        bottomBar = {
            HabilioBarraInferior(
                primerTabSeleccionado = true,
                etiquetaPrimero = "Explorar",
                iconoPrimero = Icons.Default.Search,
                alClickPrimero = alIrAExplorar,
                etiquetaSegundo = "Mi Perfil",
                iconoSegundo = Icons.Outlined.Person,
                alClickSegundo = alIrAMiPerfil
            )
        },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = alVolverAlListado, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = HabilioTheme.TextoPrimario, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Volver al listado", color = HabilioTheme.TextoPrimario, fontSize = 14.sp)
                }

                HabilioBadge(
                    texto = if (tieneEvidencia) "✓ Evidencia Comprobable" else "Perfil Registrado",
                    colorTexto = if (tieneEvidencia) HabilioTheme.AzulVerificado else HabilioTheme.TextoSecundario,
                    colorFondo = if (tieneEvidencia) HabilioTheme.FondoAzulSuave else HabilioTheme.SuperficieElevada
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HabilioTarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarUsuario(tamano = 64.dp, fotoUrl = especialista.fotoUrl)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("● Disponible hoy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.VerdeExito)
                        Text(especialista.nombre, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = especialista.oficio,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HabilioTheme.AmarilloPrimario
                        )
                        Text(especialista.zona, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                    if (tieneEvidencia) {
                        Spacer(modifier = Modifier.width(8.dp))
                        HabilioBadge(
                            texto = "★ VERIFICADO",
                            colorTexto = HabilioTheme.TextoSobreAmarillo,
                            colorFondo = HabilioTheme.AmarilloPrimario
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CajaMetricaDetalle(
                        valor = if (resenas.isNotEmpty()) "${String.format("%.1f", especialista.calificacion)} ★" else "Nuevo",
                        etiqueta = "${resenas.size} reseñas",
                        colorValor = HabilioTheme.AmarilloPrimario,
                        modifier = Modifier.weight(1f)
                    )
                    CajaMetricaDetalle(
                        valor = "$totalServicios",
                        etiqueta = "Servicios",
                        colorValor = HabilioTheme.VerdeExito,
                        modifier = Modifier.weight(1f)
                    )
                    CajaMetricaDetalle(
                        valor = "${evidencias.size} fotos",
                        etiqueta = "Evidencias",
                        colorValor = HabilioTheme.AzulVerificado,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HabilioTarjeta(
                colorFondo = HabilioTheme.FondoAzulSuave.copy(alpha = 0.6f),
                colorBorde = HabilioTheme.AzulVerificado.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "✦ Resumen de Experiencia Práctica",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HabilioTheme.AzulVerificado
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = construirResumenLimpio(especialista),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EtiquetaCompleta("🛠️ ${especialista.oficio}", Modifier.weight(1f))
                    EtiquetaCompleta(especialista.modalidad, Modifier.weight(1f))
                }

                if (especialista.especialidad.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HabilioTarjeta(
                        colorFondo = HabilioTheme.SuperficieInput,
                        radioEsquina = 10.dp,
                        paddingInterno = 12.dp
                    ) {
                        Text(
                            text = "Especialidad y Servicios Específicos:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HabilioTheme.AmarilloPrimario
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = especialista.especialidad,
                            fontSize = 13.sp,
                            color = HabilioTheme.TextoPrimario,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            BotonPrimarioHabilio(
                texto = "Solicitar Servicio",
                alClick = alAbrirModalSolicitar
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Solo permitir calificar cuando exista un servicio completado con este trabajador


            Spacer(modifier = Modifier.height(22.dp))

            Text("Portafolio de trabajos reales (${evidencias.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            EstadoCargaOVacio(
                cargando = false,
                estaVacio = evidencias.isEmpty(),
                mensajeVacio = "Este trabajador aún no ha subido fotografías a su portafolio."
            )

            evidencias.forEach { ev ->
                val tieneFoto = !ev.fotoUrl.isNullOrBlank()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HabilioTheme.SuperficieTarjeta,
                    border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clickable(enabled = tieneFoto) { alClickFotoEvidencia(ev) }
                ) {
                    Column {
                        if (tieneFoto) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = ev.fotoUrl,
                                    contentDescription = ev.titulo,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(HabilioTheme.SuperficieElevada)
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = "🔍 Toca para ampliar",
                                        fontSize = 11.sp,
                                        color = HabilioTheme.TextoPrimario,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(HabilioTheme.SuperficieElevada),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoCamera,
                                        contentDescription = null,
                                        tint = HabilioTheme.AmarilloPrimario,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Evidencia registrada sin fotografía",
                                        fontSize = 12.sp,
                                        color = HabilioTheme.TextoSecundario
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(ev.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("✓ ${ev.fechaPublicacion}", fontSize = 12.sp, color = HabilioTheme.VerdeExito, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recomendaciones (${resenas.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (puedeCalificar) {
                    TextButton(onClick = alAbrirModalCalificar) {
                        Text("+ Escribir reseña", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.AmarilloPrimario)
                    }
                }
            }

            EstadoCargaOVacio(
                cargando = false,
                estaVacio = resenas.isEmpty(),
                mensajeVacio = "Aún no hay reseñas registradas para ${especialista.nombre}."
            )

            resenas.forEach { r ->
                TarjetaResena(
                    autor = r.autorNombre,
                    comentario = "\"${r.comentario}\"",
                    estrellas = r.estrellas,
                    fecha = r.fechaTexto
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CajaMetricaDetalle(
    valor: String,
    etiqueta: String,
    colorValor: Color,
    modifier: Modifier = Modifier
) {
    HabilioTarjeta(
        colorFondo = HabilioTheme.SuperficieInput,
        radioEsquina = 10.dp,
        paddingInterno = 10.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = valor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = colorValor)
            Text(text = etiqueta, fontSize = 12.sp, color = HabilioTheme.TextoSecundario, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EtiquetaCompleta(texto: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = HabilioTheme.SuperficieInput,
        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
        modifier = modifier
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            color = HabilioTheme.TextoPrimario,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
        )
    }
}

@Composable
private fun TarjetaResena(autor: String, comentario: String, estrellas: Int, fecha: String) {
    HabilioTarjeta(radioEsquina = 14.dp, paddingInterno = 14.dp) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = autor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "★".repeat(estrellas.coerceIn(1, 5)), color = HabilioTheme.AmarilloPrimario, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = comentario, fontSize = 13.sp, fontStyle = FontStyle.Italic, lineHeight = 19.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = fecha, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
    }
}

// ============================================================================
// 13. PANTALLA 8: PERFIL DEL CLIENTE DINÁMICO (Con botón Editar Perfil)
// ============================================================================
@Composable
fun PantallaPerfilCliente(
    cliente: PerfilUsuario?,
    solicitudes: List<SolicitudItem>,
    alEditarPerfil: () -> Unit,
    alVerSeguimientoSolicitud: (SolicitudItem) -> Unit,
    alLiberarPago: (SolicitudItem) -> Unit,
    alCalificarTrabajador: (SolicitudItem) -> Unit,
    alIrAExplorar: () -> Unit,
    alCambiarRol: () -> Unit
) {
    val totalPedidos = solicitudes.size
    val totalCompletados = solicitudes.count { it.estado == "completado" }
    val enCurso = solicitudes.filter {
        it.estado == "pendiente" || it.estado == "en_ejecucion" || it.estado == "esperando_confirmacion"
    }
    val montoAcordado = enCurso.sumOf { it.monto }

    Scaffold(
        topBar = {
            HabilioTopBar(
                subtituloUbicacion = cliente?.zona ?: "Pénjamo, Gto.",
                alAccionDerecha = alCambiarRol
            )
        },
        bottomBar = {
            HabilioBarraInferior(
                primerTabSeleccionado = false,
                etiquetaPrimero = "Explorar",
                iconoPrimero = Icons.Default.Search,
                alClickPrimero = alIrAExplorar,
                etiquetaSegundo = "Mi Perfil",
                iconoSegundo = Icons.Outlined.Person,
                alClickSegundo = {}
            )
        },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            HabilioTarjeta {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AvatarUsuario(
                                tamano = 64.dp,
                                esCircular = true,
                                colorIcono = HabilioTheme.TextoPrimario,
                                fotoUrl = cliente?.fotoUrl
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(HabilioTheme.VerdeExito),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = HabilioTheme.TextoSobreAmarillo, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(cliente?.nombre ?: "Cliente Habilio", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            HabilioBadge(
                                texto = "✓ Cuenta Activa",
                                colorTexto = HabilioTheme.VerdeExito,
                                colorFondo = HabilioTheme.FondoVerdeSuave
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Miembro local • ${cliente?.zona ?: "Pénjamo, Gto."}", fontSize = 13.sp, color = HabilioTheme.TextoSecundario)
                        }
                    }

                    OutlinedButton(
                        onClick = alEditarPerfil,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil", tint = HabilioTheme.AmarilloPrimario, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", color = HabilioTheme.AmarilloPrimario, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = HabilioTheme.BordeTarjeta)
                Spacer(modifier = Modifier.height(12.dp))

                Text("📱 +52 ${cliente?.telefono ?: ""}  •  ${cliente?.correo ?: ""}", fontSize = 13.sp, color = HabilioTheme.TextoSecundario)
                Spacer(modifier = Modifier.height(4.dp))
                Text("📍 ${cliente?.direccion ?: "Zona Centro, Pénjamo, Gto."}", fontSize = 13.sp, color = HabilioTheme.TextoSecundario)
            }

            Spacer(modifier = Modifier.height(16.dp))

            HabilioTarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = HabilioTheme.AmarilloPrimario, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actividad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CajaMetricaDetalle("$totalPedidos", "Servicios\nPedidos", HabilioTheme.TextoPrimario, Modifier.weight(1f))
                    CajaMetricaDetalle("$totalCompletados", "Trabajos\nHechos", HabilioTheme.VerdeExito, Modifier.weight(1f))
                    CajaMetricaDetalle("${enCurso.size}", "$${montoAcordado.toInt()} Activos", HabilioTheme.AmarilloPrimario, Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Dirección y Zona", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            HabilioTarjeta {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.FondoAmarilloSuave),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = HabilioTheme.AmarilloPrimario)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Casa Principal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            HabilioBadge("Frecuente", HabilioTheme.AmarilloPrimario, HabilioTheme.FondoAmarilloSuave)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(cliente?.direccion ?: "Calle Hidalgo #114, Zona Centro", fontSize = 14.sp)
                        Text("${cliente?.zona ?: "Pénjamo, Guanajuato"} • CP 36900", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Servicios Contratados", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                HabilioBadge("${solicitudes.size} registros", HabilioTheme.TextoSecundario, HabilioTheme.SuperficieElevada)
            }

            Spacer(modifier = Modifier.height(12.dp))

            EstadoCargaOVacio(
                cargando = false,
                estaVacio = solicitudes.isEmpty(),
                mensajeVacio = "Aún no has solicitado servicios. Explora el catálogo de especialistas para solicitar un presupuesto."
            )

            solicitudes.forEach { sol ->
                val completado = sol.estado == "completado"
                val rechazado = sol.estado == "rechazado"
                val cancelada = sol.estado == "cancelada"
                val esperandoConfirmacion = sol.estado == "esperando_confirmacion"

                HabilioTarjeta(modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AvatarUsuario(tamano = 44.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(sol.nombreTrabajador, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = HabilioTheme.VerdeExito, modifier = Modifier.size(15.dp))
                                }
                                Text(sol.oficioTrabajador, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                            }
                        }

                        val (textoBadge, colorTextoBadge, colorFondoBadge) = when (sol.estado) {
                            "completado" -> Triple("★ Completado", HabilioTheme.VerdeExito, HabilioTheme.FondoVerdeSuave)
                            "esperando_confirmacion" -> Triple("⏳ Por confirmar", HabilioTheme.AmarilloPrimario, HabilioTheme.FondoAmarilloSuave)
                            "en_ejecucion" -> Triple("● En ejecución", HabilioTheme.AmarilloPrimario, HabilioTheme.FondoAmarilloSuave)
                            "cancelada" -> Triple("✕ Cancelada", HabilioTheme.RojoUrgente, HabilioTheme.FondoRojoSuave)
                            "rechazado" -> Triple("✕ Rechazado", HabilioTheme.RojoUrgente, HabilioTheme.FondoRojoSuave)
                            else -> Triple("● Pendiente", HabilioTheme.AzulVerificado, HabilioTheme.FondoAzulSuave)
                        }
                        HabilioBadge(textoBadge, colorTextoBadge, colorFondoBadge)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput, radioEsquina = 10.dp, paddingInterno = 12.dp) {
                        Text(sol.descripcion, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    completado -> "✓ Servicio finalizado"
                                    cancelada -> "✕ Solicitud cancelada por ti"
                                    rechazado -> "✕ Solicitud rechazada"
                                    esperandoConfirmacion -> "⏳ Especialista terminó • Confirma el cierre"
                                    else -> "🤝 Acuerdo directo en curso"
                                },
                                fontSize = 12.sp,
                                color = if (esperandoConfirmacion) HabilioTheme.AmarilloPrimario else HabilioTheme.TextoSecundario
                            )
                            Text(
                                text = "$${String.format("%.2f", sol.monto)} MXN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = when {
                                    completado -> HabilioTheme.VerdeExito
                                    cancelada || rechazado -> HabilioTheme.TextoSecundario
                                    else -> HabilioTheme.AmarilloPrimario
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { alVerSeguimientoSolicitud(sol) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta)
                        ) {
                            Icon(Icons.Outlined.Visibility, contentDescription = null, tint = HabilioTheme.TextoPrimario, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver detalle", color = HabilioTheme.TextoPrimario, fontSize = 13.sp)
                        }

                        if (!completado && !rechazado && !cancelada) {
                            Button(
                                onClick = { alLiberarPago(sol) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HabilioTheme.VerdeExito,
                                    contentColor = HabilioTheme.TextoSobreAmarillo
                                )
                            ) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirmar Cierre", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else if (completado) {
                            val estaEvaluada = sol.fueEvaluada

                            OutlinedButton(
                                onClick = { alCalificarTrabajador(sol) },
                                enabled = !estaEvaluada,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (estaEvaluada) HabilioTheme.BordeTarjeta else HabilioTheme.AmarilloPrimario
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    disabledContentColor = HabilioTheme.TextoSecundario
                                )
                            ) {
                                Text(
                                    text = if (estaEvaluada) "★ Evaluado" else "★ Evaluar",
                                    color = if (estaEvaluada) HabilioTheme.TextoSecundario else HabilioTheme.AmarilloPrimario,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 14. PANTALLA 9: SOLICITUDES DEL TRABAJADOR DINÁMICAS
// ============================================================================
@Composable
fun PantallaSolicitudesTrabajador(
    trabajador: PerfilUsuario?,
    solicitudes: List<SolicitudItem>,
    cargando: Boolean,
    alRefrescar: () -> Unit,
    alCambiarEstadoSolicitud: (solicitud: SolicitudItem, nuevoEstado: String) -> Unit,
    alIrAMiTaller: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var filtroSeleccionado by remember { mutableStateOf("Todas") }
    val pendientesCount = solicitudes.count { it.estado == "pendiente" }
    val enCursoCount = solicitudes.count {
        it.estado == "en_ejecucion" || it.estado == "esperando_confirmacion"
    }

    val solicitudesFiltradas = remember(solicitudes, filtroSeleccionado) {
        when (filtroSeleccionado) {
            "Nuevas" -> solicitudes.filter { it.estado == "pendiente" }
            "En curso" -> solicitudes.filter {
                it.estado == "en_ejecucion" || it.estado == "esperando_confirmacion"
            }
            "Completadas" -> solicitudes.filter { it.estado == "completado" }
            else -> solicitudes
        }
    }

    Scaffold(
        topBar = {
            HabilioTopBar(
                subtituloUbicacion = trabajador?.zona ?: "Pénjamo, Gto.",
                modoTrabajador = true,
                alAccionDerecha = alCambiarRol
            )
        },
        bottomBar = {
            HabilioBarraInferior(
                primerTabSeleccionado = true,
                etiquetaPrimero = "Solicitudes",
                iconoPrimero = Icons.Default.FormatListBulleted,
                alClickPrimero = alRefrescar,
                etiquetaSegundo = "Mi Taller",
                iconoSegundo = Icons.Default.Build,
                alClickSegundo = alIrAMiTaller
            )
        },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Solicitudes de trabajo", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "● $pendientesCount pendientes • $enCursoCount en curso",
                        fontSize = 13.sp,
                        color = HabilioTheme.AmarilloPrimario,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = alRefrescar) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar solicitudes", tint = HabilioTheme.AmarilloPrimario)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val filtros = listOf(
                "Todas" to "Todas ${solicitudes.size}",
                "Nuevas" to "Nuevas $pendientesCount",
                "En curso" to "En curso $enCursoCount",
                "Completadas" to "Completadas"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtros.forEach { (clave, etiqueta) ->
                    val activo = filtroSeleccionado == clave
                    FilterChip(
                        selected = activo,
                        onClick = { filtroSeleccionado = clave },
                        label = { Text(etiqueta, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HabilioTheme.AmarilloPrimario,
                            selectedLabelColor = HabilioTheme.TextoSobreAmarillo,
                            containerColor = HabilioTheme.SuperficieTarjeta,
                            labelColor = HabilioTheme.TextoPrimario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activo,
                            borderColor = HabilioTheme.BordeTarjeta
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EstadoCargaOVacio(
                cargando = cargando,
                estaVacio = solicitudesFiltradas.isEmpty(),
                mensajeVacio = "No tienes solicitudes en esta sección todavía. Cuando un cliente solicite tus servicios aparecerán aquí al instante."
            )

            if (!cargando) {
                solicitudesFiltradas.forEach { sol ->
                    if (sol.estado == "en_ejecucion" ||
                        sol.estado == "esperando_confirmacion" ||
                        sol.estado == "completado"
                    ) {
                        TarjetaSolicitudEnEjecucion(
                            solicitud = sol,
                            alTerminarTrabajo = {
                                // Se solicita confirmacion del cliente antes de pasar a completado
                                alCambiarEstadoSolicitud(sol, "esperando_confirmacion")
                            }
                        )
                    } else {
                        TarjetaSolicitudNueva(
                            solicitud = sol,
                            alAceptar = { alCambiarEstadoSolicitud(sol, "en_ejecucion") },
                            alRechazar = { alCambiarEstadoSolicitud(sol, "rechazado") }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitudNueva(
    solicitud: SolicitudItem,
    alAceptar: () -> Unit,
    alRechazar: () -> Unit
) {
    val esCancelada = solicitud.estado == "cancelada"
    val esRechazada = solicitud.estado == "rechazado"

    HabilioTarjeta {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AvatarUsuario(tamano = 46.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(solicitud.nombreCliente, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("📍 ${solicitud.ubicacion}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                }
            }

            val textoEstado = when {
                esCancelada -> "✕ Cancelada por cliente"
                esRechazada -> "Rechazada"
                else -> "⏱ Nueva"
            }

            HabilioBadge(
                texto = textoEstado,
                colorTexto = if (solicitud.esUrgente || esCancelada || esRechazada) HabilioTheme.RojoUrgente else HabilioTheme.TextoSecundario,
                colorFondo = if (solicitud.esUrgente || esCancelada || esRechazada) HabilioTheme.FondoRojoSuave else HabilioTheme.SuperficieElevada
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HabilioBadge("🛠️ ${solicitud.oficioEtiqueta}", HabilioTheme.TextoPrimario, HabilioTheme.SuperficieElevada)
            HabilioBadge("🤝 Acuerdo directo", HabilioTheme.VerdeExito, HabilioTheme.FondoVerdeSuave)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(solicitud.descripcion, fontSize = 14.sp, lineHeight = 20.sp)

        Spacer(modifier = Modifier.height(12.dp))

        HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput, radioEsquina = 10.dp, paddingInterno = 12.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PRESUPUESTO PROPUESTO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
                    Text(
                        text = "$${String.format("%.2f", solicitud.monto)} MXN",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = HabilioTheme.AmarilloPrimario
                    )
                }
                Text(
                    text = when {
                        esCancelada -> "✕ Cancelada"
                        esRechazada -> "✕ Rechazada"
                        else -> "✓ Solicitud activa"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (esCancelada || esRechazada) HabilioTheme.RojoUrgente else HabilioTheme.VerdeExito
                )
            }
        }

        if (solicitud.estado == "pendiente") {
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = alRechazar,
                    modifier = Modifier
                        .weight(0.4f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = HabilioTheme.SuperficieElevada,
                        contentColor = HabilioTheme.TextoPrimario
                    )
                ) {
                    Text("Rechazar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Button(
                    onClick = alAceptar,
                    modifier = Modifier
                        .weight(0.6f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HabilioTheme.AmarilloPrimario,
                        contentColor = HabilioTheme.TextoSobreAmarillo
                    )
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aceptar solicitud", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitudEnEjecucion(
    solicitud: SolicitudItem,
    alTerminarTrabajo: () -> Unit
) {
    val completada = solicitud.estado == "completado"
    val esperandoConfirmacion = solicitud.estado == "esperando_confirmacion"

    HabilioTarjeta(
        colorBorde = if (esperandoConfirmacion) HabilioTheme.AmarilloPrimario else HabilioTheme.VerdeExito,
        grosorBorde = 1.5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AvatarUsuario(
                    tamano = 44.dp,
                    icono = Icons.Outlined.WorkOutline,
                    colorIcono = if (esperandoConfirmacion) HabilioTheme.AmarilloPrimario else HabilioTheme.VerdeExito,
                    colorFondo = if (esperandoConfirmacion) HabilioTheme.FondoAmarilloSuave else HabilioTheme.FondoVerdeSuave
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(solicitud.nombreCliente, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("📍 ${solicitud.ubicacion}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                }
            }

            HabilioBadge(
                texto = when {
                    completada -> "✓ Completado"
                    esperandoConfirmacion -> "⏳ Esperando confirmación"
                    else -> "● En Ejecución"
                },
                colorTexto = if (esperandoConfirmacion) HabilioTheme.AmarilloPrimario else HabilioTheme.VerdeExito,
                colorFondo = if (esperandoConfirmacion) HabilioTheme.FondoAmarilloSuave else HabilioTheme.FondoVerdeSuave
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(solicitud.descripcion, fontSize = 14.sp, lineHeight = 20.sp)

        Spacer(modifier = Modifier.height(12.dp))

        HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput, radioEsquina = 10.dp, paddingInterno = 12.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Presupuesto acordado", fontSize = 11.sp, color = HabilioTheme.TextoSecundario)
                    Text(
                        text = "$${String.format("%.2f", solicitud.monto)} MXN",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (esperandoConfirmacion) HabilioTheme.AmarilloPrimario else HabilioTheme.VerdeExito
                    )
                }
                Text(
                    text = when {
                        completada -> "✓ Trabajo concluido"
                        esperandoConfirmacion -> "⏳ Por confirmar cliente"
                        else -> "🛠️ En proceso"
                    },
                    fontSize = 12.sp,
                    color = if (esperandoConfirmacion) HabilioTheme.AmarilloPrimario else HabilioTheme.TextoPrimario
                )
            }
        }

        if (!completada) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = alTerminarTrabajo,
                enabled = !esperandoConfirmacion,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabilioTheme.VerdeExito,
                    contentColor = HabilioTheme.TextoSobreAmarillo,
                    disabledContainerColor = HabilioTheme.FondoAmarilloSuave,
                    disabledContentColor = HabilioTheme.AmarilloPrimario
                )
            ) {
                Icon(
                    imageVector = if (esperandoConfirmacion) Icons.Default.HourglassEmpty else Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (esperandoConfirmacion) {
                        "Esperando confirmación del cliente"
                    } else {
                        "Marcar Trabajo como Terminado"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ============================================================================
// 15. PANTALLA 10: MI TALLER DEL TRABAJADOR DINÁMICO (Con botón Editar Perfil)
// ============================================================================
@Composable
fun PantallaTallerTrabajador(
    trabajador: PerfilUsuario?,
    evidencias: List<EvidenciaItem>,
    solicitudes: List<SolicitudItem>,
    alEditarPerfil: () -> Unit,
    alPublicarEvidencia: (String, Uri?) -> Unit,
    alClickFotoEvidencia: (EvidenciaItem) -> Unit,
    alIrASolicitudes: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var tituloNuevaEvidencia by remember { mutableStateOf("") }
    var imagenSeleccionada by remember { mutableStateOf<Uri?>(null) }

    val ultimoTrabajoConcluido = solicitudes.firstOrNull { it.estado == "completado" }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imagenSeleccionada = uri
    }

    Scaffold(
        topBar = {
            HabilioTopBar(
                subtituloUbicacion = trabajador?.zona ?: "Pénjamo, Gto.",
                modoTrabajador = true,
                alAccionDerecha = alCambiarRol
            )
        },
        bottomBar = {
            HabilioBarraInferior(
                primerTabSeleccionado = false,
                etiquetaPrimero = "Solicitudes",
                iconoPrimero = Icons.Default.FormatListBulleted,
                alClickPrimero = alIrASolicitudes,
                etiquetaSegundo = "Mi Taller",
                iconoSegundo = Icons.Default.Build,
                alClickSegundo = {}
            )
        },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (ultimoTrabajoConcluido != null) {
                HabilioTarjeta(
                    colorFondo = HabilioTheme.FondoVerdeSuave,
                    colorBorde = HabilioTheme.VerdeExito.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HabilioBadge("SERVICIO CONCLUIDO", HabilioTheme.VerdeExito, Color(0xFF0B1C16))
                        Text("Completado", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${ultimoTrabajoConcluido.descripcion} • Cliente ${ultimoTrabajoConcluido.nombreCliente}",
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format("%.2f", ultimoTrabajoConcluido.monto)} MXN (Acuerdo directo)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HabilioTheme.VerdeExito
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            HabilioTarjeta {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        AvatarUsuario(
                            tamano = 56.dp,
                            icono = Icons.Default.Person,
                            colorIcono = HabilioTheme.TextoPrimario,
                            fotoUrl = trabajador?.fotoUrl
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(trabajador?.nombre ?: "Especialista Habilio", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = trabajador?.oficio ?: "Oficio",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HabilioTheme.AmarilloPrimario
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = alEditarPerfil,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil", tint = HabilioTheme.AmarilloPrimario, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", color = HabilioTheme.AmarilloPrimario, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput, radioEsquina = 8.dp, paddingInterno = 10.dp) {
                    Text(
                        text = "Especialista en ${trabajador?.oficio ?: "Oficio"} • ${trabajador?.modalidad ?: "Trabajo a domicilio"}" +
                                if (!trabajador?.especialidad.isNullOrBlank()) "\n${trabajador?.especialidad}" else "",
                        fontSize = 13.sp,
                        color = HabilioTheme.TextoSecundario
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HabilioTarjeta(colorBorde = HabilioTheme.AmarilloPrimario.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subir foto de trabajo real", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("+ Evidencia", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HabilioTheme.AmarilloPrimario)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Toma o sube una foto de un trabajo terminado para demostrar tu experiencia a nuevos clientes:",
                    fontSize = 13.sp,
                    color = HabilioTheme.TextoSecundario,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tituloNuevaEvidencia,
                    onValueChange = { tituloNuevaEvidencia = it },
                    placeholder = {
                        Text("Título (ej. Impermeabilización de azotea)", color = HabilioTheme.TextoPlaceholder, fontSize = 13.sp)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = coloresInputHabilio()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (imagenSeleccionada != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.SuperficieElevada)
                            .clickable { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, tint = HabilioTheme.VerdeExito, contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Fotografía lista y comprimida (Toca para cambiar)", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario)
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = HabilioTheme.AmarilloPrimario, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir fotografía", color = HabilioTheme.AmarilloPrimario, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (tituloNuevaEvidencia.isNotBlank() && imagenSeleccionada != null) {
                            alPublicarEvidencia(tituloNuevaEvidencia, imagenSeleccionada)
                            tituloNuevaEvidencia = ""
                            imagenSeleccionada = null
                        }
                    },
                    enabled = tituloNuevaEvidencia.isNotBlank() && imagenSeleccionada != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HabilioTheme.AmarilloPrimario,
                        contentColor = HabilioTheme.TextoSobreAmarillo
                    )
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subir evidencia", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Mis evidencias publicadas (${evidencias.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            EstadoCargaOVacio(
                cargando = false,
                estaVacio = evidencias.isEmpty(),
                mensajeVacio = "Aún no tienes evidencias publicadas. Escribe un título arriba y presiona 'Subir Evidencia'."
            )

            evidencias.chunked(2).forEach { fila ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fila.forEach { ev ->
                        val tieneFoto = !ev.fotoUrl.isNullOrBlank()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HabilioTheme.SuperficieTarjeta,
                            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = tieneFoto) { alClickFotoEvidencia(ev) }
                        ) {
                            Column {
                                if (tieneFoto) {
                                    AsyncImage(
                                        model = ev.fotoUrl,
                                        contentDescription = ev.titulo,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .background(HabilioTheme.SuperficieElevada)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .background(HabilioTheme.SuperficieElevada),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Image, contentDescription = null, tint = HabilioTheme.AmarilloPrimario, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = ev.titulo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("✓ Publicada", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HabilioTheme.VerdeExito)
                                }
                            }
                        }
                    }
                    if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ============================================================================
// 16. PANTALLA: SEGUIMIENTO DE SOLICITUD (Estilo Rastreo de Pedido Amazon)
// ============================================================================
@Composable
fun PantallaSeguimientoSolicitud(
    solicitud: SolicitudItem,
    especialista: PerfilUsuario?,
    alVolver: () -> Unit,
    alRefrescar: () -> Unit,
    alCancelarPedido: () -> Unit,
    alConfirmarCierre: () -> Unit,
    alCalificar: () -> Unit,
    alVerPerfilTrabajador: () -> Unit
) {
    val folioCorto = remember(solicitud.id) {
        val digitos = solicitud.id.filter { it.isDigit() }.take(4).padEnd(4, '8')
        "#HAB-$digitos"
    }

    val esRechazado = solicitud.estado == "rechazado"
    val esCancelada = solicitud.estado == "cancelada"
    val esperandoConfirmacion = solicitud.estado == "esperando_confirmacion"

    val pasoActual = when (solicitud.estado) {
        "pendiente" -> 1
        "en_ejecucion", "esperando_confirmacion" -> 2
        "completado" -> 3
        else -> 0
    }

    Scaffold(
        topBar = {
            HabilioTopBar(
                subtituloUbicacion = "Seguimiento de servicio",
                alAccionDerecha = alRefrescar,
                iconoDerecha = Icons.Default.Refresh
            )
        },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = alVolver, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = HabilioTheme.TextoPrimario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Volver a Mi Perfil", color = HabilioTheme.TextoPrimario, fontSize = 14.sp)
                }

                HabilioBadge(
                    texto = "Folio $folioCorto",
                    colorTexto = HabilioTheme.AmarilloPrimario,
                    colorFondo = HabilioTheme.FondoAmarilloSuave
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HabilioTarjeta(
                colorBorde = when {
                    esRechazado || esCancelada -> HabilioTheme.RojoUrgente
                    pasoActual == 3 -> HabilioTheme.VerdeExito
                    else -> HabilioTheme.AmarilloPrimario.copy(alpha = 0.6f)
                },
                grosorBorde = 1.5.dp
            ) {
                val tituloEstado = when {
                    esCancelada -> "Solicitud cancelada"
                    esRechazado -> "Solicitud rechazada por el especialista"
                    pasoActual == 1 -> "Esperando confirmación del especialista"
                    esperandoConfirmacion -> "Especialista finalizó • Esperando tu confirmación"
                    pasoActual == 2 -> "Trabajo en ejecución"
                    else -> "¡Servicio concluido con éxito!"
                }
                val subtituloEstado = when {
                    esCancelada -> "Cancelaste esta solicitud. El registro se conserva en tu historial de servicios."
                    esRechazado -> "El trabajador no pudo atender esta solicitud en este momento. Puedes buscar otro especialista disponible."
                    pasoActual == 1 -> "Tu solicitud ya fue enviada a ${solicitud.nombreTrabajador}. Te notificaremos en cuanto la acepte."
                    esperandoConfirmacion -> "${solicitud.nombreTrabajador} marcó el trabajo como terminado. Revisa el resultado y presiona el botón verde para confirmar."
                    pasoActual == 2 -> "${solicitud.nombreTrabajador} aceptó tu solicitud y el trabajo está en proceso."
                    else -> "El servicio ha sido confirmado como finalizado por ambas partes. Ya puedes dejar tu reseña y calificación."
                }

                Text(
                    text = tituloEstado,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        esRechazado || esCancelada -> HabilioTheme.RojoUrgente
                        pasoActual == 3 -> HabilioTheme.VerdeExito
                        else -> HabilioTheme.AmarilloPrimario
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtituloEstado,
                    fontSize = 13.sp,
                    color = HabilioTheme.TextoSecundario,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                when {
                    esCancelada -> {
                        PasoRastreoPedido(
                            titulo = "Solicitud enviada",
                            subtitulo = "Registrada con folio $folioCorto",
                            completado = true,
                            activo = false,
                            mostrarLineaInferior = true
                        )
                        PasoRastreoPedido(
                            titulo = "Pedido cancelado por el cliente",
                            subtitulo = "La solicitud fue cancelada y archivada en tu historial",
                            completado = false,
                            activo = true,
                            esError = true,
                            mostrarLineaInferior = false
                        )
                    }
                    esRechazado -> {
                        PasoRastreoPedido(
                            titulo = "Solicitud enviada",
                            subtitulo = "Registrada con folio $folioCorto",
                            completado = true,
                            activo = false,
                            mostrarLineaInferior = true
                        )
                        PasoRastreoPedido(
                            titulo = "Solicitud rechazada",
                            subtitulo = "El especialista no tenía disponibilidad para este trabajo",
                            completado = false,
                            activo = true,
                            esError = true,
                            mostrarLineaInferior = false
                        )
                    }
                    else -> {
                        PasoRastreoPedido(
                            titulo = "1. Solicitud enviada",
                            subtitulo = "Enviada a ${solicitud.nombreTrabajador} con presupuesto propuesto",
                            completado = pasoActual >= 1,
                            activo = pasoActual == 1,
                            mostrarLineaInferior = true
                        )
                        PasoRastreoPedido(
                            titulo = if (esperandoConfirmacion) "2. Trabajo terminado por especialista" else "2. Solicitud aceptada y en ejecución",
                            subtitulo = when {
                                esperandoConfirmacion -> "El especialista reportó haber concluido • Pendiente de tu confirmación"
                                pasoActual >= 2 -> "El especialista confirmó y realiza el servicio"
                                else -> "Pendiente de aceptación del especialista"
                            },
                            completado = pasoActual >= 2,
                            activo = pasoActual == 2,
                            mostrarLineaInferior = true
                        )
                        PasoRastreoPedido(
                            titulo = "3. Trabajo verificado y concluido",
                            subtitulo = when {
                                pasoActual >= 3 -> "Servicio finalizado y confirmado satisfactoriamente"
                                esperandoConfirmacion -> "Presiona 'Confirmar que el servicio fue completado' abajo"
                                else -> "Se completará cuando ambos confirmen el cierre del trabajo"
                            },
                            completado = pasoActual >= 3,
                            activo = pasoActual == 3 || esperandoConfirmacion,
                            mostrarLineaInferior = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detalles del servicio
            HabilioTarjeta {
                Text(
                    text = "📦 DETALLES DE LA SOLICITUD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HabilioTheme.AmarilloPrimario
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        AvatarUsuario(
                            tamano = 48.dp,
                            fotoUrl = especialista?.fotoUrl
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(solicitud.nombreTrabajador, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(solicitud.oficioTrabajador, fontSize = 13.sp, color = HabilioTheme.AmarilloPrimario)
                            if (!especialista?.telefono.isNullOrBlank()) {
                                Text("📱 +52 ${especialista?.telefono}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                            }
                        }
                    }

                    TextButton(onClick = alVerPerfilTrabajador) {
                        Text("Ver perfil >", color = HabilioTheme.AzulVerificado, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = HabilioTheme.BordeTarjeta)
                Spacer(modifier = Modifier.height(14.dp))

                Text("Trabajo solicitado:", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                Spacer(modifier = Modifier.height(4.dp))
                Text(solicitud.descripcion, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Ubicación de atención:", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                Spacer(modifier = Modifier.height(4.dp))
                Text("📍 ${solicitud.ubicacion}", fontSize = 14.sp)

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = HabilioTheme.BordeTarjeta)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PRESUPUESTO ACORDADO", fontSize = 11.sp, color = HabilioTheme.TextoSecundario, fontWeight = FontWeight.Bold)
                        Text("Acuerdo directo con el especialista", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                    Text(
                        text = "$${String.format("%.2f", solicitud.monto)} MXN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HabilioTheme.AmarilloPrimario
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!esRechazado && !esCancelada && pasoActual < 3) {
                if (pasoActual == 2) {
                    Button(
                        onClick = alConfirmarCierre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HabilioTheme.VerdeExito,
                            contentColor = HabilioTheme.TextoSobreAmarillo
                        )
                    ) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar que el servicio fue completado", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = alCancelarPedido,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HabilioTheme.RojoUrgente,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancelar pedido", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            } else if (pasoActual == 3) {
                val yaEvaluada = solicitud.fueEvaluada
                BotonPrimarioHabilio(
                    texto = if (yaEvaluada) "★ Servicio Evaluado" else "★ Calificar y Recomendar al Especialista",
                    habilitado = !yaEvaluada,
                    alClick = alCalificar
                )
            }
        }
    }
}

@Composable
private fun PasoRastreoPedido(
    titulo: String,
    subtitulo: String,
    completado: Boolean,
    activo: Boolean,
    esError: Boolean = false,
    mostrarLineaInferior: Boolean
) {
    val colorCirculo = when {
        esError -> HabilioTheme.RojoUrgente
        completado -> HabilioTheme.VerdeExito
        activo -> HabilioTheme.AmarilloPrimario
        else -> HabilioTheme.BordeTarjeta
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colorCirculo),
                contentAlignment = Alignment.Center
            ) {
                when {
                    esError -> Icon(Icons.Default.Close, contentDescription = null, tint = HabilioTheme.TextoPrimario, modifier = Modifier.size(16.dp))
                    completado -> Icon(Icons.Default.Check, contentDescription = null, tint = HabilioTheme.TextoSobreAmarillo, modifier = Modifier.size(16.dp))
                    else -> Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(HabilioTheme.FondoPrincipal))
                }
            }
            if (mostrarLineaInferior) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(42.dp)
                        .background(if (completado) HabilioTheme.VerdeExito else HabilioTheme.BordeTarjeta)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.padding(bottom = if (mostrarLineaInferior) 18.dp else 0.dp)) {
            Text(
                text = titulo,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (completado || activo) HabilioTheme.TextoPrimario else HabilioTheme.TextoSecundario
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitulo,
                fontSize = 12.sp,
                color = HabilioTheme.TextoSecundario,
                lineHeight = 17.sp
            )
        }
    }
}