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
fun PantallaSeguimientoSolicitud(
    solicitud: SolicitudItem,
    especialista: PerfilUsuario?,
    alVolver: () -> Unit,
    alRefrescar: () -> Unit,
    alCancelarPedido: () -> Unit,
    alConfirmarCierre: () -> Unit,
    alCalificar: () -> Unit,
    alVerPerfilTrabajador: () -> Unit,
    usuarioId: String = "",
    alReportarEspecialista: () -> Unit = {},
    alBloquearEspecialista: () -> Unit = {},
    alAbrirSeguridad: () -> Unit = {}
) {
    // Acta #25: el teléfono de la persona especialista solo se pide y se muestra cuando el servicio fue aceptado
    var telefonoServicio by remember(solicitud.id, solicitud.estado) { mutableStateOf("") }
    LaunchedEffect(solicitud.id, solicitud.estado) {
        if (puedeVerContacto(solicitud.estado) && telefonoServicio.isBlank()) {
            telefonoServicio = SupabaseService.obtenerTelefonoParaServicio(solicitud.trabajadorId).orEmpty()
        }
    }
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
                TextButton(onClick = alVolver, modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp))) {
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
                    colorTexto = HabilioTheme.AmarilloAcentoTexto,
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
                    esRechazado -> "Solicitud rechazada por la persona especialista"
                    pasoActual == 1 -> "Esperando confirmación del especialista"
                    esperandoConfirmacion -> "Especialista finalizó • Esperando tu confirmación"
                    pasoActual == 2 -> "Trabajo en ejecución"
                    else -> "¡Servicio concluido con éxito!"
                }
                val subtituloEstado = when {
                    esCancelada -> "Cancelaste esta solicitud. El registro se conserva en tu historial de servicios."
                    esRechazado -> "La persona especialista no pudo atender esta solicitud en este momento. Puedes buscar a otra persona disponible."
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
                        else -> HabilioTheme.AmarilloAcentoTexto
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
                            titulo = "Servicio cancelado",
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
                            titulo = "3. Trabajo concluido y confirmado",
                            subtitulo = when {
                                pasoActual >= 3 -> "Servicio finalizado y confirmado satisfactoriamente"
                                esperandoConfirmacion -> "Presiona 'Confirmar que el servicio fue completado' abajo"
                                else -> "Se completa cuando quien solicitó confirma el cierre del trabajo"
                            },
                            completado = pasoActual >= 3,
                            activo = pasoActual == 3 || esperandoConfirmacion,
                            mostrarLineaInferior = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HabilioTarjeta {
                Text(
                    text = "📦 DETALLES DE LA SOLICITUD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HabilioTheme.AmarilloAcentoTexto
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
                            Text(solicitud.oficioTrabajador, fontSize = 13.sp, color = HabilioTheme.AmarilloAcentoTexto)
                            if (puedeVerContacto(solicitud.estado)) {
                                if (telefonoServicio.isNotBlank()) {
                                    Text("📱 +52 $telefonoServicio", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                                }
                            } else {
                                Text("🔒 El teléfono se muestra cuando se acepta el servicio", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
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
                Text("📍 ${ubicacionParaCliente(solicitud.ubicacion)}", fontSize = 14.sp)

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = HabilioTheme.BordeTarjeta)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MONTO ORIENTATIVO", fontSize = 11.sp, color = HabilioTheme.TextoSecundario, fontWeight = FontWeight.Bold)
                        Text("Monto orientativo. El monto final se acuerda en la sección de presupuesto y se paga directamente a la persona especialista (Habilio no procesa pagos).", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                    Text(
                        text = "$${String.format("%.2f", solicitud.monto)} MXN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HabilioTheme.AmarilloAcentoTexto
                    )
                }
            }

            if (solicitud.fotosProblema.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HabilioTarjeta {
                    Text("Fotos del problema que enviaste", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        solicitud.fotosProblema.forEachIndexed { i, url ->
                            AsyncImage(
                                model = url, contentDescription = "Foto ${i + 1} del problema",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            }

            if (usuarioId.isNotBlank() && solicitud.estado != "rechazado" && solicitud.estado != "cancelada") {
                Spacer(modifier = Modifier.height(12.dp))
                TarjetaSeguridadServicio(
                    usuarioId = usuarioId,
                    solicitud = solicitud,
                    otraParte = solicitud.nombreTrabajador,
                    esEspecialista = false,
                    alReportar = alReportarEspecialista,
                    alBloquear = alBloquearEspecialista,
                    alAbrirCentro = alAbrirSeguridad
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            SeccionAcuerdosYHistorial(solicitud = solicitud, rol = "cliente")

            Spacer(modifier = Modifier.height(20.dp))

            if (!esRechazado && !esCancelada && pasoActual < 3) {
                if (pasoActual == 2) {
                    Button(
                        onClick = alConfirmarCierre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = HabilioTheme.alturaMinima(52.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HabilioTheme.VerdeExito,
                            contentColor = HabilioTheme.TextoSobreVerde
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
                        .heightIn(min = HabilioTheme.alturaMinima(52.dp)),
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
fun PasoRastreoPedido(
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
                    completado -> Icon(Icons.Default.Check, contentDescription = null, tint = HabilioTheme.TextoSobreVerde, modifier = Modifier.size(16.dp))
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
