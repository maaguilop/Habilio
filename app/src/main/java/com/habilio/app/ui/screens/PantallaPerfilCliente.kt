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
fun PantallaPerfilCliente(
    cliente: PerfilUsuario?,
    solicitudes: List<SolicitudItem>,
    escalaTexto: Float,
    alCambiarEscalaTexto: (Float) -> Unit,
    apariencia: String,
    alCambiarApariencia: (String) -> Unit,
    alEditarPerfil: () -> Unit,
    alVerSeguimientoSolicitud: (SolicitudItem) -> Unit,
    alConfirmarCierreServicio: (SolicitudItem) -> Unit,
    alCalificarTrabajador: (SolicitudItem) -> Unit,
    alIrAExplorar: () -> Unit,
    alCambiarRol: () -> Unit,
    alAbrirSeguridad: () -> Unit = {},
    totalBloqueados: Int = 0
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
                                Icon(Icons.Default.Check, contentDescription = null, tint = HabilioTheme.TextoSobreVerde, modifier = Modifier.size(14.dp))
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
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil", tint = HabilioTheme.AmarilloAcentoTexto, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", color = HabilioTheme.AmarilloAcentoTexto, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

            // Sección de accesibilidad: Tamaño del texto (Modo Cliente)
            SeccionTamanoTexto(
                escalaActual = escalaTexto,
                alCambiarEscala = alCambiarEscalaTexto
            )

            Spacer(modifier = Modifier.height(16.dp))

            SeccionApariencia(
                aparienciaActual = apariencia,
                alCambiarApariencia = alCambiarApariencia
            )

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaSeguridadPerfil(totalBloqueados = totalBloqueados, alAbrirCentro = alAbrirSeguridad)

            Spacer(modifier = Modifier.height(16.dp))

            HabilioTarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = HabilioTheme.AmarilloAcentoTexto, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actividad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CajaMetricaDetalle("$totalPedidos", "Servicios\nPedidos", HabilioTheme.TextoPrimario, Modifier.weight(1f))
                    CajaMetricaDetalle("$totalCompletados", "Trabajos\nHechos", HabilioTheme.VerdeExito, Modifier.weight(1f))
                    CajaMetricaDetalle("${enCurso.size}", "$${montoAcordado.toInt()} Activos", HabilioTheme.AmarilloAcentoTexto, Modifier.weight(1f))
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
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = HabilioTheme.AmarilloAcentoTexto)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Casa Principal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            HabilioBadge("Frecuente", HabilioTheme.AmarilloAcentoTexto, HabilioTheme.FondoAmarilloSuave)
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
                            "esperando_confirmacion" -> Triple("⏳ Por confirmar", HabilioTheme.AmarilloAcentoTexto, HabilioTheme.FondoAmarilloSuave)
                            "en_ejecucion" -> Triple("● En ejecución", HabilioTheme.AmarilloAcentoTexto, HabilioTheme.FondoAmarilloSuave)
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
                                color = if (esperandoConfirmacion) HabilioTheme.AmarilloAcentoTexto else HabilioTheme.TextoSecundario
                            )
                            Text(
                                text = "$${String.format("%.2f", sol.monto)} MXN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = when {
                                    completado -> HabilioTheme.VerdeExito
                                    cancelada || rechazado -> HabilioTheme.TextoSecundario
                                    else -> HabilioTheme.AmarilloAcentoTexto
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
                                .heightIn(min = HabilioTheme.alturaMinima(48.dp)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta)
                        ) {
                            Icon(Icons.Outlined.Visibility, contentDescription = null, tint = HabilioTheme.TextoPrimario, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver detalle", color = HabilioTheme.TextoPrimario, fontSize = 13.sp)
                        }

                        if (!completado && !rechazado && !cancelada) {
                            Button(
                                onClick = { alConfirmarCierreServicio(sol) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = HabilioTheme.alturaMinima(48.dp)),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HabilioTheme.VerdeExito,
                                    contentColor = HabilioTheme.TextoSobreVerde
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
                                    .heightIn(min = HabilioTheme.alturaMinima(48.dp)),
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
                                    color = if (estaEvaluada) HabilioTheme.TextoSecundario else HabilioTheme.AmarilloAcentoTexto,
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
