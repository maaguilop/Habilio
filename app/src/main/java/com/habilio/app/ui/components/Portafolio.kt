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

// ---------------------------------------------------------------------------
// Portafolio: fotos sueltas o conjuntos "Antes / Después" (RF-02)
// ---------------------------------------------------------------------------
@Composable
fun EtiquetaAntesDespues(etiqueta: String, modifier: Modifier = Modifier) {
    when (etiqueta) {
        EtiquetaFoto.ANTES -> HabilioBadge(
            texto = "ANTES",
            colorTexto = HabilioTheme.TextoSecundario,
            colorFondo = HabilioTheme.SuperficieElevada,
            modifier = modifier
        )
        EtiquetaFoto.DESPUES -> HabilioBadge(
            texto = "DESPUÉS",
            colorTexto = HabilioTheme.VerdeExito,
            colorFondo = HabilioTheme.FondoVerdeSuave,
            modifier = modifier
        )
    }
}

@Composable
fun FotoDeTrabajo(
    evidencia: EvidenciaItem,
    alto: Dp,
    alClick: (EvidenciaItem) -> Unit
) {
    val tieneFoto = !evidencia.fotoUrl.isNullOrBlank()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(10.dp))
            .background(HabilioTheme.SuperficieElevada)
            .clickable(enabled = tieneFoto) { alClick(evidencia) },
        contentAlignment = Alignment.BottomEnd
    ) {
        if (tieneFoto) {
            AsyncImage(
                model = evidencia.fotoUrl,
                contentDescription = when (evidencia.etiqueta) {
                    EtiquetaFoto.ANTES -> "Foto de antes: ${evidencia.titulo}"
                    EtiquetaFoto.DESPUES -> "Foto de después: ${evidencia.titulo}"
                    else -> evidencia.titulo
                },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ZoomIn,
                    contentDescription = "Toca para ampliar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = HabilioTheme.AmarilloAcentoTexto,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Tarjeta de un trabajo del portafolio. Si tiene fotos de "Antes" y "Después" las muestra juntas
 * en dos columnas; si no, muestra la foto individual.
 */
@Composable
fun TarjetaTrabajoPortafolio(
    trabajo: TrabajoPortafolio,
    alClickFoto: (EvidenciaItem) -> Unit,
    altoFoto: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    HabilioTarjeta(
        radioEsquina = 14.dp,
        paddingInterno = 10.dp,
        modifier = modifier
    ) {
        if (trabajo.esComparativo) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(trabajo.antes to EtiquetaFoto.ANTES, trabajo.despues to EtiquetaFoto.DESPUES)
                    .forEach { (fotosLado, etiqueta) ->
                        Column(modifier = Modifier.weight(1f)) {
                            EtiquetaAntesDespues(etiqueta)
                            Spacer(modifier = Modifier.height(6.dp))
                            val altoPorFoto = if (fotosLado.size > 1) altoFoto * 0.62f else altoFoto
                            fotosLado.forEachIndexed { indice, foto ->
                                if (indice > 0) Spacer(modifier = Modifier.height(6.dp))
                                FotoDeTrabajo(foto, altoPorFoto, alClickFoto)
                            }
                        }
                    }
            }
        } else {
            trabajo.fotos.forEachIndexed { indice, foto ->
                if (indice > 0) Spacer(modifier = Modifier.height(6.dp))
                FotoDeTrabajo(foto, altoFoto, alClickFoto)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(trabajo.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (trabajo.descripcion.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(trabajo.descripcion, fontSize = 13.sp, lineHeight = 18.sp, color = HabilioTheme.TextoPrimario)
            }
            val detalles = listOf(trabajo.tipoServicio, if (trabajo.fechaTrabajo.isNotBlank()) "Realizado el ${trabajo.fechaTrabajo}" else "")
                .filter { it.isNotBlank() }.joinToString(" • ")
            if (detalles.isNotBlank()) {
                Text(detalles, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
            }
            Spacer(modifier = Modifier.height(4.dp))
            FilaAdaptable(
                inicio = {
                    Text(
                        text = trabajo.fechaPublicacion.replace("Evidencia real • ", "Publicado en "),
                        fontSize = 12.sp,
                        color = HabilioTheme.TextoSecundario,
                        fontWeight = FontWeight.Medium
                    )
                },
                fin = {
                    if (trabajo.esComparativo) {
                        Spacer(modifier = Modifier.width(8.dp))
                        HabilioBadge(
                            texto = "ANTES Y DESPUÉS",
                            colorTexto = HabilioTheme.AzulVerificado,
                            colorFondo = HabilioTheme.FondoAzulSuave
                        )
                    }
                }
            )
        }
    }
}

/** Miniatura de un trabajo para las tarjetas del buscador (RF-02 / RF-03). */
@Composable
fun MiniaturaTrabajo(
    trabajo: TrabajoPortafolio?,
    alClickFoto: (EvidenciaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(forma)
            .background(HabilioTheme.SuperficieElevada)
            .border(1.dp, HabilioTheme.BordeTarjeta, forma),
        contentAlignment = Alignment.Center
    ) {
        when {
            trabajo == null || trabajo.fotos.all { it.fotoUrl.isNullOrBlank() } -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = if (trabajo != null) HabilioTheme.AmarilloAcentoTexto else HabilioTheme.TextoSecundario
                    )
                    Text(
                        text = trabajo?.titulo ?: "Sin foto",
                        fontSize = 10.sp,
                        color = HabilioTheme.TextoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
            trabajo.esComparativo -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    listOf(trabajo.antes.first() to "ANTES", trabajo.despues.first() to "DESPUÉS").forEach { (foto, texto) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(enabled = !foto.fotoUrl.isNullOrBlank()) { alClickFoto(foto) },
                            contentAlignment = Alignment.BottomStart
                        ) {
                            AsyncImage(
                                model = foto.fotoUrl,
                                contentDescription = "$texto: ${foto.titulo}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = texto,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(3.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
            else -> {
                val foto = trabajo.portada
                AsyncImage(
                    model = foto.fotoUrl,
                    contentDescription = foto.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = !foto.fotoUrl.isNullOrBlank()) { alClickFoto(foto) }
                )
            }
        }
    }
}

@Composable
fun OpcionTipoTrabajo(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    seleccionado: Boolean,
    alClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = alClick,
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) HabilioTheme.FondoAmarilloSuave else HabilioTheme.SuperficieInput,
        border = BorderStroke(
            if (seleccionado) 2.dp else 1.dp,
            if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput
        ),
        modifier = modifier.heightIn(min = HabilioTheme.alturaMinima(56.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = HabilioTheme.AmarilloAcentoTexto,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = titulo,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = HabilioTheme.TextoPrimario
            )
            Text(
                text = descripcion,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = HabilioTheme.TextoSecundario,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun FotoElegida(
    uri: Uri,
    descripcion: String,
    alto: Dp,
    alQuitar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(10.dp))
            .background(HabilioTheme.SuperficieElevada),
        contentAlignment = Alignment.TopEnd
    ) {
        AsyncImage(
            model = uri,
            contentDescription = descripcion,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(onClick = alQuitar, modifier = Modifier.size(48.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitar esta foto",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun BotonAgregarFoto(
    texto: String,
    alClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = alClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HabilioTheme.alturaMinima(52.dp)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = HabilioTheme.AmarilloAcentoTexto, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texto, color = HabilioTheme.AmarilloAcentoTexto, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

/** Acta #9: línea con lo medido de la foto (peso antes/después, resolución, giro) y aviso si es de baja resolución. */
@Composable
fun InformeFotoTexto(informe: InformeImagen?) {
    if (informe == null) {
        Text("Analizando foto…", fontSize = 11.sp, color = HabilioTheme.TextoSecundario)
        return
    }
    Text(informe.resumen(), fontSize = 11.sp, color = HabilioTheme.TextoSecundario, lineHeight = 15.sp)
    if (informe.resolucionBaja) {
        Text("Resolución baja: se verá borrosa. Si puedes, elige otra foto.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.RojoUrgente)
    }
}
