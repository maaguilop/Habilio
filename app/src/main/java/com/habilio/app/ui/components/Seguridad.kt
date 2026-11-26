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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

// ============================================================================================
// ACTA #25: CONFIANZA Y SEGURIDAD DE LAS USUARIAS
// Reporte de conductas inapropiadas, bloqueo de usuarios, canal de atención, protección de datos
// de contacto y protocolo de seguridad para servicios presenciales, con enfoque en la protección
// de mujeres que ofrecen o reciben servicios.
// ============================================================================================

/**
 * Canal de atención de Habilio. IMPORTANTE: el equipo debe reemplazar este correo por la cuenta real que
 * revisará los reportes antes de publicar la app.
 */
const val CORREO_ATENCION_HABILIO = "seguridad@habilio.app"

/** Motivos de reporte de conducta. Los primeros cubren las situaciones de riesgo más frecuentes para mujeres. */
val MOTIVOS_REPORTE_CONDUCTA = listOf(
    "Acoso, insinuaciones o comentarios sexuales",
    "Amenazas, intimidación o violencia",
    "Discriminación o lenguaje ofensivo",
    "Me pidió datos personales, fotos o pagos fuera de lo acordado",
    "No se presentó o el perfil parece falso",
    "Otro motivo"
)

/** Protocolo de seguridad para servicios presenciales. */
val PROTOCOLO_SEGURIDAD = listOf(
    "Mantén la comunicación dentro de Habilio hasta que el servicio sea aceptado.",
    "Comparte los datos del servicio (nombre, zona, fecha) con alguien de confianza antes de la visita.",
    "Si puedes, recibe o realiza el servicio acompañada o con alguien enterado de la hora.",
    "Tu dirección exacta y tu teléfono solo se comparten después de la aceptación, y solo si tú lo permites.",
    "Nunca pagues por adelantado fuera de lo acordado en el presupuesto. Habilio no procesa pagos.",
    "Si algo te incomoda, termina la visita, repórtalo aquí y bloquea a la persona.",
    "En una emergencia llama al 911."
)

@Composable
private fun OpcionMotivo(texto: String, seleccionada: Boolean, alElegir: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HabilioTheme.alturaMinima(48.dp))
            .selectable(selected = seleccionada, onClick = alElegir, role = Role.RadioButton)
    ) {
        RadioButton(
            selected = seleccionada, onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = HabilioTheme.AmarilloAcentoTexto)
        )
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 14.sp, color = HabilioTheme.TextoPrimario, lineHeight = 19.sp)
    }
}

@Composable
private fun AvisoEmergencia() {
    HabilioTarjeta(colorFondo = HabilioTheme.FondoRojoSuave, radioEsquina = 12.dp, paddingInterno = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = HabilioTheme.RojoUrgente, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Si estás en peligro ahora, llama al 911. Habilio revisa los reportes, pero no es un servicio de emergencias.",
                fontSize = 12.sp, lineHeight = 17.sp, color = HabilioTheme.TextoPrimario
            )
        }
    }
}

