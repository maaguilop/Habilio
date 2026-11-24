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
    alIrAMiPerfil: () -> Unit,
    alReportarResena: ((ResenaItem) -> Unit)? = null
) {
    val tieneEvidencia = evidencias.isNotEmpty()
    val trabajos = remember(evidencias) { agruparEvidencias(evidencias) }

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
                TextButton(onClick = alVolverAlListado, modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = HabilioTheme.TextoPrimario, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Volver al listado", color = HabilioTheme.TextoPrimario, fontSize = 14.sp)
                }

                HabilioBadge(
                    texto = if (tieneEvidencia) "Con trabajos publicados" else "Perfil registrado",
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
                        Text(textoDisponibilidad(especialista), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (especialista.disponibilidad.isBlank()) HabilioTheme.TextoSecundario else HabilioTheme.VerdeExito)
                        Text(especialista.nombre, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = especialista.oficio,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HabilioTheme.AmarilloAcentoTexto
                        )
                        Text(especialista.zona, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CajaMetricaDetalle(
                        valor = if (resenas.isNotEmpty()) "${String.format("%.1f", especialista.calificacion)} ★" else "Nuevo",
                        etiqueta = "${resenas.size} reseñas",
                        colorValor = HabilioTheme.AmarilloAcentoTexto,
                        modifier = Modifier.weight(1f)
                    )
                    CajaMetricaDetalle(
                        valor = "$totalServicios",
                        etiqueta = "Servicios",
                        colorValor = HabilioTheme.VerdeExito,
                        modifier = Modifier.weight(1f)
                    )
                    CajaMetricaDetalle(
                        valor = "${trabajos.size}",
                        etiqueta = "Trabajos",
                        colorValor = HabilioTheme.AzulVerificado,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HabilioTarjeta(
                colorFondo = HabilioTheme.FondoAzulSuave.copy(alpha = 0.6f),
                colorBorde = HabilioTheme.AzulIcono.copy(alpha = 0.3f)
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
                            color = HabilioTheme.AmarilloAcentoTexto
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CajaMetricaDetalle(
                    valor = if (especialista.aniosExperiencia > 0) "${especialista.aniosExperiencia} años" else "Sin dato",
                    etiqueta = "De experiencia (la persona lo declara)",
                    colorValor = HabilioTheme.TextoPrimario,
                    modifier = Modifier.weight(1f)
                )
                CajaMetricaDetalle(
                    valor = if (especialista.precioDesde > 0) "Desde $${especialista.precioDesde.toInt()}" else "A cotizar",
                    etiqueta = "Precio orientativo",
                    colorValor = HabilioTheme.AmarilloAcentoTexto,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            PanelInsignias(especialista, evidencias, resenas)

            Spacer(modifier = Modifier.height(16.dp))

            BotonPrimarioHabilio(
                texto = "Solicitar Servicio",
                alClick = alAbrirModalSolicitar
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text("Portafolio de trabajos reales (${trabajos.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            EstadoCargaOVacio(
                cargando = false,
                estaVacio = trabajos.isEmpty(),
                mensajeVacio = "Este trabajador aún no ha subido fotografías a su portafolio."
            )

            trabajos.forEach { trabajo ->
                TarjetaTrabajoPortafolio(
                    trabajo = trabajo,
                    alClickFoto = alClickFotoEvidencia,
                    altoFoto = 170.dp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
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
                        Text("+ Escribir reseña", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.AmarilloAcentoTexto)
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
                    fecha = r.fechaTexto,
                    alReportar = alReportarResena?.let { f -> { f(r) } }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun CajaMetricaDetalle(
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
fun EtiquetaCompleta(texto: String, modifier: Modifier = Modifier) {
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
fun TarjetaResena(
    autor: String, comentario: String, estrellas: Int, fecha: String,
    alReportar: (() -> Unit)? = null
) {
    HabilioTarjeta(radioEsquina = 14.dp, paddingInterno = 14.dp) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = autor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "★".repeat(estrellas.coerceIn(1, 5)), color = HabilioTheme.AmarilloAcentoTexto, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = comentario, fontSize = 13.sp, fontStyle = FontStyle.Italic, lineHeight = 19.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = fecha, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
        if (alReportar != null) {
            TextButton(onClick = alReportar, modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(40.dp))) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(14.dp), tint = HabilioTheme.TextoSecundario)
                Spacer(Modifier.width(4.dp))
                Text("Reportar reseña", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
            }
        }
    }
}
