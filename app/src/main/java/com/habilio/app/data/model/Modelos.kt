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
    val fotoUrl: String,
    /** RF-05: "mayor_de_edad" | "15_a_17" (vacío en cuentas anteriores a la v0.2). */
    val rangoEdad: String = "",
    /** Acta #6: años de experiencia declarados (0 = sin dato). */
    val aniosExperiencia: Int = 0,
    /** Acta #6: "hoy" | "esta_semana" | "bajo_cita" (vacío = sin dato). */
    val disponibilidad: String = "",
    /** Acta #6: precio orientativo "desde" por servicio, en MXN (0 = sin dato). */
    val precioDesde: Double = 0.0,
    /** Acta #1 y #26: opción voluntaria para aparecer en el filtro "Talento de mujeres". */
    val visibleTalentoMujeres: Boolean = false,
    /** Acta #4: "" | "pendiente" | "verificada" | "rechazada" (autorización de madre, padre o tutor, 15 a 17 años). */
    val autorizacionEstado: String = ""
)

/** Etiquetas de una foto de portafolio (RF-02). */
object EtiquetaFoto {
    const val UNICA = "unica"
    const val ANTES = "antes"
    const val DESPUES = "despues"
}

/** Una fila de la tabla `evidencias` = una fotografía. Varias fotos con el mismo grupoId forman un trabajo. */
data class EvidenciaItem(
    val id: String,
    val trabajadorId: String,
    val titulo: String,
    val ubicacion: String,
    val fechaPublicacion: String,
    val fotoUrl: String? = null,
    val grupoId: String? = null,
    val etiqueta: String = EtiquetaFoto.UNICA,
    /** Acta #8: datos del trabajo y consentimiento para publicar las imágenes. */
    val descripcion: String = "",
    val tipoServicio: String = "",
    val fechaTrabajo: String = "",
    val consentimientoImagenes: Boolean = false
)

/** Acta #9: lo que se midió de una foto al procesarla (peso, resolución, giro y calidad). */
data class InformeImagen(
    val anchoOriginal: Int, val altoOriginal: Int, val bytesOriginales: Long,
    val anchoFinal: Int, val altoFinal: Int, val bytesFinales: Long,
    val giroGrados: Int, val calidadJpeg: Int
) {
    val ahorroPorcentaje: Int
        get() = if (bytesOriginales > 0) (100 - bytesFinales * 100 / bytesOriginales).toInt().coerceIn(0, 100) else 0
    /** Menos de 640 px en su lado mayor se ve borrosa en pantalla completa. */
    val resolucionBaja: Boolean get() = maxOf(anchoFinal, altoFinal) < 640
    private fun kb(b: Long) = if (b >= 1_048_576) "%.1f MB".format(b / 1_048_576.0) else "${b / 1024} KB"
    fun resumen(): String = buildString {
        append("${kb(bytesOriginales)} → ${kb(bytesFinales)} ($ahorroPorcentaje % menos) • ${anchoFinal}×${altoFinal} px")
        if (giroGrados != 0) append(" • enderezada $giroGrados°")
    }
}

/** Datos de un trabajo nuevo del portafolio (Acta #8). */
data class NuevoTrabajo(
    val titulo: String, val descripcion: String, val tipoServicio: String,
    val fechaTrabajo: String, val consentimientoImagenes: Boolean, val fotos: List<FotoNueva>
)

/** Foto elegida por el trabajador y aún no subida. */
data class FotoNueva(val etiqueta: String, val uri: Uri)

/**
 * Un trabajo del portafolio: una sola foto, o un conjunto "Antes / Después" (hasta 4 fotos en total).
 */
data class TrabajoPortafolio(
    val clave: String,
    val trabajadorId: String,
    val titulo: String,
    val ubicacion: String,
    val fechaPublicacion: String,
    val fotos: List<EvidenciaItem>,
    val descripcion: String = "",
    val tipoServicio: String = "",
    val fechaTrabajo: String = "",
    val consentimientoImagenes: Boolean = false
) {
    val antes: List<EvidenciaItem> get() = fotos.filter { it.etiqueta == EtiquetaFoto.ANTES }
    val despues: List<EvidenciaItem> get() = fotos.filter { it.etiqueta == EtiquetaFoto.DESPUES }
    val esComparativo: Boolean get() = antes.isNotEmpty() && despues.isNotEmpty()
    val portada: EvidenciaItem get() = despues.firstOrNull() ?: fotos.first()
}

const val MAX_FOTOS_POR_TRABAJO = 4

const val MAX_FOTOS_POR_LADO = 2

/**
 * Agrupa las fotos en trabajos. Las filas sin grupo (publicadas antes de la v0.2) cuentan como
 * un trabajo de una sola foto. Respeta el orden de la lista recibida (más reciente primero).
 */
