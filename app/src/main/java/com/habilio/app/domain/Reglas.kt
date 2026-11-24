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

/** RF-07: tres niveles de texto. El nivel "muy grande" equivale al 160 % de RNF-04. */
val NIVELES_TEXTO = listOf(
    Triple("Normal", "100%", 1.00f),
    Triple("Grande", "130%", 1.30f),
    Triple("Muy grande", "160%", 1.60f)
)

fun ajustarEscalaGuardada(valor: Float): Float =
    NIVELES_TEXTO.minByOrNull { abs(it.third - valor) }?.third ?: 1.0f

/** Convierte una fecha ISO de Supabase (UTC) a "23/11/2026 14:05" en la hora del teléfono. */
fun formatearFechaEvento(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val fecha = entrada.parse(iso.take(19)) ?: return iso
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX")).format(fecha)
    } catch (_: Exception) { iso.take(16).replace('T', ' ') }
}

/** Texto en lenguaje claro para cada evento de la línea de tiempo. */
fun textoEvento(e: EventoSolicitud): String {
    val quien = if (e.actorRol == "cliente") "La persona que solicita" else "La persona especialista"
    val extra = if (e.detalle.isNotBlank()) ": ${e.detalle}" else ""
    return when (e.tipo) {
        TipoEvento.CREADA -> "Solicitud enviada"
        TipoEvento.ACEPTADA -> "$quien aceptó el servicio"
        TipoEvento.RECHAZADA -> "$quien rechazó la solicitud"
        TipoEvento.TERMINADA -> "$quien marcó el trabajo como terminado"
        TipoEvento.CONFIRMADA -> "$quien confirmó que el trabajo quedó concluido"
        TipoEvento.CANCELADA -> "$quien canceló$extra"
        TipoEvento.PRESUPUESTO_PROPUESTO -> "$quien propuso un presupuesto$extra"
        TipoEvento.PRESUPUESTO_ACEPTADO -> "$quien aceptó el presupuesto$extra"
        TipoEvento.PRESUPUESTO_RECHAZADO -> "$quien rechazó el presupuesto$extra"
        TipoEvento.DESACUERDO_ABIERTO -> "$quien reportó un desacuerdo$extra"
        TipoEvento.DESACUERDO_RESPONDIDO -> "$quien respondió al desacuerdo"
        TipoEvento.DESACUERDO_RESUELTO -> "El desacuerdo se marcó como resuelto"
        TipoEvento.DESACUERDO_ESCALADO -> "El desacuerdo se envió a revisión del equipo Habilio"
        else -> e.tipo
    }
}

fun calcularInsignias(esp: PerfilUsuario, evidencias: List<EvidenciaItem>, resenas: List<ResenaItem>): List<Insignia> {
    val trabajos = agruparEvidencias(evidencias)
    val perfilCompleto = esp.fotoUrl.isNotBlank() && esp.descripcion.isNotBlank() &&
        esp.especialidad.isNotBlank() && esp.zona.isNotBlank() && esp.disponibilidad.isNotBlank()
    return listOf(
        Insignia("Trabajos publicados", "3 o más trabajos con fotos propias publicados en su portafolio",
            trabajos.size >= 3, Icons.Default.PhotoLibrary),
        Insignia("Antes y después", "Al menos 1 trabajo con foto de antes y de después",
            trabajos.any { it.esComparativo }, Icons.Default.Compare),
        Insignia("Con reseñas de clientes", "3 o más reseñas de servicios concluidos en Habilio",
            resenas.size >= 3, Icons.Default.RateReview),
        Insignia("Valoración alta", "Promedio de 4.5 estrellas o más con al menos 5 reseñas",
            resenas.size >= 5 && esp.calificacion >= 4.5, Icons.Default.Star),
        Insignia("Perfil completo", "Foto, descripción, especialidad, zona y disponibilidad registradas",
            perfilCompleto, Icons.Default.AssignmentTurnedIn)
    )
}

fun datosTutorValidos(nombre: String, telefono: String, parentesco: String) =
    nombre.trim().length >= 3 && telefono.filter { it.isDigit() }.length >= 10 && parentesco.isNotBlank()

fun armarDescripcionProfesional(queHaces: String, comoAprendiste: String, queDebenSaber: String): String =
    listOf(queHaces, comoAprendiste, queDebenSaber).map { it.trim().trimEnd('.') }.filter { it.isNotBlank() }
        .joinToString(". ").let { if (it.isBlank()) "" else "$it." }
