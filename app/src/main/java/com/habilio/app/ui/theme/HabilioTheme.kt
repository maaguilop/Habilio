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

object HabilioTheme {
    /** Estado observable: al cambiar, toda la UI que lea estos tokens se recompone. */
    var modoOscuro by mutableStateOf(true)

    /**
     * Factor con el que crecen los botones y controles cuando el usuario sube el tamaño del texto
     * (RF-07). Se actualiza desde HabilioApp. 1.0 = tamaño base.
     */
    var factorControles by mutableFloatStateOf(1f)

    /** Altura mínima de un control táctil, ampliada en proporción al texto (nunca menor a la base). */
    fun alturaMinima(base: Dp): Dp = base * factorControles.coerceAtLeast(1f)

    private fun c(oscuro: Long, claro: Long) = Color(if (modoOscuro) oscuro else claro)

    // ------------------------------------------------------------------
    // MODO OSCURO: sin cambios respecto a la versión anterior.
    // MODO CLARO: base cálida de la paleta de diseño (#FAF8F5 / #FFFFFF / #E8E2D9,
    //             ámbar #F59E0B + #B45309, azul #0284C7 + #0369A1), con los verdes
    //             y rojos del código ajustados para llegar a 4.5:1 como texto.
    // ------------------------------------------------------------------
    val FondoPrincipal get() = c(0xFF0E1013, 0xFFFAF8F5)
    val SuperficieTarjeta get() = c(0xFF181B20, 0xFFFFFFFF)
    val SuperficieInput get() = c(0xFF121418, 0xFFF7F3EC)
    val SuperficieElevada get() = c(0xFF22262E, 0xFFF0EAE0)
    val BordeTarjeta get() = c(0xFF2A2F3A, 0xFFE8E2D9)
    /** Borde de campos de texto: un poco más marcado en claro para que el campo se distinga. */
    val BordeInput get() = c(0xFF2A2F3A, 0xFFB8AD9C)

    val AmarilloPrimario get() = c(0xFFF5B811, 0xFFF59E0B)
    /** Amarillo para texto, estrellas, iconos y enlaces (más oscuro en claro para contraste: 5.0:1). */
    val AmarilloAcentoTexto get() = c(0xFFF5B811, 0xFFB45309)
    val FondoAmarilloSuave get() = c(0xFF332910, 0xFFFEF3C7)
    val TextoSobreAmarillo get() = Color(0xFF111315)

    val VerdeExito get() = c(0xFF12C97D, 0xFF047857)
    val FondoVerdeSuave get() = c(0xFF132A23, 0xFFDCFCE7)
    /** Texto o icono sobre un fondo verde sólido (en claro el verde es oscuro, por eso va blanco). */
    val TextoSobreVerde get() = c(0xFF111315, 0xFFFFFFFF)

    /** Azul para texto (5.9:1 en claro). */
    val AzulVerificado get() = c(0xFF3898FF, 0xFF0369A1)
    /** Azul para iconos, bordes e indicadores (no texto). */
    val AzulIcono get() = c(0xFF3898FF, 0xFF0284C7)
    val FondoAzulSuave get() = c(0xFF132235, 0xFFE0F2FE)

    val RojoUrgente get() = c(0xFFFF6B6B, 0xFFB91C1C)
    val FondoRojoSuave get() = c(0xFF2D181B, 0xFFFEE2E2)

    val TextoPrimario get() = c(0xFFF4F6F8, 0xFF1C1917)
    val TextoSecundario get() = c(0xFFB0B8C4, 0xFF57534E)
    val TextoPlaceholder get() = c(0xFF8A94A6, 0xFF6B645D)

    /** Fondo del indicador de la pestaña activa en la barra inferior. */
    val IndicadorNavegacion get() = c(0xFF181B20, 0xFFFEF3C7)

    /** Sombra de tarjetas: solo en claro, muy suave, para separarlas del fondo cálido. */
    val ElevacionTarjeta: Dp get() = if (modoOscuro) 0.dp else 1.dp

    // Barras del sistema
    private val BARRA_OSCURA = 0xFF0E1013.toInt()
    private val BARRA_CLARA = 0xFFFAF8F5.toInt()

    fun estiloBarraSistema(oscuro: Boolean): SystemBarStyle =
        if (oscuro) SystemBarStyle.dark(BARRA_OSCURA)
        else SystemBarStyle.light(BARRA_CLARA, BARRA_CLARA)
}

object AppConfig {
    var logoGlobal: Any = R.drawable.logo_habilio
    const val PREFS_NAME = "habilio_preferencias"
    const val KEY_ESCALA_TEXTO = "escala_texto"
    const val KEY_APARIENCIA = "apariencia" // "sistema" | "claro" | "oscuro"
}
