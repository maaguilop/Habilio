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

@Composable
fun TituloSeccion(texto: String, icono: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)) {
        Icon(icono, contentDescription = null, tint = HabilioTheme.AmarilloAcentoTexto, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = HabilioTheme.TextoPrimario)
    }
}

@Composable
fun OpcionRadio(texto: String, seleccionada: Boolean, alElegir: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(44.dp)).clickable { alElegir() }
    ) {
        RadioButton(
            selected = seleccionada, onClick = alElegir,
            colors = RadioButtonDefaults.colors(selectedColor = HabilioTheme.AmarilloAcentoTexto)
        )
        Text(texto, fontSize = 14.sp, color = HabilioTheme.TextoPrimario, modifier = Modifier.padding(start = 4.dp))
    }
}

/**
 * Sección completa de seguimiento formal de un servicio. Se carga sola desde Supabase y la usan las dos partes:
 *  1) línea de tiempo con fecha y hora (Acta #12), 2) presupuesto con aceptación de ambas partes (Acta #11),
 *  3) gestión formal de desacuerdos (Acta #12).
 * [rol] es "cliente" o "trabajador".
 */
@Composable
fun SeccionAcuerdosYHistorial(solicitud: SolicitudItem, rol: String) {
    val scope = rememberCoroutineScope()
    var eventos by remember { mutableStateOf<List<EventoSolicitud>>(emptyList()) }
    var presupuestos by remember { mutableStateOf<List<PresupuestoItem>>(emptyList()) }
    var desacuerdos by remember { mutableStateOf<List<DesacuerdoItem>>(emptyList()) }
    var refresco by remember { mutableIntStateOf(0) }
    var cargando by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }

    LaunchedEffect(solicitud.id, solicitud.estado, refresco) {
        cargando = true
        eventos = SupabaseService.obtenerEventos(solicitud.id)
        presupuestos = SupabaseService.obtenerPresupuestos(solicitud.id)
        desacuerdos = SupabaseService.obtenerDesacuerdos(solicitud.id)
        cargando = false
    }

    val servicioAbierto = solicitud.estado !in listOf("completado", "cancelada", "rechazado")

    Column(modifier = Modifier.fillMaxWidth()) {
        // ---------------- Línea de tiempo ----------------
        TituloSeccion("Historial del servicio", Icons.Default.Timeline)
        HabilioTarjeta(radioEsquina = 14.dp, paddingInterno = 14.dp) {
            val inicial = if (eventos.none { it.tipo == TipoEvento.CREADA })
                listOf("Solicitud enviada" to formatearFechaEvento(solicitud.creadoEn)) else emptyList()
            val filas = inicial + eventos.map { textoEvento(it) to formatearFechaEvento(it.creadoEn) }
            if (cargando && filas.isEmpty()) {
                Text("Cargando historial…", fontSize = 13.sp, color = HabilioTheme.TextoSecundario)
            } else if (filas.isEmpty()) {
                Text("Todavía no hay movimientos.", fontSize = 13.sp, color = HabilioTheme.TextoSecundario)
            }
            filas.forEachIndexed { i, (texto, fecha) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Box(
                        modifier = Modifier.padding(top = 5.dp).size(10.dp).clip(CircleShape)
                            .background(if (i == filas.lastIndex) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(texto, fontSize = 14.sp, color = HabilioTheme.TextoPrimario, lineHeight = 19.sp)
                        if (fecha.isNotBlank()) Text(fecha, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                }
            }
        }

        // ---------------- Presupuesto ----------------
        TituloSeccion("Presupuesto acordado", Icons.Default.RequestQuote)
        AvisoPagosHabilio()
        val aceptado = presupuestos.lastOrNull { it.estado == "aceptado" }
        val pendiente = presupuestos.lastOrNull { it.estado == "pendiente" }
        HabilioTarjeta(radioEsquina = 14.dp, paddingInterno = 14.dp) {
            if (aceptado != null) {
                Text("Acordado por ambas partes", fontSize = 12.sp, color = HabilioTheme.VerdeExito, fontWeight = FontWeight.Bold)
                Text("$${"%.2f".format(aceptado.monto)} MXN", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = HabilioTheme.TextoPrimario)
                Text(aceptado.descripcion, fontSize = 13.sp, color = HabilioTheme.TextoPrimario)
                Text("Condiciones: ${aceptado.condiciones}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
            } else {
                Text(
                    if (solicitud.monto > 0) "Monto orientativo de la solicitud: $${"%.2f".format(solicitud.monto)} MXN (aún sin acuerdo)"
                    else "Todavía no hay un presupuesto acordado.",
                    fontSize = 13.sp, color = HabilioTheme.TextoSecundario
                )
            }
            if (pendiente != null) {
                Spacer(Modifier.height(10.dp))
                HabilioTarjeta(colorFondo = HabilioTheme.FondoAmarilloSuave, radioEsquina = 12.dp, paddingInterno = 12.dp) {
                    val propia = pendiente.propuestoPorRol == rol
                    Text(if (propia) "Tu propuesta está esperando respuesta" else "Tienes una propuesta por responder",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario)
                    Text("$${"%.2f".format(pendiente.monto)} MXN", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = HabilioTheme.AmarilloAcentoTexto)
                    Text(pendiente.descripcion, fontSize = 13.sp, color = HabilioTheme.TextoPrimario)
                    Text("Condiciones: ${pendiente.condiciones}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    if (!propia && servicioAbierto) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        SupabaseService.responderPresupuesto(solicitud, pendiente, rol, true)
                                            .onFailure { mensaje = it.message ?: "No se pudo aceptar." }
                                        refresco++
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(48.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = HabilioTheme.AmarilloPrimario, contentColor = HabilioTheme.TextoSobreAmarillo)
                            ) { Text("Aceptar", fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        SupabaseService.responderPresupuesto(solicitud, pendiente, rol, false)
                                        refresco++
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(48.dp))
                            ) { Text("Rechazar", color = HabilioTheme.TextoPrimario) }
                        }
                    }
                }
            }

            if (servicioAbierto && (solicitud.estado != "pendiente" || rol == "trabajador")) {
                var formularioAbierto by remember { mutableStateOf(false) }
                var monto by remember { mutableStateOf("") }
                var desc by remember { mutableStateOf("") }
                var cond by remember { mutableStateOf("") }
                Spacer(Modifier.height(10.dp))
                if (!formularioAbierto) {
                    OutlinedButton(
                        onClick = { formularioAbierto = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(48.dp))
                    ) { Text(if (aceptado != null) "Proponer un cambio de presupuesto" else "Proponer presupuesto", color = HabilioTheme.TextoPrimario) }
                } else {
                    if (aceptado != null) {
                        Text("Un cambio de presupuesto solo vale si la otra parte lo acepta; mientras tanto sigue vigente el monto acordado.",
                            fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                        Spacer(Modifier.height(6.dp))
                    }
                    OutlinedTextField(monto, { monto = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Monto total (MXN)") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(desc, { desc = it }, label = { Text("¿Qué incluye?") }, minLines = 2,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(cond, { cond = it }, label = { Text("Condiciones (materiales, pago, tiempos)") }, minLines = 2,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio())
                    Spacer(Modifier.height(10.dp))
                    BotonPrimarioHabilio(
                        texto = "Enviar propuesta",
                        habilitado = monto.toDoubleOrNull()?.let { it > 0 } == true,
                        alClick = {
                            scope.launch {
                                SupabaseService.proponerPresupuesto(solicitud, rol, monto.toDouble(), desc, cond)
                                    .onSuccess { formularioAbierto = false; monto = ""; desc = ""; cond = ""; mensaje = "" }
                                    .onFailure { mensaje = it.message ?: "No se pudo enviar." }
                                refresco++
                            }
                        }
                    )
                }
            }

            if (presupuestos.size > 1) {
                Spacer(Modifier.height(10.dp))
                Text("Propuestas anteriores", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
                presupuestos.dropLast(if (pendiente != null || aceptado != null) 1 else 0).forEach {
                    Text("• $${"%.2f".format(it.monto)} MXN — ${it.estado} (${formatearFechaEvento(it.creadoEn)})",
                        fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                }
            }
        }

        // ---------------- Desacuerdos ----------------
        TituloSeccion("Desacuerdos", Icons.Default.Gavel)
        HabilioTarjeta(radioEsquina = 14.dp, paddingInterno = 14.dp) {
            if (desacuerdos.isEmpty()) {
                Text("No hay desacuerdos registrados. Si algo no sale como se acordó, puedes reportarlo aquí en lugar de cancelar.",
                    fontSize = 13.sp, color = HabilioTheme.TextoSecundario, lineHeight = 18.sp)
            }
            desacuerdos.forEach { d ->
                var respuesta by remember(d.id) { mutableStateOf("") }
                HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput, radioEsquina = 12.dp, paddingInterno = 12.dp,
                    modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(d.motivo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario, modifier = Modifier.weight(1f))
                        HabilioBadge(
                            texto = d.estado.uppercase(),
                            colorTexto = if (d.estado == "resuelto") HabilioTheme.VerdeExito else HabilioTheme.AmarilloAcentoTexto,
                            colorFondo = if (d.estado == "resuelto") HabilioTheme.FondoVerdeSuave else HabilioTheme.FondoAmarilloSuave
                        )
                    }
                    Text(d.descripcion, fontSize = 13.sp, color = HabilioTheme.TextoPrimario, modifier = Modifier.padding(top = 4.dp))
                    Text("Abierto por ${if (d.abiertoPorRol == "cliente") "quien solicitó" else "la persona especialista"} • ${formatearFechaEvento(d.creadoEn)}",
                        fontSize = 11.sp, color = HabilioTheme.TextoSecundario)
                    if (d.respuesta.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Respuesta de la otra parte: ${d.respuesta}", fontSize = 13.sp, color = HabilioTheme.TextoPrimario)
                    }
                    val esperaMiRespuesta = d.estado == "abierto" && d.abiertoPorRol != rol
                    if (esperaMiRespuesta) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(respuesta, { respuesta = it }, label = { Text("Tu respuesta") }, minLines = 2,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio())
                        Spacer(Modifier.height(6.dp))
                        BotonPrimarioHabilio("Enviar respuesta", habilitado = respuesta.trim().length >= 10, alClick = {
                            scope.launch { SupabaseService.actualizarDesacuerdo(solicitud, d, rol, "respondido", respuesta); refresco++ }
                        })
                    }
                    if (d.estado == "abierto" || d.estado == "respondido") {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { scope.launch { SupabaseService.actualizarDesacuerdo(solicitud, d, rol, "resuelto"); refresco++ } },
                                modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(44.dp))
                            ) { Text("Ya se resolvió", fontSize = 12.sp, color = HabilioTheme.TextoPrimario) }
                            OutlinedButton(
                                onClick = { scope.launch { SupabaseService.actualizarDesacuerdo(solicitud, d, rol, "escalado"); refresco++ } },
                                modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(44.dp))
                            ) { Text("Pedir revisión", fontSize = 12.sp, color = HabilioTheme.TextoPrimario) }
                        }
                    }
                }
            }

            val hayAbierto = desacuerdos.any { it.estado == "abierto" || it.estado == "respondido" }
            if (solicitud.estado in listOf("en_ejecucion", "esperando_confirmacion", "completado") && !hayAbierto) {
                var formulario by remember { mutableStateOf(false) }
                var motivo by remember { mutableStateOf(SupabaseService.MOTIVOS_DESACUERDO.first()) }
                var detalle by remember { mutableStateOf("") }
                if (!formulario) {
                    OutlinedButton(onClick = { formulario = true }, modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(48.dp))) {
                        Text("Reportar un desacuerdo", color = HabilioTheme.TextoPrimario)
                    }
                } else {
                    Text("¿Qué pasó?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario)
                    SupabaseService.MOTIVOS_DESACUERDO.forEach { m -> OpcionRadio(m, motivo == m) { motivo = m } }
                    OutlinedTextField(detalle, { detalle = it }, label = { Text("Cuéntalo con detalle (mín. 20 caracteres)") }, minLines = 3,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio())
                    Spacer(Modifier.height(8.dp))
                    BotonPrimarioHabilio("Enviar desacuerdo", habilitado = detalle.trim().length >= 20, alClick = {
                        scope.launch {
                            SupabaseService.abrirDesacuerdo(solicitud, rol, motivo, detalle)
                                .onSuccess { formulario = false; detalle = ""; mensaje = "" }
                                .onFailure { mensaje = it.message ?: "No se pudo registrar." }
                            refresco++
                        }
                    })
                }
            }
        }

        if (mensaje.isNotBlank()) {
            Text(mensaje, color = HabilioTheme.RojoUrgente, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}


/**
 * Aviso de pagos: Habilio NO procesa pagos. Distingue presupuesto orientativo, pago acordado y servicio concluido
 * para que nadie interprete que la plataforma cobra, retiene o libera dinero.
 */
@Composable
fun AvisoPagosHabilio() {
    HabilioTarjeta(radioEsquina = 14.dp, paddingInterno = 12.dp) {
        Text("Cómo funcionan los pagos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario)
        Spacer(Modifier.height(4.dp))
        Text("Habilio no procesa pagos: no cobra, no retiene ni transfiere dinero.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
        Spacer(Modifier.height(4.dp))
        Text("• Presupuesto orientativo: referencia inicial, no es un cobro.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
        Text("• Pago acordado: monto que ambas partes aceptan aquí; se paga directamente a la persona especialista.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
        Text("• Servicio concluido: se marca al confirmar el cierre del trabajo; no implica que Habilio haya recibido ni liberado dinero.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
    }
    Spacer(Modifier.height(8.dp))
}
