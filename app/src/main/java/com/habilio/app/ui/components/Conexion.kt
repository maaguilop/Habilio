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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

// ============================================================================================
// ACTA #24: ESTADO DE CONEXIÓN VISIBLE
// ============================================================================================

/**
 * Escucha al sistema (ConnectivityManager) y actualiza [EstadoRed.hayRed] cuando el teléfono gana o pierde
 * internet. Se coloca una sola vez en la raíz de la app y se da de baja sola al salir.
 */
@Composable
fun MonitorDeConexion() {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val cm = contexto.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager
        if (cm == null) {
            onDispose { }
        } else {
            fun hayInternetAhora(): Boolean {
                val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
                return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            EstadoRed.hayRed = hayInternetAhora()
            val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) { EstadoRed.hayRed = true }
                override fun onLost(network: android.net.Network) { EstadoRed.hayRed = hayInternetAhora() }
                override fun onUnavailable() { EstadoRed.hayRed = false }
            }
            try {
                cm.registerDefaultNetworkCallback(callback)
            } catch (_: Exception) { }
            onDispose {
                try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) { }
            }
        }
    }
}

/**
 * Franja superior que explica el estado de la conexión en palabras sencillas:
 *  - sin internet (y si lo que se ve es la copia guardada en el teléfono),
 *  - una carga que falló, con botón para reintentar,
 *  - conexión restablecida,
 *  - actualizando información.
 * Si no hay nada que avisar no ocupa espacio. Se anuncia a lectores de pantalla (liveRegion).
 */
@Composable
fun BannerConexion(
    sinRed: Boolean,
    falloCarga: Boolean,
    mostrandoCopia: Boolean,
    restablecida: Boolean,
    cargando: Boolean,
    alReintentar: () -> Unit
) {
    val (texto, fondo, colorTexto, icono) = when {
        sinRed -> Quad(
            if (mostrandoCopia) "Sin conexión. Estás viendo la última información guardada en tu teléfono."
            else "Sin conexión a internet. Lo que escribas se guarda y podrás enviarlo cuando vuelva la señal.",
            HabilioTheme.FondoRojoSuave, HabilioTheme.RojoUrgente, Icons.Default.WifiOff
        )
        falloCarga -> Quad(
            (EstadoRed.ultimoMensaje ?: "No se pudo actualizar la información.") +
                if (mostrandoCopia) " Se muestra la última copia guardada." else "",
            HabilioTheme.FondoAmarilloSuave, HabilioTheme.AmarilloAcentoTexto, Icons.Default.CloudOff
        )
        restablecida -> Quad(
            "Conexión restablecida. Actualizando…",
            HabilioTheme.FondoVerdeSuave, HabilioTheme.VerdeExito, Icons.Default.Wifi
        )
        cargando -> Quad("", HabilioTheme.FondoPrincipal, HabilioTheme.TextoSecundario, Icons.Default.Sync)
        else -> return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(fondo)
            .statusBarsPadding()
    ) {
        if (texto.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics(mergeDescendants = true) {
                        liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(texto, fontSize = 13.sp, color = colorTexto, lineHeight = 17.sp, modifier = Modifier.weight(1f))
                if (falloCarga && !sinRed) {
                    TextButton(
                        onClick = alReintentar,
                        modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp))
                    ) { Text("Reintentar", fontWeight = FontWeight.Bold, color = colorTexto) }
                }
            }
        }
        if (cargando || restablecida) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .semantics { contentDescription = "Actualizando información" },
                color = HabilioTheme.AmarilloPrimario,
                trackColor = Color.Transparent
            )
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
