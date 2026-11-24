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
fun PantallaLoginUnificada(
    esModoTrabajador: Boolean,
    alIrARegistro: () -> Unit,
    alLoginExitoso: (PerfilUsuario) -> Unit,
    alCambiarRolOpuesto: () -> Unit,
    alVolverInicio: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var telefonoOCorreo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Te damos la bienvenida a Habilio",
            subtitulo = "Encuentra y contrata a personas de oficios locales que muestran su trabajo real.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = true,
            alSeleccionarLogin = {},
            alSeleccionarRegistro = alIrARegistro
        )

        Spacer(modifier = Modifier.height(20.dp))

        HabilioTarjeta(paddingInterno = 18.dp) {
            HabilioCampoTexto(
                etiqueta = "Teléfono móvil o Correo",
                etiquetaDerecha = "10 dígitos o correo",
                valor = telefonoOCorreo,
                alCambiarValor = { telefonoOCorreo = it },
                placeholder = "469 123 4567 o tu@correo.com",
                iconoInicial = Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(16.dp))
            HabilioCampoTexto(
                etiqueta = "Contraseña",
                valor = contrasena,
                alCambiarValor = { contrasena = it },
                placeholder = "••••••••••••",
                iconoInicial = Icons.Outlined.Lock,
                esContrasena = true
            )

            if (mensajeError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(mensajeError!!, color = HabilioTheme.RojoUrgente, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(22.dp))

            val textoBoton = if (cargando) {
                "Verificando credenciales..."
            } else if (esModoTrabajador) {
                "Continuar como trabajador"
            } else {
                "Continuar como Cliente"
            }

            BotonPrimarioHabilio(
                texto = textoBoton,
                habilitado = !cargando && telefonoOCorreo.isNotBlank() && contrasena.isNotBlank(),
                alClick = {
                    cargando = true
                    mensajeError = null
                    scope.launch {
                        val res = SupabaseService.iniciarSesion(telefonoOCorreo, contrasena)
                        cargando = false
                        res.onSuccess(alLoginExitoso).onFailure {
                            mensajeError = it.message ?: "Error al iniciar sesión"
                        }
                    }
                },
                mostrarFlecha = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (esModoTrabajador) {
            TarjetaCambioACliente(alClick = alCambiarRolOpuesto)
        } else {
            TarjetaCambioATrabajador(alClick = alCambiarRolOpuesto)
        }
    }
}

@Composable
fun TarjetaCambioATrabajador(alClick: () -> Unit) {
    HabilioTarjeta(paddingInterno = 18.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = HabilioTheme.AmarilloAcentoTexto, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("¿Haces trabajos de oficio?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Si eres albañil, plomero, electricista, costurera o carpintero en Guanajuato:",
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = alClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HabilioTheme.alturaMinima(48.dp)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = HabilioTheme.SuperficieElevada,
                    contentColor = HabilioTheme.TextoPrimario
                )
            ) {
                Text("Ingresar como Trabajador", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TarjetaCambioACliente(alClick: () -> Unit) {
    HabilioTarjeta(paddingInterno = 16.dp, alClick = alClick) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("¿Buscas contratar servicios para tu hogar?", fontSize = 13.sp, color = HabilioTheme.TextoSecundario)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ingresar o registrarme como Cliente >",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.AmarilloAcentoTexto
            )
        }
    }
}