fun agruparEvidencias(evidencias: List<EvidenciaItem>): List<TrabajoPortafolio> {
    val grupos = LinkedHashMap<String, MutableList<EvidenciaItem>>()
    evidencias.forEach { ev -> grupos.getOrPut(ev.grupoId ?: ev.id) { mutableListOf() }.add(ev) }
    return grupos.map { (clave, filas) ->
        // La lista llega de la más nueva a la más vieja: se invierte para conservar el orden de subida
        val cronologicas = filas.reversed()
        val ordenadas = cronologicas.sortedBy { if (it.etiqueta == EtiquetaFoto.DESPUES) 1 else 0 }
        val primera = filas.last()
        TrabajoPortafolio(
            clave = clave,
            trabajadorId = primera.trabajadorId,
            titulo = primera.titulo,
            ubicacion = primera.ubicacion,
            fechaPublicacion = primera.fechaPublicacion,
            fotos = ordenadas.take(MAX_FOTOS_POR_TRABAJO),
            descripcion = primera.descripcion,
            tipoServicio = primera.tipoServicio,
            fechaTrabajo = primera.fechaTrabajo,
            consentimientoImagenes = primera.consentimientoImagenes
        )
    }
}

object RangoEdad {
    const val MAYOR = "mayor_de_edad"
    const val MENOR_15_17 = "15_a_17"
    const val MENOR_15 = "menor_de_15"
}

/** Zonas predefinidas de servicio (sin GPS). */
val ZONAS_HABILIO = listOf("Pénjamo Centro", "Santa Ana Pacueco", "Col. Juárez", "Col. Lindavista")

data class ResenaItem(
    val id: String,
    val trabajadorId: String,
    val autorNombre: String,
    val comentario: String,
    val estrellas: Int,
    val fechaTexto: String,
    /** Acta #14: una reseña oculta por moderación no se muestra ni cuenta en el promedio. */
    val oculta: Boolean = false,
    val solicitudId: String = ""
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
    val oficioTrabajador: String = "Especialista en Habilio",
    /** Acta #10: fotografías del problema que adjuntó la persona que solicita. */
    val fotosProblema: List<String> = emptyList(),
    val creadoEn: String = ""
)

/** Acta #11: una propuesta de presupuesto dentro de una solicitud (el historial completo queda guardado). */
data class PresupuestoItem(
    val id: String,
    val solicitudId: String,
    val monto: Double,
    val descripcion: String,
    val condiciones: String,
    /** "cliente" | "trabajador": quién hizo la propuesta (la otra parte debe aceptarla). */
    val propuestoPorRol: String,
    /** "pendiente" | "aceptado" | "rechazado" | "reemplazado" */
    val estado: String,
    val creadoEn: String
)

/** Tipos de evento del historial de una solicitud (Acta #12 y #13). */
object TipoEvento {
    const val CREADA = "creada"
    const val ACEPTADA = "aceptada"
    const val RECHAZADA = "rechazada"
    const val TERMINADA = "terminada"
    const val CONFIRMADA = "confirmada"
    const val CANCELADA = "cancelada"
    const val PRESUPUESTO_PROPUESTO = "presupuesto_propuesto"
    const val PRESUPUESTO_ACEPTADO = "presupuesto_aceptado"
    const val PRESUPUESTO_RECHAZADO = "presupuesto_rechazado"
    const val DESACUERDO_ABIERTO = "desacuerdo_abierto"
    const val DESACUERDO_RESPONDIDO = "desacuerdo_respondido"
    const val DESACUERDO_RESUELTO = "desacuerdo_resuelto"
    const val DESACUERDO_ESCALADO = "desacuerdo_escalado"
}

/** Un evento del historial. Los eventos con destinatario y sin leer funcionan como avisos dentro de la app. */
data class EventoSolicitud(
    val id: String,
    val solicitudId: String,
    val actorRol: String,
    val tipo: String,
    val detalle: String,
    val destinatarioId: String,
    val leido: Boolean,
    val creadoEn: String
)

/** Acta #12: desacuerdo formal entre las partes de un servicio. */
data class DesacuerdoItem(
    val id: String,
    val solicitudId: String,
    val abiertoPorRol: String,
    val motivo: String,
    val descripcion: String,
    val respuesta: String,
    /** "abierto" | "respondido" | "resuelto" | "escalado" */
    val estado: String,
    val creadoEn: String
)

/**
 * Una insignia SOLO se muestra si se cumple un criterio que cualquiera puede comprobar en la propia app
 * (fotos publicadas, reseñas, datos del perfil). Habilio no verifica identidad ni habilidades de nadie,
 * por eso ya no se usan los textos "Oficio Verificado" ni "Cliente Verificado".
 */
data class Insignia(val nombre: String, val criterio: String, val obtenida: Boolean, val icono: ImageVector)
