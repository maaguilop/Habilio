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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

// ============================================================================================
// ACTA #26: REPRESENTACIÓN VISUAL DE MUJERES EN OFICIOS
// Ilustraciones originales dibujadas con código (Canvas de Compose): no usan imágenes de terceros,
// pesan casi nada en el APK y se adaptan al modo claro/oscuro. Muestran mujeres en oficios
// tradicionalmente masculinizados (electricidad, albañilería, plomería, herrería, carpintería)
// con distintos tonos de piel y peinados.
// ============================================================================================

/** Una persona ilustrada: tono de piel, cabello, si lleva casco y el oficio (icono de herramienta). */
data class PersonaIlustrada(
    val oficio: String,
    val icono: ImageVector,
    val piel: Color,
    val cabello: Color,
    val cabelloLargo: Boolean,
    val casco: Color?,
    val ropa: Color
)

val MUJERES_EN_OFICIOS = listOf(
    PersonaIlustrada("Electricista", Icons.Default.Bolt, Color(0xFF8D5524), Color(0xFF1B1B1B), true, Color(0xFFF5B811), Color(0xFF0369A1)),
    PersonaIlustrada("Albañil", Icons.Default.Construction, Color(0xFFC68642), Color(0xFF3B2314), false, Color(0xFFF97316), Color(0xFF57534E)),
    PersonaIlustrada("Plomera", Icons.Default.Plumbing, Color(0xFFE0AC69), Color(0xFF5A3825), true, null, Color(0xFF047857)),
    PersonaIlustrada("Herrera", Icons.Default.Hardware, Color(0xFF6B3E26), Color(0xFF0F0F0F), false, null, Color(0xFF7C2D12)),
    PersonaIlustrada("Carpintera", Icons.Default.Carpenter, Color(0xFFF1C27D), Color(0xFF8B4513), true, null, Color(0xFFB45309))
)

/**
 * Dibuja a una mujer con ropa de trabajo y, en una insignia, la herramienta de su oficio.
 * Es decorativa para el lector de pantalla: el nombre del oficio se escribe debajo como texto.
 */
@Composable
fun IlustracionPersonaOficio(persona: PersonaIlustrada, modifier: Modifier = Modifier, tamano: Dp = 76.dp) {
    val fondo = HabilioTheme.FondoAmarilloSuave
    Box(modifier = modifier.size(tamano)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Fondo circular
            drawCircle(color = fondo, radius = w / 2f, center = center)
            // Cabello largo por detrás de la cabeza
            if (persona.cabelloLargo) {
                drawRoundRect(
                    color = persona.cabello,
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.22f),
                    size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.42f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f, w * 0.18f)
                )
            }
            // Torso con overol (recortado al círculo)
            val torso = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.18f, h * 0.98f)
                cubicTo(w * 0.20f, h * 0.70f, w * 0.34f, h * 0.62f, w * 0.50f, h * 0.62f)
                cubicTo(w * 0.66f, h * 0.62f, w * 0.80f, h * 0.70f, w * 0.82f, h * 0.98f)
                close()
            }
            drawPath(torso, color = persona.ropa)
            // Tirantes del overol
            drawLine(Color.White.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.65f),
                androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.95f), strokeWidth = w * 0.03f)
            drawLine(Color.White.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.65f),
                androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.95f), strokeWidth = w * 0.03f)
            // Cuello y cabeza
            drawRect(persona.piel, topLeft = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.52f),
                size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.12f))
            drawCircle(color = persona.piel, radius = w * 0.15f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.40f))
            // Cabello / fleco (o casco)
            if (persona.casco != null) {
                drawArc(
                    color = persona.casco, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.20f),
                    size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.30f)
                )
                drawRect(persona.casco, topLeft = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.34f),
                    size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.035f))
            } else {
                drawArc(
                    color = persona.cabello, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.34f, h * 0.24f),
                    size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.24f)
                )
                if (!persona.cabelloLargo) {
                    // Chongo alto
                    drawCircle(persona.cabello, radius = w * 0.07f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.22f))
                }
            }
            // Ojos y sonrisa sencilla
            val ojo = Color(0xFF1C1917)
            drawCircle(ojo, radius = w * 0.012f, center = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.41f))
            drawCircle(ojo, radius = w * 0.012f, center = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.41f))
            drawArc(
                color = ojo, startAngle = 20f, sweepAngle = 140f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.42f),
                size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.06f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.012f)
            )
        }
        // Insignia con la herramienta del oficio
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(tamano * 0.36f)
                .clip(CircleShape)
                .background(HabilioTheme.SuperficieTarjeta)
                .border(1.5.dp, HabilioTheme.AmarilloPrimario, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(persona.icono, contentDescription = null, tint = HabilioTheme.AmarilloAcentoTexto, modifier = Modifier.size(tamano * 0.22f))
        }
    }
}

/** Franja "Oficios para todas" con las ilustraciones. Se usa en la pantalla de inicio. */
@Composable
fun SeccionMujeresEnOficios(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Oficios sin etiquetas",
            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = HabilioTheme.TextoPrimario,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            "Cada vez más mujeres trabajan en electricidad, albañilería, plomería y herrería. En Habilio cualquier persona puede mostrar su oficio.",
            fontSize = 13.sp, color = HabilioTheme.TextoSecundario, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MUJERES_EN_OFICIOS.forEach { p ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Ilustración de una mujer ${p.oficio.lowercase()}" }
                ) {
                    IlustracionPersonaOficio(p)
                    Spacer(Modifier.height(6.dp))
                    Text(p.oficio, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HabilioTheme.TextoPrimario)
                }
            }
        }
    }
}
