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
fun PantallaSolicitudesTrabajador(
    trabajador: PerfilUsuario?,
    solicitudes: List<SolicitudItem>,
    cargando: Boolean,
    alRefrescar: () -> Unit,
    alCambiarEstadoSolicitud: (solicitud: SolicitudItem, nuevoEstado: String) -> Unit,
    alIrAMiTaller: () -> Unit,
    alCambiarRol: () -> Unit,
    usuarioId: String = "",
    alReportarCliente: (SolicitudItem) -> Unit = {},
    alBloquearCliente: (SolicitudItem) -> Unit = {},
    alAbrirSeguridad: () -> Unit = {}
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
                        color = HabilioTheme.AmarilloAcentoTexto,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = alRefrescar) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar solicitudes", tint = HabilioTheme.AmarilloAcentoTexto)
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
                mensajeVacio = "No tienes solicitudes en esta sección todavía. Cuando alguien solicite tus servicios aparecerán aquí al instante."
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
                    var verAcuerdos by remember(sol.id) { mutableStateOf(false) }
                    run {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { verAcuerdos = true },
                                modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(44.dp))
                            ) { Text("Presupuesto e historial", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.AzulVerificado) }
                            if (sol.estado == "en_ejecucion" || sol.estado == "esperando_confirmacion") {
                                TextButton(
                                    onClick = { alCambiarEstadoSolicitud(sol, "cancelada") },
                                    modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(44.dp))
                                ) { Text("Cancelar servicio", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.RojoUrgente) }
                            }
                        }
                    }
                    if (verAcuerdos) DialogoAcuerdos(sol, "trabajador") { verAcuerdos = false }
                    // Acta #25: seguridad del servicio (reportar, bloquear, aviso a contacto de confianza, 911)
                    if (usuarioId.isNotBlank() && sol.estado != "rechazado" && sol.estado != "cancelada" && sol.estado != "completado") {
                        Spacer(modifier = Modifier.height(8.dp))
                        TarjetaSeguridadServicio(
                            usuarioId = usuarioId,
                            solicitud = sol,
                            otraParte = sol.nombreCliente,
                            esEspecialista = true,
                            alReportar = { alReportarCliente(sol) },
                            alBloquear = { alBloquearCliente(sol) },
                            alAbrirCentro = alAbrirSeguridad
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun TarjetaSolicitudNueva(
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
                    Text("📍 ${ubicacionParaEspecialista(solicitud)}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                }
            }

            val textoEstado = when {
                esCancelada -> "✕ Cancelada"
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

        if (solicitud.fotosProblema.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Fotos del problema", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                solicitud.fotosProblema.forEachIndexed { i, url ->
                    AsyncImage(
                        model = url, contentDescription = "Foto ${i + 1} del problema enviada por la persona que solicita",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput, radioEsquina = 10.dp, paddingInterno = 12.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PRESUPUESTO ORIENTATIVO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
                    Text(
                        text = if (solicitud.monto > 0) "$${String.format("%.2f", solicitud.monto)} MXN" else "Por cotizar",
                        maxLines = 1,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = HabilioTheme.AmarilloAcentoTexto
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
                        .heightIn(min = HabilioTheme.alturaMinima(48.dp)),
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
                        .heightIn(min = HabilioTheme.alturaMinima(48.dp)),
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
fun TarjetaSolicitudEnEjecucion(
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
                    colorIcono = if (esperandoConfirmacion) HabilioTheme.AmarilloAcentoTexto else HabilioTheme.VerdeExito,
                    colorFondo = if (esperandoConfirmacion) HabilioTheme.FondoAmarilloSuave else HabilioTheme.FondoVerdeSuave
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(solicitud.nombreCliente, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("📍 ${ubicacionParaEspecialista(solicitud)}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                }
            }

            HabilioBadge(
                texto = when {
                    completada -> "✓ Completado"
                    esperandoConfirmacion -> "⏳ Esperando confirmación"
                    else -> "● En Ejecución"
                },
                colorTexto = if (esperandoConfirmacion) HabilioTheme.AmarilloAcentoTexto else HabilioTheme.VerdeExito,
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
                        color = if (esperandoConfirmacion) HabilioTheme.AmarilloAcentoTexto else HabilioTheme.VerdeExito
                    )
                }
                Text(
                    text = when {
                        completada -> "✓ Trabajo concluido"
                        esperandoConfirmacion -> "⏳ Por confirmar por el cliente"
                        else -> "🛠️ En proceso"
                    },
                    fontSize = 12.sp,
                    color = if (esperandoConfirmacion) HabilioTheme.AmarilloAcentoTexto else HabilioTheme.TextoPrimario
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
                    .heightIn(min = HabilioTheme.alturaMinima(50.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabilioTheme.VerdeExito,
                    contentColor = HabilioTheme.TextoSobreVerde,
                    disabledContainerColor = HabilioTheme.FondoAmarilloSuave,
                    disabledContentColor = HabilioTheme.AmarilloAcentoTexto
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
                        "Esperando confirmación de quien solicitó"
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