/** Ventana para reportar una conducta inapropiada. Opcionalmente bloquea a la persona en el mismo paso. */
@Composable
fun DialogoReportarConducta(
    objetivo: ObjetivoReporte,
    alCerrar: () -> Unit,
    alEnviar: (motivo: String, detalle: String, tambienBloquear: Boolean) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("") }
    var tambienBloquear by remember { mutableStateOf(true) }
    val detalleObligatorio = motivo == "Otro motivo"
    val valido = motivo.isNotBlank() && (!detalleObligatorio || detalle.trim().length >= 10)

    DialogoHabilio(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HabilioTarjeta(
            radioEsquina = 24.dp, paddingInterno = 20.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(titulo = "Reportar conducta", icono = Icons.Default.Report, alCerrar = alCerrar)
            Text(
                "Vas a reportar a ${objetivo.nombre}. La otra persona no sabrá quién la reportó.",
                fontSize = 13.sp, color = HabilioTheme.TextoSecundario, lineHeight = 18.sp
            )
            Spacer(Modifier.height(10.dp))
            AvisoEmergencia()
            Spacer(Modifier.height(12.dp))
            Text("¿Qué pasó?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario,
                modifier = Modifier.semantics { heading() })
            Column(modifier = Modifier.selectableGroup()) {
                MOTIVOS_REPORTE_CONDUCTA.forEach { m -> OpcionMotivo(m, motivo == m) { motivo = m } }
            }
            OutlinedTextField(
                value = detalle, onValueChange = { detalle = it.take(800) },
                label = { Text(if (detalleObligatorio) "Cuéntanos qué pasó (obligatorio)" else "Detalles (opcional)") },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HabilioTheme.alturaMinima(48.dp))
                    .toggleable(value = tambienBloquear, onValueChange = { tambienBloquear = it }, role = Role.Checkbox)
            ) {
                Checkbox(
                    checked = tambienBloquear, onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = HabilioTheme.AmarilloPrimario, checkmarkColor = HabilioTheme.TextoSobreAmarillo)
                )
                Spacer(Modifier.width(8.dp))
                Text("También bloquear a esta persona", fontSize = 14.sp, color = HabilioTheme.TextoPrimario)
            }
            Spacer(Modifier.height(12.dp))
            BotonPrimarioHabilio(
                texto = "Enviar reporte",
                habilitado = valido,
                alClick = { alEnviar(motivo, detalle, tambienBloquear) }
            )
            Text(
                "Si no hay conexión, el reporte se guarda en tu teléfono y se envía solo después.",
                fontSize = 11.sp, color = HabilioTheme.TextoSecundario, modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Confirmación antes de bloquear: explica exactamente qué pasa y qué no. */
/** Estados en los que una solicitud sigue abierta entre dos personas. */
val ESTADOS_SOLICITUD_ABIERTA = setOf("pendiente", "en_ejecucion", "esperando_confirmacion")

private fun nombreEstado(estado: String) = when (estado) {
    "pendiente" -> "pendiente"
    "en_ejecucion" -> "en ejecución"
    "esperando_confirmacion" -> "esperando confirmación"
    else -> estado
}

/**
 * Confirmación antes de bloquear. Explica qué pasa y, si hay solicitudes abiertas con esa persona,
 * las enumera y avisa que se van a cerrar, para que la persona confirme sabiendo lo que ocurrirá.
 */
@Composable
fun DialogoConfirmarBloqueo(
    objetivo: ObjetivoReporte,
    quienBloqueaEsEspecialista: Boolean,
    alCerrar: () -> Unit,
    alConfirmar: () -> Unit,
    solicitudesAbiertas: List<SolicitudItem> = emptyList()
) {
    DialogoHabilio(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HabilioTarjeta(
            radioEsquina = 24.dp, paddingInterno = 20.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .heightIn(max = altoMaximoVentana()).verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(titulo = "¿Bloquear a ${objetivo.nombre}?", icono = Icons.Default.Block, alCerrar = alCerrar)

            if (solicitudesAbiertas.isNotEmpty()) {
                HabilioTarjeta(colorFondo = HabilioTheme.FondoRojoSuave, radioEsquina = 12.dp, paddingInterno = 12.dp) {
                    Text(
                        if (solicitudesAbiertas.size == 1) "Se cerrará 1 solicitud abierta con esta persona:"
                        else "Se cerrarán ${solicitudesAbiertas.size} solicitudes abiertas con esta persona:",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(6.dp))
                    solicitudesAbiertas.forEach { s ->
                        Text(
                            "• ${s.oficioEtiqueta.ifBlank { "Servicio" }} (${nombreEstado(s.estado)})",
                            fontSize = 13.sp, lineHeight = 18.sp, color = HabilioTheme.TextoPrimario
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Quedarán como canceladas en el historial. La otra persona verá que la solicitud se cerró, pero no que la bloqueaste.",
                        fontSize = 12.sp, lineHeight = 17.sp, color = HabilioTheme.TextoSecundario
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Text("Además:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario)
            Spacer(Modifier.height(4.dp))
            val efectos = buildList {
                if (quienBloqueaEsEspecialista) {
                    add("No podrá enviarte nuevas solicitudes.")
                } else {
                    add("Dejarás de ver su perfil en las búsquedas.")
                    add("No podrás enviarle solicitudes por error.")
                }
                add("No se le avisará que la bloqueaste.")
                add("Puedes desbloquearla cuando quieras desde el Centro de seguridad (las solicitudes cerradas no se reabren).")
            }
            efectos.forEach { Text("• $it", fontSize = 14.sp, lineHeight = 20.sp, color = HabilioTheme.TextoPrimario) }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = alConfirmar,
                modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(52.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HabilioTheme.RojoUrgente, contentColor = Color.White)
            ) {
                Text(
                    if (solicitudesAbiertas.isEmpty()) "Bloquear"
                    else "Bloquear y cerrar ${if (solicitudesAbiertas.size == 1) "la solicitud" else "las solicitudes"}",
                    fontWeight = FontWeight.ExtraBold
                )
            }
            TextButton(
                onClick = alCerrar,
                modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(48.dp))
            ) { Text("No bloquear", color = HabilioTheme.TextoSecundario) }
        }
    }
}

/**
 * Centro de seguridad: protocolo para servicios presenciales, personas bloqueadas (con opción de desbloquear),
 * canal de atención y qué datos se comparten y cuándo.
 */
@Composable
fun DialogoCentroSeguridad(
    usuarioId: String,
    esEspecialista: Boolean,
    bloqueados: List<UsuarioBloqueado>,
    alDesbloquear: (UsuarioBloqueado) -> Unit,
    alCerrar: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    DialogoHabilio(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HabilioTarjeta(
            radioEsquina = 24.dp, paddingInterno = 20.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(max = altoMaximoVentana())
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(titulo = "Centro de seguridad", icono = Icons.Default.Shield, alCerrar = alCerrar)
            AvisoEmergencia()

            Spacer(Modifier.height(16.dp))
            Text("Antes y durante un servicio", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.TextoPrimario, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(6.dp))
            PROTOCOLO_SEGURIDAD.forEach {
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HabilioTheme.VerdeExito,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = HabilioTheme.TextoPrimario)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Qué datos tuyos se ven", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.TextoPrimario, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(6.dp))
            val datos = if (esEspecialista) listOf(
                "Cualquier persona: nombre, oficio, zona general, portafolio y reseñas.",
                "Solo quien te contrata, después de que aceptes: tu teléfono.",
                "Nunca: tu correo ni tu dirección."
            ) else listOf(
                "La persona especialista ve tu nombre y la zona general del trabajo.",
                "Tu dirección exacta solo se comparte después de que acepte, y solo si tú lo permites.",
                "Nunca: tu correo."
            )
            datos.forEach { Text("• $it", fontSize = 13.sp, lineHeight = 18.sp, color = HabilioTheme.TextoSecundario) }

            Spacer(Modifier.height(16.dp))
            Text("Personas bloqueadas (${bloqueados.size})", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.TextoPrimario, modifier = Modifier.semantics { heading() })
            if (bloqueados.isEmpty()) {
                Text("No has bloqueado a nadie. Puedes hacerlo desde un perfil o desde una solicitud.",
                    fontSize = 13.sp, color = HabilioTheme.TextoSecundario, modifier = Modifier.padding(top = 4.dp))
            }
            bloqueados.forEach { b ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = HabilioTheme.TextoSecundario, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(b.nombre, fontSize = 14.sp, color = HabilioTheme.TextoPrimario, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { alDesbloquear(b) },
                        modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp))
                            .semantics { contentDescription = "Desbloquear a ${b.nombre}" }
                    ) { Text("Desbloquear", color = HabilioTheme.AzulVerificado, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Canal de atención", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.TextoPrimario, modifier = Modifier.semantics { heading() })
            Text(
                "Para reportar algo que no esté ligado a un perfil o solicitud, o dar seguimiento a un reporte, escríbenos. Respondemos en días hábiles.",
                fontSize = 13.sp, lineHeight = 18.sp, color = HabilioTheme.TextoSecundario, modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:$CORREO_ATENCION_HABILIO")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Seguridad Habilio")
                    }
                    try { contexto.startActivity(intent) } catch (_: Exception) { }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(48.dp))
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Escribir a $CORREO_ATENCION_HABILIO", color = HabilioTheme.TextoPrimario, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Tarjeta dentro de una solicitud activa: botón para compartir los datos del servicio con alguien de confianza,
 * y accesos a reportar, bloquear y al Centro de seguridad.
 */
@Composable
fun TarjetaSeguridadServicio(
    usuarioId: String,
    solicitud: SolicitudItem,
    otraParte: String,
    esEspecialista: Boolean,
    alReportar: () -> Unit,
    alBloquear: () -> Unit,
    alAbrirCentro: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    HabilioTarjeta(radioEsquina = 14.dp, paddingInterno = 14.dp, colorFondo = HabilioTheme.SuperficieInput) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = HabilioTheme.AzulVerificado, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tu seguridad en este servicio", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = HabilioTheme.TextoPrimario, modifier = Modifier.semantics { heading() })
        }
        Text(
            "Antes de la visita, comparte los datos del servicio con alguien de confianza.",
            fontSize = 12.sp, lineHeight = 17.sp, color = HabilioTheme.TextoSecundario, modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val zona = if (esEspecialista) ubicacionParaEspecialista(solicitud) else ubicacionParaCliente(solicitud.ubicacion)
                val texto = buildString {
                    append("Voy a ")
                    append(if (esEspecialista) "realizar" else "recibir")
                    append(" un servicio por Habilio.\n")
                    append(if (esEspecialista) "Cliente: " else "Especialista: ").append(otraParte).append('\n')
                    append("Trabajo: ").append(solicitud.oficioEtiqueta).append('\n')
                    append("Zona: ").append(zona).append('\n')
                    if (solicitud.creadoEn.isNotBlank()) append("Solicitud del ").append(formatearFechaEvento(solicitud.creadoEn)).append('\n')
                    append("Te aviso cuando termine.")
                }
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, texto)
                }
                try {
                    contexto.startActivity(android.content.Intent.createChooser(intent, "Compartir datos del servicio"))
                } catch (_: Exception) { }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(48.dp))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Compartir con alguien de confianza", color = HabilioTheme.TextoPrimario, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = alReportar, modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(48.dp))) {
                Text("Reportar", color = HabilioTheme.RojoUrgente, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            TextButton(onClick = alBloquear, modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(48.dp))) {
                Text("Bloquear", color = HabilioTheme.TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            TextButton(onClick = alAbrirCentro, modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(48.dp))) {
                Text("Ayuda", color = HabilioTheme.AzulVerificado, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/** Acceso al Centro de seguridad desde "Mi perfil" o "Mi taller". */
@Composable
fun TarjetaSeguridadPerfil(totalBloqueados: Int, alAbrirCentro: () -> Unit) {
    HabilioTarjeta(alClick = alAbrirCentro) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = HabilioTheme.AzulVerificado, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Seguridad y privacidad", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HabilioTheme.TextoPrimario)
                Text(
                    if (totalBloqueados == 0) "Consejos, datos que compartes y canal de atención"
                    else "$totalBloqueados ${if (totalBloqueados == 1) "persona bloqueada" else "personas bloqueadas"} · consejos y ayuda",
                    fontSize = 12.sp, color = HabilioTheme.TextoSecundario
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = HabilioTheme.TextoSecundario)
        }
    }
}

/** Menú de tres puntos en un perfil: reportar conducta o bloquear. */
@Composable
fun MenuSeguridadUsuario(alReportar: () -> Unit, alBloquear: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val densidadApp = LocalDensity.current
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones: reportar o bloquear", tint = HabilioTheme.TextoPrimario)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            CompositionLocalProvider(LocalDensity provides densidadApp) {
            DropdownMenuItem(
                text = { Text("Reportar conducta") },
                leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) },
                onClick = { abierto = false; alReportar() }
            )
            DropdownMenuItem(
                text = { Text("Bloquear") },
                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                onClick = { abierto = false; alBloquear() }
            )
            }
        }
    }
}

