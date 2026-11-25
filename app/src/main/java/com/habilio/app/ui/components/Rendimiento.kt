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

// ============================================================================================
// ACTA #23: PAGINACIÓN Y DIAGNÓSTICO DE RENDIMIENTO
// ============================================================================================

/** Especialistas que se muestran por página en la búsqueda. */
const val TAMANO_PAGINA = 10

/** Reseñas que se muestran por página en el perfil de la persona especialista. */
const val TAMANO_PAGINA_RESENAS = 5

/** Botón "Ver más" de una lista paginada. No se muestra si ya se ve todo. */
@Composable
fun BotonVerMas(mostrados: Int, total: Int, alClick: () -> Unit, texto: String = "Ver más resultados") {
    if (mostrados >= total) return
    OutlinedButton(
        onClick = alClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .heightIn(min = HabilioTheme.alturaMinima(48.dp))
    ) {
        Text("$texto (${total - mostrados} más)", color = HabilioTheme.TextoPrimario, fontWeight = FontWeight.SemiBold)
    }
    Text(
        "Mostrando $mostrados de $total",
        fontSize = 12.sp, color = HabilioTheme.TextoSecundario,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        textAlign = TextAlign.Center
    )
}

/**
 * Tarjeta plegable con las mediciones de [MetricasRendimiento]. Pensada para las pruebas del equipo: los valores
 * se copian al informe de rendimiento (docs/RENDIMIENTO.md) y también quedan en Logcat ("HabilioMetricas").
 */
@Composable
fun TarjetaDiagnostico() {
    var abierta by remember { mutableStateOf(false) }
    var filas by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    HabilioTarjeta(alClick = {
        abierta = !abierta
        if (abierta) filas = MetricasRendimiento.resumen()
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = HabilioTheme.AzulVerificado, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Diagnóstico de rendimiento", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HabilioTheme.TextoPrimario)
                Text("Arranque, memoria, fluidez y red de este teléfono", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
            }
            Icon(
                if (abierta) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (abierta) "Ocultar diagnóstico" else "Mostrar diagnóstico",
                tint = HabilioTheme.TextoSecundario
            )
        }
        if (abierta) {
            Spacer(Modifier.height(10.dp))
            filas.forEach { (k, v) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(k, fontSize = 12.sp, color = HabilioTheme.TextoSecundario, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(v, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HabilioTheme.TextoPrimario,
                        textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
            }
            TextButton(
                onClick = { filas = MetricasRendimiento.resumen() },
                modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp))
            ) { Text("Actualizar mediciones", color = HabilioTheme.AzulVerificado, fontWeight = FontWeight.Bold) }
        }
    }
}
