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

/**
 * CATÁLOGO DE OFICIOS (Acta #5). Cada oficio trae: nombre, descripción corta, icono, especialidades
 * y sinónimos. Los sinónimos permiten que la búsqueda encuentre "chamba" escrita como la gente la dice
 * ("tubero" -> Plomería, "modista" -> Costura). Los textos usan lenguaje neutral e inclusivo (Acta #26).
 */
data class OficioCatalogo(
    val nombre: String,
    val descripcion: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector,
    val especialidades: List<String>,
    val sinonimos: List<String>,
    /** Oficio que la ley mexicana restringe a personas menores de 18 años (Acta #4). */
    val deRiesgo: Boolean = false
)

val CATALOGO_OFICIOS = listOf(
    OficioCatalogo("Albañilería", "Obra y acabados", Icons.Default.Construction,
        listOf("Pisos y azulejo", "Losas y colados", "Bardas y muros", "Repellado y yeso", "Remodelación"),
        listOf("albañil", "constructor", "maestro de obra", "obra negra", "cemento"), deRiesgo = true),
    OficioCatalogo("Plomería", "Fugas y tubos", Icons.Default.Plumbing,
        listOf("Fugas y llaves", "Boiler y calentadores", "Instalación de tinacos", "Drenaje y destape"),
        listOf("plomero", "plomera", "tubero", "fuga", "agua", "drenaje", "boiler")),
    OficioCatalogo("Electricidad", "110v / 220v", Icons.Default.Bolt,
        listOf("Instalaciones eléctricas", "Apagadores y contactos", "Cableado de casas", "Lámparas y ventiladores"),
        listOf("electricista", "luz", "corto circuito", "cableado", "contacto", "apagador"), deRiesgo = true),
    OficioCatalogo("Carpintería", "Madera y clósets", Icons.Default.Carpenter,
        listOf("Clósets y cocinas", "Puertas y muebles", "Reparación de muebles", "Muebles a medida"),
        listOf("carpintero", "carpintera", "madera", "muebles", "closet", "ebanista")),
    OficioCatalogo("Herrería", "Puertas y rejas", Icons.Default.Hardware,
        listOf("Puertas y rejas", "Portones y barandales", "Soldadura en general", "Estructuras metálicas"),
        listOf("herrero", "herrera", "soldador", "soldadora", "soldadura", "reja", "portón"), deRiesgo = true),
    OficioCatalogo("Pintura", "Interiores y fachadas", Icons.Default.FormatPaint,
        listOf("Interiores", "Fachadas e impermeabilizante", "Acabados y texturas"),
        listOf("pintor", "pintora", "pintar", "impermeabilizar", "brocha")),
    OficioCatalogo("Costura", "Prendas y arreglos", Icons.Default.ContentCut,
        listOf("Arreglos de ropa", "Uniformes", "Cortinas y confección", "Bordado y personalización"),
        listOf("costurera", "costurero", "modista", "sastre", "sastrería", "bordado", "dobladillo")),
    OficioCatalogo("Jardinería", "Plantas y áreas verdes", Icons.Default.Park,
        listOf("Poda y limpieza", "Diseño de jardín", "Sistemas de riego"),
        listOf("jardinero", "jardinera", "podar", "pasto", "plantas", "riego")),
    OficioCatalogo("Limpieza", "Casa, oficina y obra", Icons.Default.CleaningServices,
        listOf("Limpieza profunda", "Limpieza de obra", "Lavado de muebles y tapicería"),
        listOf("aseo", "limpiar", "intendencia", "limpiadora", "limpiador")),
    OficioCatalogo("Reparación de electrodomésticos", "Línea blanca y pequeños", Icons.Default.Build,
        listOf("Lavadoras", "Refrigeradores", "Estufas", "Microondas y licuadoras"),
        listOf("técnico", "técnica", "lavadora", "refri", "refrigerador", "estufa", "electrodoméstico")),
    OficioCatalogo("Cerrajería", "Chapas y llaves", Icons.Default.Key,
        listOf("Cambio de chapas", "Copias de llaves", "Apertura de puertas"),
        listOf("cerrajero", "cerrajera", "chapa", "llave", "candado")),
    OficioCatalogo("Gastronomía y repostería", "Comida por encargo", Icons.Default.Restaurant,
        listOf("Pasteles y postres", "Banquetes y fiestas", "Comida por encargo"),
        listOf("cocinera", "cocinero", "repostera", "repostero", "pastelero", "pastelera", "banquete", "comida")),
    OficioCatalogo("Belleza y cuidado personal", "A domicilio o en local", Icons.Default.Face,
        listOf("Corte y peinado", "Uñas", "Maquillaje"),
        listOf("estilista", "peinado", "uñas", "manicurista", "maquilladora", "maquillador", "barbero", "barbera"))
)

/** Compatibilidad con las pantallas existentes: (nombre, descripción, icono). */
val OFICIOS_HABILIO = CATALOGO_OFICIOS.map { Triple(it.nombre, it.descripcion, it.icono) }

/** Oficios que solo pueden ejercerse desde los 18 años (RF-05 / Acta #4). */
val OFICIOS_DE_RIESGO: Set<String> = CATALOGO_OFICIOS.filter { it.deRiesgo }.map { it.nombre }.toSet()

/** Ejemplos tocables para llenar "¿Qué trabajos específicos realizas?" sin escribir desde cero (RF-06). */
val EJEMPLOS_ESPECIALIDAD: Map<String, List<String>> =
    CATALOGO_OFICIOS.associate { it.nombre to it.especialidades }

fun normalizarBusqueda(t: String): String =
    java.text.Normalizer.normalize(t.lowercase().trim(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

/**
 * Devuelve el oficio al que apunta un texto libre (nombre, especialidad o sinónimo), o null.
 * Ignora acentos y mayúsculas: "plomeria", "Tubero" y "FUGA" encuentran Plomería.
 */
fun oficioPorTexto(texto: String): String? {
    val q = normalizarBusqueda(texto)
    if (q.length < 3) return null
    return CATALOGO_OFICIOS.firstOrNull { o ->
        normalizarBusqueda(o.nombre).contains(q) ||
            o.sinonimos.any { normalizarBusqueda(it).contains(q) || q.contains(normalizarBusqueda(it)) } ||
            o.especialidades.any { normalizarBusqueda(it).contains(q) }
    }?.nombre
}

val MODALIDADES_HABILIO = listOf(
    "🚗 A Domicilio" to "Me traslado a la casa o negocio de quien solicita el servicio en la región.",
    "🏠 En mi Taller o Local" to "Tengo ubicación física.",
    "🔄 Ambos (Taller y a domicilio)" to "Flexibilidad total según lo que requiera quien solicita."
)

val DISPONIBILIDADES_HABILIO = listOf("Entre semana", "Fines de semana", "Mañanas", "Tardes", "Urgencias")

fun textoDisponibilidad(p: PerfilUsuario): String =
    if (p.disponibilidad.isBlank()) "Disponibilidad por confirmar"
    else "● " + p.disponibilidad.split(",").joinToString(" · ") { it.trim() }
