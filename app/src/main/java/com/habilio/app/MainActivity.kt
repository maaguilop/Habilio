package com.habilio.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

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
    val nombreCliente: String = "Cliente Habilio",
    val nombreTrabajador: String = "Especialista Habilio",
    val oficioTrabajador: String = "Oficio Verificado"
)

object SupabaseService {
    private const val PROJECT_URL = "https://zuqiuzdkobbomtoonace.supabase.co"
    private const val REST_URL = "$PROJECT_URL/rest/v1"
    private const val AUTH_URL = "$PROJECT_URL/auth/v1"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp1cWl1emRrb2Jib210b29uYWNlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTM1ODQ0NDEsImV4cCI6MjEwOTE2MDQ0MX0.0hCBoRTfYOYJmtUX6mgmGXm0eQTf1pbfk6anIGUONPg"

    suspend fun subirFotoStorage(context: android.content.Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bytes = inputStream.use { it.readBytes() }

            val nombreArchivo = "${UUID.randomUUID()}.jpg"
            val urlStorage = "$PROJECT_URL/storage/v1/object/evidencias/$nombreArchivo"

            val conn = (URL(urlStorage).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 20000
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                setRequestProperty("Content-Type", "image/jpeg")
                doOutput = true
            }

            conn.outputStream.use { it.write(bytes) }

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
        val conn = (URL(urlCompleta).openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("apikey", ANON_KEY)
            setRequestProperty("Authorization", "Bearer $ANON_KEY")
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
        esPremium = obj.optBoolean("es_premium", true),
        calificacion = obj.optDouble("calificacion", 5.0).let { if (it.isNaN()) 5.0 else it },
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
        modalidad: String = ""
    ): Result<PerfilUsuario> = withContext(Dispatchers.IO) {
        try {
            val correoFinal = normalizarCorreo(correoInput.ifBlank { telefono }, telefono)
            val authPayload = JSONObject().apply {
                put("email", correoFinal)
                put("password", contrasena)
            }
            val (authCode, authResp) = peticionHttp("$AUTH_URL/signup", "POST", authPayload.toString(), false)

            var userId = ""
            if (authCode in 200..299 && authResp.isNotBlank()) {
                val jsonAuth = JSONObject(authResp)
                userId = jsonAuth.optJSONObject("user")?.optString("id").orEmpty()
                    .ifBlank { jsonAuth.optString("id", "") }
            } else {
                // Si el correo ya estaba registrado en Auth, verificamos con su contraseña exacta
                val (loginCode, loginResp) = peticionHttp(
                    "$AUTH_URL/token?grant_type=password",
                    "POST",
                    authPayload.toString(),
                    false
                )
                if (loginCode in 200..299 && loginResp.isNotBlank()) {
                    userId = JSONObject(loginResp).optJSONObject("user")?.optString("id").orEmpty()
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
                "Especialista verificado en $oficio.$detalleEsp Atención $modalidad en $zona."
            } else ""

            val perfilPayload = JSONObject().apply {
                put("id", userId)
                put("rol", rol)
                put("nombre", nombre.trim())
                put("telefono", telefono.trim())
                put("correo", correoFinal)
                put("zona", zona.trim().ifBlank { "Pénjamo, Gto." })
                put("direccion", direccion.trim().ifBlank { "Zona Centro, Pénjamo, Gto." })
                if (rol == "trabajador") {
                    put("nombre_taller", nombreTaller.trim())
                    put("oficio", oficio)
                    put("especialidad", especialidad.trim())
                    put("modalidad", modalidad)
                    put("descripcion", descripcionGenerada)
                    put("es_premium", true)
                    put("calificacion", 5.0)
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
     * INICIO DE SESIÓN ESTRICTO:
     * Valida obligatoriamente la contraseña contra Supabase Auth (/auth/v1/token).
     * Si la contraseña es incorrecta, rechaza el acceso de inmediato.
     */
    suspend fun iniciarSesion(identificador: String, contrasena: String): Result<PerfilUsuario> =
        withContext(Dispatchers.IO) {
            try {
                val limpio = identificador.trim()
                var correoParaAuth = normalizarCorreo(limpio)
                var perfilEncontrado: PerfilUsuario? = null

                // 1. Buscar el perfil por correo o teléfono para conocer su email de Auth asociado
                val filtro = if (limpio.contains("@")) {
                    "correo=eq.${URLEncoder.encode(limpio.lowercase(), "UTF-8")}"
                } else {
                    "telefono=eq.${URLEncoder.encode(limpio, "UTF-8")}"
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

                // 2. Autenticar estrictamente con contraseña en Supabase Auth
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

                // Si ingresó teléfono y su cuenta Auth fue creada con @habilio.app, probar también el correo sintético
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

                // 3. Si Supabase Auth no devuelve 200 OK, la contraseña es INCORRECTA -> Denegar acceso
                if (authCode !in 200..299 || authResp.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Correo/teléfono o contraseña incorrectos. Verifica tus datos.")
                    )
                }

                // 4. Contraseña válida: recuperar el perfil verificado del usuario
                val uid = JSONObject(authResp).optJSONObject("user")?.optString("id").orEmpty()
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
                put("fecha_publicacion", "Verificado • $ubicacion")
                if (!fotoUrl.isNullOrBlank()) put("foto_url", fotoUrl)
            }
            peticionHttp("$REST_URL/evidencias", "POST", payload.toString()).first in 200..299
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
                        fechaTexto = o.optString("fecha_texto", "Reciente • Servicio verificado")
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
                put("fecha_texto", "Hace un momento • Servicio verificado")
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
                        SolicitudItem(
                            id = o.optString("id"),
                            clienteId = cId,
                            trabajadorId = tId,
                            oficioEtiqueta = o.optString("oficio_etiqueta", "Servicio General"),
                            descripcion = o.optString("descripcion", ""),
                            ubicacion = o.optString("ubicacion", "Pénjamo, Gto."),
                            monto = o.optDouble("monto", 0.0),
                            esUrgente = o.optBoolean("es_urgente", false),
                            estado = o.optString("estado", "pendiente"),
                            nombreCliente = mapaNombres[cId]?.first ?: "Cliente Local",
                            nombreTrabajador = mapaNombres[tId]?.first ?: "Especialista",
                            oficioTrabajador = mapaNombres[tId]?.second ?: o.optString("oficio_etiqueta")
                        )
                    }
                }
            } catch (_: Exception) {}
            emptyList()
        }

    suspend fun actualizarEstadoSolicitud(solicitudId: String, nuevoEstado: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply { put("estado", nuevoEstado) }
                peticionHttp("$REST_URL/solicitudes?id=eq.$solicitudId", "PATCH", payload.toString()).first in 200..299
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
    TALLER_TRABAJADOR
}

@Composable
fun HabilioApp() {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var pantallaActual by remember { mutableStateOf(RutaHabilio.INICIO_ROL) }

    var usuarioActual by remember { mutableStateOf<PerfilUsuario?>(null) }
    var listaTrabajadores by remember { mutableStateOf<List<PerfilUsuario>>(emptyList()) }
    var trabajadorSeleccionado by remember { mutableStateOf<PerfilUsuario?>(null) }
    var todasLasEvidencias by remember { mutableStateOf<List<EvidenciaItem>>(emptyList()) }
    var todasLasResenas by remember { mutableStateOf<List<ResenaItem>>(emptyList()) }
    var solicitudesUsuario by remember { mutableStateOf<List<SolicitudItem>>(emptyList()) }
    var cargandoDatos by remember { mutableStateOf(false) }

    var mostrarModalCompra by remember { mutableStateOf(false) }
    var solicitudRecienConfirmada by remember { mutableStateOf<SolicitudItem?>(null) }
    var mostrarModalCalificar by remember { mutableStateOf(false) }

    // Estado para abrir cualquier fotografía de evidencia en pantalla completa
    var evidenciaEnPantallaCompleta by remember { mutableStateOf<EvidenciaItem?>(null) }

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
            }
            cargandoDatos = false
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
                    alCambiarRol = {
                        usuarioActual = null
                        pantallaActual = RutaHabilio.INICIO_ROL
                    }
                )

                RutaHabilio.DETALLE_ESPECIALISTA -> {
                    val esp = trabajadorSeleccionado
                    if (esp != null) {
                        PantallaDetalleEspecialista(
                            especialista = esp,
                            evidencias = todasLasEvidencias.filter { it.trabajadorId == esp.id },
                            resenas = todasLasResenas.filter { it.trabajadorId == esp.id },
                            totalServicios = solicitudesUsuario.count { it.trabajadorId == esp.id && it.estado == "completado" },
                            alVolverAlListado = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                            alAbrirModalSolicitar = { mostrarModalCompra = true },
                            alAbrirModalCalificar = { mostrarModalCalificar = true },
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
                    alVerDetalleTrabajador = { trabajadorId ->
                        listaTrabajadores.find { it.id == trabajadorId }?.let {
                            trabajadorSeleccionado = it
                            pantallaActual = RutaHabilio.DETALLE_ESPECIALISTA
                        }
                    },
                    alLiberarPago = { solicitudId ->
                        scope.launch {
                            SupabaseService.actualizarEstadoSolicitud(solicitudId, "completado")
                            recargarDatosGlobales()
                        }
                    },
                    alCalificarTrabajador = { trabajadorId ->
                        listaTrabajadores.find { it.id == trabajadorId }?.let {
                            trabajadorSeleccionado = it
                            mostrarModalCalificar = true
                        }
                    },
                    alIrAExplorar = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                    alCambiarRol = {
                        usuarioActual = null
                        pantallaActual = RutaHabilio.INICIO_ROL
                    }
                )

                RutaHabilio.SOLICITUDES_TRABAJADOR -> PantallaSolicitudesTrabajador(
                    trabajador = usuarioActual,
                    solicitudes = solicitudesUsuario,
                    cargando = cargandoDatos,
                    alRefrescar = { recargarDatosGlobales() },
                    alCambiarEstadoSolicitud = { solicitudId, nuevoEstado ->
                        scope.launch {
                            SupabaseService.actualizarEstadoSolicitud(solicitudId, nuevoEstado)
                            recargarDatosGlobales()
                        }
                    },
                    alIrAMiTaller = {
                        recargarDatosGlobales()
                        pantallaActual = RutaHabilio.TALLER_TRABAJADOR
                    },
                    alCambiarRol = {
                        usuarioActual = null
                        pantallaActual = RutaHabilio.INICIO_ROL
                    }
                )

                RutaHabilio.TALLER_TRABAJADOR -> PantallaTallerTrabajador(
                    trabajador = usuarioActual,
                    evidencias = todasLasEvidencias.filter { it.trabajadorId == usuarioActual?.id },
                    solicitudes = solicitudesUsuario,
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
                    alCambiarRol = {
                        usuarioActual = null
                        pantallaActual = RutaHabilio.INICIO_ROL
                    }
                )
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
                        solicitudRecienConfirmada = null
                        recargarDatosGlobales()
                        pantallaActual = RutaHabilio.PERFIL_CLIENTE
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
                        ?: "Familia Ramírez (Col. Centro)",
                    alCerrar = { mostrarModalCalificar = false },
                    alPublicarResena = { estrellas, autor, comentario ->
                        val trab = trabajadorSeleccionado ?: return@ModalEvaluarServicio
                        scope.launch {
                            SupabaseService.publicarResena(trab.id, autor, comentario, estrellas)
                            mostrarModalCalificar = false
                            recargarDatosGlobales()
                        }
                    }
                )
            }

            // Visor de foto a pantalla completa cuando el usuario o trabajador toca una evidencia
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

/**
 * Visor modal para abrir cualquier fotografía de evidencia en tamaño completo.
 */
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
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = "Logo de Habilio",
                            tint = HabilioTheme.AmarilloPrimario,
                            modifier = Modifier.size(24.dp)
                        )
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
                                    texto = "● Modo Trabajador",
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
    colorFondo: Color = HabilioTheme.SuperficieElevada
) {
    val shape = if (esCircular) CircleShape else RoundedCornerShape(12.dp)
    Box(contentAlignment = Alignment.BottomEnd) {
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
                    contentDescription = "Volver a selección de rol",
                    tint = HabilioTheme.TextoSecundario,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cambiar rol", color = HabilioTheme.TextoSecundario, fontSize = 13.sp)
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
            Icon(
                imageVector = Icons.Default.Handyman,
                contentDescription = "Logo Habilio",
                tint = HabilioTheme.AmarilloPrimario,
                modifier = Modifier.size(36.dp)
            )
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
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Presupuesto estimado", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
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
                    text = "¡Solicitud Confirmada!",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HabilioTheme.TextoPrimario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tu solicitud ha sido enviada a ${solicitud.nombreTrabajador}. Tienes tu presupuesto protegido con la Garantía Habilio.",
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
                            Text("PROTEGIDO", fontSize = 11.sp, color = HabilioTheme.TextoSecundario, fontWeight = FontWeight.Bold)
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
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = HabilioTheme.VerdeExito,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "El pago se libera al verificar el trabajo",
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                BotonPrimarioHabilio(
                    texto = "Aceptar y Ver Estatus",
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

            Text("Recomendación o reporte del servicio:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                    Icon(
                        imageVector = Icons.Default.Handyman,
                        contentDescription = null,
                        tint = HabilioTheme.AmarilloPrimario,
                        modifier = Modifier.size(44.dp)
                    )
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
                descripcion = "Buscar trabajadores por oficio, revisar fotos de sus trabajos y contratar con seguridad.",
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
            subtitulo = "Encuentra y contrata a los mejores maestros de oficios certificados con evidencia real de trabajo.",
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
                etiquetaDerecha = "WhatsApp / SMS directo",
                valor = telefonoOCorreo,
                alCambiarValor = { telefonoOCorreo = it },
                placeholder = "469 123 4567 o tu@correo.com",
                prefijoTexto = "+52"
            )

            Spacer(modifier = Modifier.height(16.dp))

            HabilioCampoTexto(
                etiqueta = "Contraseña",
                etiquetaDerecha = "¿La olvidaste?",
                colorEtiquetaDerecha = HabilioTheme.AmarilloPrimario,
                accionEtiquetaDerecha = {},
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
                text = "Si eres albañil, plomero, electricista o carpintero en Guanajuato:",
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
    val scope = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
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
            subtitulo = "Encuentra y contrata a los mejores maestros de oficios certificados con evidencia real de trabajo.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = false,
            alSeleccionarLogin = alIrALogin,
            alSeleccionarRegistro = {}
        )

        Spacer(modifier = Modifier.height(20.dp))

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
                etiqueta = "Teléfono móvil (WhatsApp)",
                etiquetaDerecha = "Para confirmaciones",
                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                valor = telefono,
                alCambiarValor = { telefono = it },
                placeholder = "469 123 4567",
                prefijoTexto = "+52"
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Correo electrónico",
                etiquetaDerecha = "Opcional",
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

            Spacer(modifier = Modifier.height(22.dp))

            BotonPrimarioHabilio(
                texto = if (cargando) "Creando cuenta..." else "Crear Cuenta de Cliente",
                habilitado = !cargando && nombre.isNotBlank() && telefono.isNotBlank() && contrasena.length >= 6,
                alClick = {
                    cargando = true
                    mensajeError = null
                    scope.launch {
                        val res = SupabaseService.registrarUsuario(
                            rol = "cliente",
                            nombre = nombre,
                            telefono = telefono,
                            correoInput = correo,
                            contrasena = contrasena
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
    val scope = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }
    var nombreTaller by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var oficioSeleccionado by remember { mutableStateOf("Albañilería") }
    var especialidad by remember { mutableStateOf("") }
    var zonaServicio by remember { mutableStateOf("Pénjamo Centro, Santa Ana Pacueco y alrededores") }
    var modalidadSeleccionada by remember { mutableStateOf(0) }
    var direccionTaller by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val oficios = listOf(
        Triple("Albañilería", "Obra y acabados", Icons.Default.Construction),
        Triple("Plomería", "Fugas y tubos", Icons.Default.Plumbing),
        Triple("Electricidad", "110v / 220v", Icons.Default.Bolt),
        Triple("Carpintería", "Madera y clósets", Icons.Default.Carpenter),
        Triple("Herrería", "Puertas y rejas", Icons.Default.Hardware),
        Triple("Pintura", "Interiores y fachadas", Icons.Default.FormatPaint)
    )

    val modalidades = listOf(
        "🚗 A Domicilio" to "Me traslado a la casa o negocio del cliente en la región.",
        "🏠 En mi Taller o Local" to "Tengo ubicación física.",
        "🔄 Ambos (Taller y a domicilio)" to "Flexibilidad total según lo que el cliente requiera."
    )

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
            subtitulo = "Publica tus trabajos con evidencia real, recibe solicitudes directas y haz crecer tu taller sin comisiones abusivas.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(18.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = false,
            alSeleccionarLogin = alIrALogin,
            alSeleccionarRegistro = {}
        )

        Spacer(modifier = Modifier.height(18.dp))

        HabilioTarjeta(paddingInterno = 18.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AvatarUsuario(
                        tamano = 72.dp,
                        esCircular = true,
                        icono = Icons.Default.Handyman,
                        colorIcono = HabilioTheme.AmarilloPrimario
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(HabilioTheme.AmarilloPrimario),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Subir foto",
                            tint = HabilioTheme.TextoSobreAmarillo,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Foto de Perfil Profesional", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sube una imagen clara de tu rostro, uniforme o taller. Un perfil con foto recibe 4x más solicitudes directas.",
                    fontSize = 13.sp,
                    color = HabilioTheme.TextoSecundario,
                    textAlign = TextAlign.Center
                )
            }
        }

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
            etiqueta = "Teléfono Celular",
            valor = telefono,
            alCambiarValor = { telefono = it },
            placeholder = "469 123 4567",
            prefijoTexto = "+52"
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

        oficios.chunked(2).forEach { fila ->
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
            placeholder = "Pénjamo Centro y alrededores",
            iconoInicial = Icons.Outlined.Explore
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text("¿Cómo atiendes a tus clientes?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        modalidades.forEachIndexed { indice, (tituloMod, descMod) ->
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

        Spacer(modifier = Modifier.height(24.dp))

        BotonPrimarioHabilio(
            texto = if (cargando) "Registrando trabajador..." else "Registrarme como trabajador",
            habilitado = !cargando && nombre.isNotBlank() && telefono.isNotBlank() && contrasena.length >= 6,
            alClick = {
                cargando = true
                mensajeError = null
                scope.launch {
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
                        modalidad = modalidades[modalidadSeleccionada].first
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
// 10. PANTALLA 6: BUSCADOR DE ESPECIALISTAS DINÁMICO (Con fotos reales y zoom)
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
    val categorias = listOf("Todos", "Albañilería", "Plomería", "Electricidad", "Carpintería", "Herrería", "Pintura")

    val trabajadoresFiltrados = remember(trabajadores, busqueda, categoriaActiva) {
        trabajadores.filter { t ->
            val coincideCat = categoriaActiva == "Todos" ||
                    t.oficio.contains(categoriaActiva, ignoreCase = true) ||
                    t.especialidad.contains(categoriaActiva, ignoreCase = true)
            val coincideTexto = busqueda.isBlank() ||
                    t.nombre.contains(busqueda, ignoreCase = true) ||
                    t.oficio.contains(busqueda, ignoreCase = true) ||
                    t.especialidad.contains(busqueda, ignoreCase = true) ||
                    t.descripcion.contains(busqueda, ignoreCase = true)
            coincideCat && coincideTexto
        }
    }

    Scaffold(
        topBar = { HabilioTopBar(subtituloUbicacion = "Pénjamo, Gto.", alAccionDerecha = alCambiarRol) },
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
                    Text("ZONA DE SERVICIO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = HabilioTheme.RojoUrgente, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pénjamo, Gto.", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                TextButton(onClick = alRefrescar) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = HabilioTheme.AmarilloPrimario, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Actualizar", color = HabilioTheme.AmarilloPrimario, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Buscar albañil, plomero, carpintero...", color = HabilioTheme.TextoPlaceholder, fontSize = 14.sp)
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

            Spacer(modifier = Modifier.height(12.dp))

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
                        text = "Perfiles en base de datos: ${trabajadoresFiltrados.size} disponibles cerca de ti",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EstadoCargaOVacio(
                cargando = cargando,
                estaVacio = trabajadoresFiltrados.isEmpty(),
                mensajeVacio = "No hay trabajadores registrados en esta categoría todavía",
                subtituloVacio = "Registra un perfil desde la opción 'Trabajador' para que aparezca dinámicamente en este listado.",
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
    HabilioTarjeta {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            AvatarUsuario(mostrarPuntoDisponible = true)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(especialista.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Perfil verificado",
                        tint = HabilioTheme.AzulVerificado,
                        modifier = Modifier.size(16.dp)
                    )
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
                if (especialista.esPremium) {
                    HabilioBadge(
                        texto = "★ PREMIUM",
                        colorTexto = HabilioTheme.AmarilloPrimario,
                        colorFondo = HabilioTheme.FondoAmarilloSuave
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★", color = HabilioTheme.AmarilloPrimario, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(String.format("%.1f", especialista.calificacion), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("($cantidadResenas)", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
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

        // Miniaturas dinámicas de evidencia con imagen real (AsyncImage) y clic para ampliar
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
                Text("Ver Especialista", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }
}

private fun construirResumenLimpio(esp: PerfilUsuario): String {
    val tallerTexto = if (esp.nombreTaller.isNotBlank()) " (${esp.nombreTaller})" else ""
    return "Maestro especialista en ${esp.oficio}$tallerTexto con servicio garantizado en ${esp.zona}."
}

// ============================================================================
// 11. PANTALLA 7: DETALLE DE ESPECIALISTA (Muestra fotos reales y abre visor)
// ============================================================================
@Composable
fun PantallaDetalleEspecialista(
    especialista: PerfilUsuario,
    evidencias: List<EvidenciaItem>,
    resenas: List<ResenaItem>,
    totalServicios: Int,
    alVolverAlListado: () -> Unit,
    alAbrirModalSolicitar: () -> Unit,
    alAbrirModalCalificar: () -> Unit,
    alClickFotoEvidencia: (EvidenciaItem) -> Unit,
    alIrAExplorar: () -> Unit,
    alIrAMiPerfil: () -> Unit
) {
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
                    texto = "✓ Perfil Verificado",
                    colorTexto = HabilioTheme.AzulVerificado,
                    colorFondo = HabilioTheme.FondoAzulSuave
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HabilioTarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarUsuario(tamano = 64.dp)
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
                    if (especialista.esPremium) {
                        Spacer(modifier = Modifier.width(8.dp))
                        HabilioBadge(
                            texto = "★ PREMIUM",
                            colorTexto = HabilioTheme.TextoSobreAmarillo,
                            colorFondo = HabilioTheme.AmarilloPrimario
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CajaMetricaDetalle(
                        valor = "${String.format("%.1f", especialista.calificacion)} ★",
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
                texto = "Solicitar Servicio ($450 - $1,200 MXN)",
                alClick = alAbrirModalSolicitar
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = alAbrirModalCalificar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "★ Calificar y Recomendar a ${especialista.nombre}",
                    color = HabilioTheme.AmarilloPrimario,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text("Portafolio de Trabajos Reales (${evidencias.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            EstadoCargaOVacio(
                cargando = false,
                estaVacio = evidencias.isEmpty(),
                mensajeVacio = "Este especialista aún no ha subido fotografías a su portafolio."
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
                TextButton(onClick = alAbrirModalCalificar) {
                    Text("+ Escribir reseña", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.AmarilloPrimario)
                }
            }

            EstadoCargaOVacio(
                cargando = false,
                estaVacio = resenas.isEmpty(),
                mensajeVacio = "Sé el primero en calificar y recomendar a ${especialista.nombre}."
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
// 12. PANTALLA 8: PERFIL DEL CLIENTE DINÁMICO
// ============================================================================
@Composable
fun PantallaPerfilCliente(
    cliente: PerfilUsuario?,
    solicitudes: List<SolicitudItem>,
    alVerDetalleTrabajador: (String) -> Unit,
    alLiberarPago: (String) -> Unit,
    alCalificarTrabajador: (String) -> Unit,
    alIrAExplorar: () -> Unit,
    alCambiarRol: () -> Unit
) {
    val totalPedidos = solicitudes.size
    val totalCompletados = solicitudes.count { it.estado == "completado" }
    val enCustodia = solicitudes.filter { it.estado == "pendiente" || it.estado == "en_ejecucion" }
    val montoCustodia = enCustodia.sumOf { it.monto }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AvatarUsuario(tamano = 64.dp, esCircular = true, colorIcono = HabilioTheme.TextoPrimario)
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
                            texto = "✓ Verificado",
                            colorTexto = HabilioTheme.VerdeExito,
                            colorFondo = HabilioTheme.FondoVerdeSuave
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Miembro local • ${cliente?.zona ?: "Pénjamo, Gto."}", fontSize = 13.sp, color = HabilioTheme.TextoSecundario)
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
                    CajaMetricaDetalle("${enCustodia.size}", "$${montoCustodia.toInt()} Custodia", HabilioTheme.AmarilloPrimario, Modifier.weight(1f))
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
                mensajeVacio = "Aún no has solicitado servicios. Explora el catálogo de especialistas para contratar con Garantía Habilio."
            )

            solicitudes.forEach { sol ->
                val completado = sol.estado == "completado"
                val rechazado = sol.estado == "rechazado"
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
                            "en_ejecucion" -> Triple("● En ejecución", HabilioTheme.AmarilloPrimario, HabilioTheme.FondoAmarilloSuave)
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
                                text = if (completado) "✓ Pago liberado al especialista" else "🔒 Garantía en custodia",
                                fontSize = 12.sp,
                                color = HabilioTheme.TextoSecundario
                            )
                            Text(
                                text = "$${String.format("%.2f", sol.monto)} MXN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (completado) HabilioTheme.VerdeExito else HabilioTheme.AmarilloPrimario
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { alVerDetalleTrabajador(sol.trabajadorId) },
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

                        if (!completado && !rechazado) {
                            Button(
                                onClick = { alLiberarPago(sol.id) },
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
                                Text("Liberar Pago", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else if (completado) {
                            OutlinedButton(
                                onClick = { alCalificarTrabajador(sol.trabajadorId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario)
                            ) {
                                Text("★ Evaluar", color = HabilioTheme.AmarilloPrimario, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 13. PANTALLA 9: SOLICITUDES DEL TRABAJADOR DINÁMICAS
// ============================================================================
@Composable
fun PantallaSolicitudesTrabajador(
    trabajador: PerfilUsuario?,
    solicitudes: List<SolicitudItem>,
    cargando: Boolean,
    alRefrescar: () -> Unit,
    alCambiarEstadoSolicitud: (solicitudId: String, nuevoEstado: String) -> Unit,
    alIrAMiTaller: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var filtroSeleccionado by remember { mutableStateOf("Todas") }
    val pendientesCount = solicitudes.count { it.estado == "pendiente" }
    val enCursoCount = solicitudes.count { it.estado == "en_ejecucion" }

    val solicitudesFiltradas = remember(solicitudes, filtroSeleccionado) {
        when (filtroSeleccionado) {
            "Nuevas" -> solicitudes.filter { it.estado == "pendiente" }
            "En curso" -> solicitudes.filter { it.estado == "en_ejecucion" }
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
                    Text("Solicitudes de Trabajo", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
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
                    if (sol.estado == "en_ejecucion" || sol.estado == "completado") {
                        TarjetaSolicitudEnEjecucion(
                            solicitud = sol,
                            alTerminarTrabajo = { alCambiarEstadoSolicitud(sol.id, "completado") }
                        )
                    } else {
                        TarjetaSolicitudNueva(
                            solicitud = sol,
                            alAceptar = { alCambiarEstadoSolicitud(sol.id, "en_ejecucion") },
                            alRechazar = { alCambiarEstadoSolicitud(sol.id, "rechazado") }
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

            HabilioBadge(
                texto = if (solicitud.estado == "rechazado") "Rechazada" else "⏱ Nueva",
                colorTexto = if (solicitud.esUrgente) HabilioTheme.RojoUrgente else HabilioTheme.TextoSecundario,
                colorFondo = if (solicitud.esUrgente) HabilioTheme.FondoRojoSuave else HabilioTheme.SuperficieElevada
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HabilioBadge("🛠️ ${solicitud.oficioEtiqueta}", HabilioTheme.TextoPrimario, HabilioTheme.SuperficieElevada)
            HabilioBadge("✓ Pago Garantizado", HabilioTheme.VerdeExito, HabilioTheme.FondoVerdeSuave)
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
                    Text("PRESUPUESTO OFRECIDO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
                    Text(
                        text = "$${String.format("%.2f", solicitud.monto)} MXN",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = HabilioTheme.AmarilloPrimario
                    )
                }
                Text("🔒 Depósito verificado", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HabilioTheme.VerdeExito)
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
                    Text("Aceptar Solicitud", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
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
    HabilioTarjeta(colorBorde = HabilioTheme.VerdeExito, grosorBorde = 1.5.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AvatarUsuario(
                    tamano = 44.dp,
                    icono = Icons.Outlined.WorkOutline,
                    colorIcono = HabilioTheme.VerdeExito,
                    colorFondo = HabilioTheme.FondoVerdeSuave
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(solicitud.nombreCliente, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("📍 ${solicitud.ubicacion}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                }
            }

            HabilioBadge(
                texto = if (completada) "✓ Completado" else "● En Ejecución",
                colorTexto = HabilioTheme.VerdeExito,
                colorFondo = HabilioTheme.FondoVerdeSuave
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
                    Text("Protegido por Habilio", fontSize = 11.sp, color = HabilioTheme.TextoSecundario)
                    Text(
                        text = "$${String.format("%.2f", solicitud.monto)} MXN",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = HabilioTheme.VerdeExito
                    )
                }
                Text(if (completada) "✓ Pago liberado" else "🔒 En custodia", fontSize = 12.sp)
            }
        }

        if (!completada) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = alTerminarTrabajo,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabilioTheme.VerdeExito,
                    contentColor = HabilioTheme.TextoSobreAmarillo
                )
            ) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Marcar como Terminado y Solicitar Pago", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }
}

// ============================================================================
// 14. PANTALLA 10: MI TALLER DEL TRABAJADOR DINÁMICO
// ============================================================================
@Composable
fun PantallaTallerTrabajador(
    trabajador: PerfilUsuario?,
    evidencias: List<EvidenciaItem>,
    solicitudes: List<SolicitudItem>,
    alPublicarEvidencia: (String, Uri?) -> Unit,
    alClickFotoEvidencia: (EvidenciaItem) -> Unit,
    alIrASolicitudes: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var tituloNuevaEvidencia by remember { mutableStateOf("") }
    var imagenSeleccionada by remember { mutableStateOf<Uri?>(null) }

    val ultimoPagoLiberado = solicitudes.firstOrNull { it.estado == "completado" }

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
            if (ultimoPagoLiberado != null) {
                HabilioTarjeta(
                    colorFondo = HabilioTheme.FondoVerdeSuave,
                    colorBorde = HabilioTheme.VerdeExito.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HabilioBadge("PAGO LIBERADO", HabilioTheme.VerdeExito, Color(0xFF0B1C16))
                        Text("Verificado", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${ultimoPagoLiberado.descripcion} • Cliente ${ultimoPagoLiberado.nombreCliente}",
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+$${String.format("%.2f", ultimoPagoLiberado.monto)} MXN",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HabilioTheme.VerdeExito
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            HabilioTarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(HabilioTheme.SuperficieElevada)
                            .border(2.dp, HabilioTheme.AmarilloPrimario, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = HabilioTheme.TextoPrimario, modifier = Modifier.size(34.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(trabajador?.nombre ?: "Especialista Habilio", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = trabajador?.oficio ?: "Oficio Verificado",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HabilioTheme.AmarilloPrimario
                        )
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
                    Text("Subir Foto de Trabajo Real", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            Text("Fotografía lista (Toca para cambiar)", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
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
                    Text("Subir Evidencia", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Mis Evidencias Publicadas (${evidencias.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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