private val PATRON_TELEFONO = Regex("""(\d[\s.-]?){8,}""")
private val PATRON_CORREO = Regex("""[\w.+-]+@[\w-]+\.[\w.]+""")
private val PATRON_DIRECCION = Regex("""(?i)\b(calle|av\.?|avenida|privada|n[uú]m(ero)?\.?|#\s*\d)""")

/**
 * Protección de datos de contacto: si la descripción de una solicitud incluye teléfono, correo o dirección,
 * se advierte que esos datos los vería la persona especialista ANTES de aceptar.
 */
@Composable
fun AvisoDatosDeContacto(texto: String) {
    val tieneDatos = PATRON_TELEFONO.containsMatchIn(texto) || PATRON_CORREO.containsMatchIn(texto) ||
        PATRON_DIRECCION.containsMatchIn(texto)
    if (!tieneDatos) return
    HabilioTarjeta(
        colorFondo = HabilioTheme.FondoAmarilloSuave, radioEsquina = 10.dp, paddingInterno = 10.dp,
        modifier = Modifier.padding(top = 6.dp).semantics {
            liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
        }
    ) {
        Text(
            "Parece que escribiste un teléfono, correo o dirección. Por tu seguridad, quítalos de la descripción: " +
                "tu dirección y teléfono se comparten solo después de que la persona acepte.",
            fontSize = 12.sp, lineHeight = 17.sp, color = HabilioTheme.TextoPrimario
        )
    }
}
