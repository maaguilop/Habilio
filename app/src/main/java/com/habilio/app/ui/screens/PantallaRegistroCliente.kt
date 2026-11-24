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
fun PantallaRegistroCliente(
    alIrALogin: () -> Unit,
    alRegistroExitoso: (PerfilUsuario) -> Unit,
    alCambiarATrabajador: () -> Unit,
    alVolverInicio: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var aceptaTerminosYEdad by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    var imagenSeleccionada by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> imagenSeleccionada = uri }

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
            esLoginSeleccionado = false,
            alSeleccionarLogin = alIrALogin,
            alSeleccionarRegistro = {}
        )

        Spacer(modifier = Modifier.height(18.dp))

        SelectorFotoPerfil(
            imagenUriLocal = imagenSeleccionada,
            fotoUrlActual = null,
            titulo = "Foto de Perfil",
            subtitulo = "Toca aquí para elegir una imagen desde tu galería (se comprime automáticamente).",
            iconoPorDefecto = Icons.Default.Person,
            alClickElegirFoto = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        HabilioTarjeta(paddingInterno = 18.dp) {
            HabilioCampoTexto(
                etiqueta = "Nombre Completo",
                etiquetaDerecha = "Requerido",
                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                valor = nombre,
                alCambiarValor = { nombre = it },
                placeholder = "Ej. Juan Pérez o Familia López",
                iconoInicial = Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Teléfono móvil (10 dígitos)",
                etiquetaDerecha = "Requerido",
                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                valor = telefono,
                alCambiarValor = { telefono = it },
                placeholder = "469 123 4567",
                iconoInicial = Icons.Outlined.Phone
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Correo electrónico",
                etiquetaDerecha = "Requerido",
                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                valor = correo,
                alCambiarValor = { correo = it },
                placeholder = "tu@correo.com",
                iconoInicial = Icons.Outlined.Email
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Crear Contraseña",
                valor = contrasena,
                alCambiarValor = { contrasena = it },
                placeholder = "Mínimo 6 caracteres",
                iconoInicial = Icons.Outlined.Lock,
                esContrasena = true
            )

            if (mensajeError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(mensajeError!!, color = HabilioTheme.RojoUrgente, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(14.dp))

            CheckDeclaracionEdadYPrivacidad(
                aceptado = aceptaTerminosYEdad,
                alCambiarAceptado = { aceptaTerminosYEdad = it },
                esTrabajador = false
            )

            Spacer(modifier = Modifier.height(18.dp))

            BotonPrimarioHabilio(
                texto = if (cargando) "Creando cuenta..." else "Crear Cuenta de Cliente",
                habilitado = !cargando &&
                        nombre.isNotBlank() &&
                        telefono.filter { it.isDigit() }.length >= 10 &&
                        contrasena.length >= 6 &&
                        aceptaTerminosYEdad,
                alClick = {
                    if (!aceptaTerminosYEdad) {
                        mensajeError = "Debes aceptar la declaración de edad y el aviso de privacidad."
                        return@BotonPrimarioHabilio
                    }
                    cargando = true
                    mensajeError = null
                    scope.launch {
                        var urlFotoPublica: String? = null
                        if (imagenSeleccionada != null) {
                            urlFotoPublica = SupabaseService.subirFotoStorage(contexto, imagenSeleccionada!!)
                        }

                        val res = SupabaseService.registrarUsuario(
                            rol = "cliente",
                            nombre = nombre,
                            telefono = telefono,
                            correoInput = correo,
                            contrasena = contrasena,
                            fotoUrl = urlFotoPublica
                        )
                        cargando = false
                        res.onSuccess(alRegistroExitoso).onFailure {
                            mensajeError = it.message ?: "Error al registrar cliente"
                        }
                    }
                },
                mostrarFlecha = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        TarjetaCambioATrabajador(alClick = alCambiarATrabajador)
    }
}
