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
fun PantallaInicioRol(
    alElegirCliente: () -> Unit,
    alElegirTrabajador: () -> Unit
) {
    Scaffold(
        topBar = { HabilioTopBar(subtituloUbicacion = null) },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.size(100.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LogoGlobalHabilio(modifier = Modifier.fillMaxSize())
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Habilio", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HabilioBadge(
                texto = "TALENTO LOCAL, A LA VISTA",
                colorTexto = HabilioTheme.AzulVerificado,
                colorFondo = HabilioTheme.FondoAzulSuave
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Demuestra tu oficio con hechos, no con un CV",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Elige cómo deseas ingresar a Habilio para conectar habilidades reales con oportunidades en tu comunidad.",
                fontSize = 14.sp,
                color = HabilioTheme.TextoSecundario,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            TarjetaSeleccionRol(
                titulo = "Cliente",
                descripcion = "Buscar trabajadores por oficio, revisar fotos de sus trabajos y contratar con confianza.",
                icono = Icons.Default.Search,
                colorIcono = HabilioTheme.AmarilloAcentoTexto,
                colorFondoIcono = HabilioTheme.FondoAmarilloSuave,
                alClick = alElegirCliente
            )

            Spacer(modifier = Modifier.height(16.dp))

            TarjetaSeleccionRol(
                titulo = "Trabajador",
                descripcion = "Responder preguntas sencillas, subir fotos de mis trabajos y recibir clientes.",
                icono = Icons.Default.Build,
                colorIcono = HabilioTheme.AzulVerificado,
                colorFondoIcono = HabilioTheme.FondoAzulSuave,
                alClick = alElegirTrabajador
            )
        }
    }
}

@Composable
fun TarjetaSeleccionRol(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    colorIcono: Color,
    colorFondoIcono: Color,
    alClick: () -> Unit
) {
    HabilioTarjeta(paddingInterno = 18.dp, alClick = alClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorFondoIcono),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(descripcion, fontSize = 14.sp, color = HabilioTheme.TextoSecundario, lineHeight = 19.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = HabilioTheme.TextoSecundario
            )
        }
    }
}
