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
fun PantallaRegistroTrabajador(
    alIrALogin: () -> Unit,
    alRegistroExitoso: (PerfilUsuario) -> Unit,
    alCambiarACliente: () -> Unit,
    alVolverInicio: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Acta #3: lo escrito se guarda como borrador y se recupera si la persona regresa o cierra la app
    val borrador = remember { BorradorRegistro(contexto, "trabajador") }
    var hayBorrador by remember { mutableStateOf(borrador.hayAvance()) }

    var pasoActual by remember { mutableIntStateOf(borrador.entero("paso", 1).coerceIn(1, 4)) }
    val totalPasos = 4

    var nombre by remember { mutableStateOf(borrador.texto("nombre")) }
    var telefono by remember { mutableStateOf(borrador.texto("telefono")) }
    var correo by remember { mutableStateOf(borrador.texto("correo")) }
    var contrasena by remember { mutableStateOf("") }
    var rangoEdad by remember { mutableStateOf(borrador.texto("rangoEdad")) }
    var autorizacionTutor by remember { mutableStateOf(false) }
    var nombreTutor by remember { mutableStateOf(borrador.texto("nombreTutor")) }
    var telefonoTutor by remember { mutableStateOf(borrador.texto("telefonoTutor")) }
    var parentescoTutor by remember { mutableStateOf(borrador.texto("parentescoTutor")) }
    var oficioSeleccionado by remember { mutableStateOf(borrador.texto("oficio")) }
    var especialidad by remember { mutableStateOf(borrador.texto("especialidad")) }
    var zonaServicio by remember { mutableStateOf(borrador.texto("zona").ifBlank { "Pénjamo Centro" }) }
    var modalidadSeleccionada by remember { mutableIntStateOf(borrador.entero("modalidad", 0).coerceIn(0, 2)) }
    var aniosExperiencia by remember { mutableStateOf(borrador.texto("anios")) }
    var disponibilidadSel by remember {
        mutableStateOf(borrador.texto("disponibilidad").split(",").filter { it.isNotBlank() }.toSet())
    }
    var precioDesdeTexto by remember { mutableStateOf(borrador.texto("precio")) }
    var presQueHaces by remember { mutableStateOf(borrador.texto("presHaces")) }
    var presComoAprendiste by remember { mutableStateOf(borrador.texto("presAprendiste")) }
    var presQueDebenSaber by remember { mutableStateOf(borrador.texto("presSaber")) }
    var mujeresEnOficios by remember { mutableStateOf(borrador.texto("mujeres") == "1") }
    var aceptaTerminosYEdad by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    var imagenSeleccionada by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> imagenSeleccionada = uri }

    val esMenorConAutorizacion = rangoEdad == RangoEdad.MENOR_15_17

    LaunchedEffect(nombre, telefono, correo, rangoEdad, nombreTutor, telefonoTutor, parentescoTutor, oficioSeleccionado,
        especialidad, zonaServicio, modalidadSeleccionada, aniosExperiencia, disponibilidadSel, precioDesdeTexto,
        presQueHaces, presComoAprendiste, presQueDebenSaber, mujeresEnOficios, pasoActual) {
        borrador.guardar(
            "nombre" to nombre, "telefono" to telefono, "correo" to correo, "rangoEdad" to rangoEdad,
            "nombreTutor" to nombreTutor, "telefonoTutor" to telefonoTutor, "parentescoTutor" to parentescoTutor,
            "oficio" to oficioSeleccionado, "especialidad" to especialidad, "zona" to zonaServicio,
            "modalidad" to modalidadSeleccionada, "anios" to aniosExperiencia,
            "disponibilidad" to disponibilidadSel.joinToString(","), "precio" to precioDesdeTexto,
            "presHaces" to presQueHaces, "presAprendiste" to presComoAprendiste, "presSaber" to presQueDebenSaber,
            "mujeres" to (if (mujeresEnOficios) "1" else "0"), "paso" to pasoActual
        )
    }

    // En el primer paso, "atrás" del teléfono ya no tira los datos: el borrador queda guardado.
    BackHandler(enabled = pasoActual > 1 && !cargando) {
        mensajeError = null
        pasoActual--
    }

    val pasoValido = when (pasoActual) {
        1 -> rangoEdad.isNotBlank() &&
                rangoEdad != RangoEdad.MENOR_15 &&
                (!esMenorConAutorizacion || (autorizacionTutor && datosTutorValidos(nombreTutor, telefonoTutor, parentescoTutor))) &&
                oficioSeleccionado.isNotBlank() &&
                !(esMenorConAutorizacion && oficioSeleccionado in OFICIOS_DE_RIESGO)
        2 -> zonaServicio.isNotBlank()
        3 -> nombre.isNotBlank() && telefono.filter { it.isDigit() }.length >= 10
        4 -> contrasenaValida(contrasena) && aceptaTerminosYEdad
        else -> false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Registro Guiado",
            subtitulo = "Paso $pasoActual de $totalPasos • Completa tu perfil profesional.",
            textoVolver = if (pasoActual == 1) "Cambiar rol" else "Paso anterior",
            alVolver = {
                if (pasoActual > 1) {
                    mensajeError = null
                    pasoActual--
                } else {
                    alVolverInicio()
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (hayBorrador) {
            AvisoBorradorRecuperado(alEmpezarDeNuevo = {
                borrador.borrar(); hayBorrador = false
                nombre = ""; telefono = ""; correo = ""; rangoEdad = ""; nombreTutor = ""; telefonoTutor = ""
                parentescoTutor = ""; oficioSeleccionado = ""; especialidad = ""; zonaServicio = "Pénjamo Centro"
                modalidadSeleccionada = 0; aniosExperiencia = ""; disponibilidadSel = emptySet(); precioDesdeTexto = ""
                presQueHaces = ""; presComoAprendiste = ""; presQueDebenSaber = ""; mujeresEnOficios = false
                pasoActual = 1
            })
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (pasoActual == 1) {
            SelectorModoAutenticacion(
                esLoginSeleccionado = false,
                alSeleccionarLogin = alIrALogin,
                alSeleccionarRegistro = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Indicador de pasos con nombre: la persona siempre sabe dónde está y cuánto falta
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Edad y oficio", "Zona y horario", "Tu perfil", "Cuenta").forEachIndexed { i, nombrePaso ->
                val n = i + 1
                Text(
                    text = "$n. $nombrePaso",
                    fontSize = 10.sp,
                    fontWeight = if (n == pasoActual) FontWeight.ExtraBold else FontWeight.Normal,
                    color = when {
                        n == pasoActual -> HabilioTheme.AmarilloAcentoTexto
                        n < pasoActual -> HabilioTheme.VerdeExito
                        else -> HabilioTheme.TextoSecundario
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROGRESO DEL REGISTRO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.TextoSecundario
            )
            Text(
                text = "${(pasoActual * 100) / totalPasos}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.AmarilloAcentoTexto
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { pasoActual.toFloat() / totalPasos.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = HabilioTheme.AmarilloPrimario,
            trackColor = HabilioTheme.SuperficieElevada
        )

        Spacer(modifier = Modifier.height(18.dp))

        HabilioTarjeta(paddingInterno = 18.dp) {
            AnimatedContent(
                targetState = pasoActual,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "paso_registro_trabajador"
            ) { paso ->
                val tituloPaso = when (paso) {
                    1 -> "Tu edad y tu oficio"
                    2 -> "¿Dónde y cómo trabajas?"
                    3 -> "Preséntate"
                    else -> "Protege tu cuenta"
                }

                val subtituloPaso = when (paso) {
                    1 -> "Primero dinos tu edad; después elige tu oficio principal para que quienes buscan servicios en tu zona te encuentren."
                    2 -> "Indica las colonias o comunidades que cubres y tu modalidad de atención."
                    3 -> "Una foto real de tu rostro o taller genera mayor confianza en quienes buscan servicios."
                    else -> "Crea tu contraseña y acepta el aviso de privacidad para comenzar."
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = tituloPaso,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HabilioTheme.TextoPrimario
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtituloPaso,
                        fontSize = 13.sp,
                        color = HabilioTheme.TextoSecundario,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    when (paso) {
                        1 -> {
                            SelectorEdadTrabajador(
                                rangoEdad = rangoEdad,
                                alCambiarRango = { nuevo ->
                                    rangoEdad = nuevo
                                    if (nuevo != RangoEdad.MENOR_15_17) autorizacionTutor = false
                                    // Un menor no puede conservar un oficio de riesgo elegido antes
                                    if (nuevo == RangoEdad.MENOR_15_17 && oficioSeleccionado in OFICIOS_DE_RIESGO) {
                                        oficioSeleccionado = ""
                                    }
                                },
                                autorizacionTutor = autorizacionTutor,
                                alCambiarAutorizacion = { autorizacionTutor = it }
                            )

                            if (esMenorConAutorizacion) {
                                CamposAutorizacionTutor(
                                    nombreTutor = nombreTutor, alCambiarNombre = { nombreTutor = it },
                                    telefonoTutor = telefonoTutor, alCambiarTelefono = { telefonoTutor = it },
                                    parentesco = parentescoTutor, alCambiarParentesco = { parentescoTutor = it }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Text("¿Qué oficio desempeñas?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))

                            OFICIOS_HABILIO.chunked(2).forEach { fila ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    fila.forEach { (tituloOficio, subOficio, iconoOficio) ->
                                        val seleccionado = oficioSeleccionado == tituloOficio
                                        val bloqueado = esMenorConAutorizacion && tituloOficio in OFICIOS_DE_RIESGO
                                        Surface(
                                            onClick = { oficioSeleccionado = tituloOficio },
                                            enabled = !bloqueado,
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.SuperficieInput,
                                            border = BorderStroke(
                                                1.dp,
                                                if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(vertical = 4.dp)
                                                .heightIn(min = HabilioTheme.alturaMinima(52.dp))
                                                .alpha(if (bloqueado) 0.5f else 1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = iconoOficio,
                                                    contentDescription = null,
                                                    tint = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.AmarilloAcentoTexto,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = tituloOficio,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoPrimario
                                                    )
                                                    Text(
                                                        text = if (bloqueado) "Desde los 18 años" else subOficio,
                                                        fontSize = 11.sp,
                                                        color = if (seleccionado) HabilioTheme.TextoSobreAmarillo.copy(alpha = 0.8f) else HabilioTheme.TextoSecundario
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            HabilioCampoTexto(
                                etiqueta = "¿Qué trabajos específicos realizas?",
                                etiquetaDerecha = "(Opcional)",
                                valor = especialidad,
                                alCambiarValor = { especialidad = it },
                                placeholder = "Ej. Vitropiso, boiler solar, soldadura..."
                            )

                            val ejemplos = EJEMPLOS_ESPECIALIDAD[oficioSeleccionado].orEmpty()
                            if (ejemplos.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Toca un ejemplo para llenarlo; después puedes cambiarlo.",
                                    fontSize = 12.sp,
                                    color = HabilioTheme.TextoSecundario
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ejemplos.forEach { ejemplo ->
                                        val sel = especialidad.equals(ejemplo, ignoreCase = true)
                                        FilterChip(
                                            selected = sel,
                                            onClick = { especialidad = ejemplo },
                                            label = { Text(ejemplo, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                            modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp)),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = HabilioTheme.FondoAmarilloSuave,
                                                selectedLabelColor = HabilioTheme.AmarilloAcentoTexto,
                                                containerColor = HabilioTheme.SuperficieInput,
                                                labelColor = HabilioTheme.TextoPrimario
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = sel,
                                                borderColor = HabilioTheme.BordeInput,
                                                selectedBorderColor = HabilioTheme.AmarilloPrimario
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            Text("¿En qué zona o colonia trabajas?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))

                            SelectorZonaHabilio(
                                zonaActual = zonaServicio,
                                alElegirZona = { zonaServicio = it }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("¿Cómo atiendes a quienes te contratan?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))

                            MODALIDADES_HABILIO.forEachIndexed { indice, (tituloMod, descMod) ->
                                val activo = modalidadSeleccionada == indice
                                HabilioTarjeta(
                                    radioEsquina = 12.dp,
                                    paddingInterno = 12.dp,
                                    colorFondo = HabilioTheme.SuperficieInput,
                                    colorBorde = if (activo) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput,
                                    alClick = { modalidadSeleccionada = indice },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = activo,
                                            onClick = { modalidadSeleccionada = indice },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = HabilioTheme.AmarilloAcentoTexto,
                                                unselectedColor = HabilioTheme.TextoSecundario
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(tituloMod, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(descMod, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                                        }
                                    }
                                }
                            }

                            SeccionDatosProfesionales(
                                anios = aniosExperiencia, alCambiarAnios = { aniosExperiencia = it },
                                disponibilidad = disponibilidadSel, alCambiarDisponibilidad = { disponibilidadSel = it },
                                precioDesde = precioDesdeTexto, alCambiarPrecio = { precioDesdeTexto = it }
                            )
                        }

                        3 -> {
                            SelectorFotoPerfil(
                                imagenUriLocal = imagenSeleccionada,
                                fotoUrlActual = null,
                                titulo = "Foto de Perfil o Taller",
                                subtitulo = "Toca para elegir una foto desde tu galería (opcional).",
                                iconoPorDefecto = Icons.Default.Handyman,
                                alClickElegirFoto = {
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            HabilioCampoTexto(
                                etiqueta = "¿Cuál es tu nombre completo?",
                                etiquetaDerecha = "Requerido",
                                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                                valor = nombre,
                                alCambiarValor = { nombre = it },
                                placeholder = "Ej. Roberto Méndez",
                                iconoInicial = Icons.Outlined.Person
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HabilioCampoTexto(
                                etiqueta = "Teléfono celular (10 dígitos)",
                                etiquetaDerecha = "Requerido",
                                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                                valor = telefono,
                                alCambiarValor = { telefono = it },
                                placeholder = "469 123 4567",
                                prefijoTexto = "+52"
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            GuiaPresentacionProfesional(
                                oficio = oficioSeleccionado,
                                queHaces = presQueHaces, alCambiarQueHaces = { presQueHaces = it },
                                comoAprendiste = presComoAprendiste, alCambiarComoAprendiste = { presComoAprendiste = it },
                                queDebenSaber = presQueDebenSaber, alCambiarQueDebenSaber = { presQueDebenSaber = it }
                            )
                            OpcionMujeresEnOficios(activa = mujeresEnOficios, alCambiar = { mujeresEnOficios = it })
                        }

                        4 -> {
                            HabilioCampoTexto(
                                etiqueta = "Correo electrónico",
                                etiquetaDerecha = "(Opcional)",
                                valor = correo,
                                alCambiarValor = { correo = it },
                                placeholder = "tucorreo@ejemplo.com",
                                iconoInicial = Icons.Outlined.Email
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HabilioCampoTexto(
                                etiqueta = "Crea una contraseña",
                                etiquetaDerecha = "Mín. 8, letras y números",
                                colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                                valor = contrasena,
                                alCambiarValor = { contrasena = it },
                                placeholder = "••••••••",
                                iconoInicial = Icons.Outlined.Lock,
                                esContrasena = true
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            CheckDeclaracionEdadYPrivacidad(
                                aceptado = aceptaTerminosYEdad,
                                alCambiarAceptado = { aceptaTerminosYEdad = it },
                                esTrabajador = true
                            )
                        }
                    }
                }
            }

            if (mensajeError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = mensajeError!!,
                    color = HabilioTheme.RojoUrgente,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pasoActual > 1) {
                    OutlinedButton(
                        onClick = {
                            mensajeError = null
                            pasoActual--
                        },
                        enabled = !cargando,
                        modifier = Modifier
                            .weight(0.38f)
                            .heightIn(min = HabilioTheme.alturaMinima(52.dp)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta)
                    ) {
                        Text("Anterior", color = HabilioTheme.TextoPrimario, fontWeight = FontWeight.Bold)
                    }
                }

                BotonPrimarioHabilio(
                    texto = when {
                        cargando -> "Registrando..."
                        pasoActual < totalPasos -> "Siguiente"
                        else -> "Finalizar registro"
                    },
                    habilitado = !cargando && pasoValido,
                    mostrarFlecha = pasoActual < totalPasos,
                    modifier = Modifier.weight(if (pasoActual > 1) 0.62f else 1f),
                    alClick = {
                        mensajeError = null
                        if (pasoActual < totalPasos) {
                            pasoActual++
                        } else {
                            if (!aceptaTerminosYEdad) {
                                mensajeError = "Debes aceptar la declaración de edad y el aviso de privacidad."
                                return@BotonPrimarioHabilio
                            }
                            cargando = true
                            scope.launch {
                                var urlFotoPublica: String? = null
                                if (imagenSeleccionada != null) {
                                    urlFotoPublica = SupabaseService.subirFotoStorage(contexto, imagenSeleccionada!!)
                                }

                                val res = SupabaseService.registrarUsuario(
                                    rol = "trabajador",
                                    nombre = nombre,
                                    telefono = telefono,
                                    correoInput = correo,
                                    contrasena = contrasena,
                                    zona = zonaServicio,
                                    direccion = zonaServicio,
                                    oficio = oficioSeleccionado,
                                    especialidad = especialidad,
                                    modalidad = MODALIDADES_HABILIO[modalidadSeleccionada].first,
                                    fotoUrl = urlFotoPublica,
                                    rangoEdad = rangoEdad,
                                    autorizacionTutor = autorizacionTutor
                                )
                                res.onSuccess { perfilNuevo ->
                                    SupabaseService.actualizarDatosProfesionales(
                                        perfilId = perfilNuevo.id,
                                        aniosExperiencia = aniosExperiencia.toIntOrNull() ?: 0,
                                        disponibilidad = disponibilidadSel.joinToString(","),
                                        precioDesde = precioDesdeTexto.toDoubleOrNull() ?: 0.0,
                                        visibleTalentoMujeres = mujeresEnOficios,
                                        descripcion = armarDescripcionProfesional(presQueHaces, presComoAprendiste, presQueDebenSaber)
                                            .ifBlank { null }
                                    )
                                    var perfilFinal = perfilNuevo
                                    if (esMenorConAutorizacion) {
                                        SupabaseService.registrarAutorizacionTutor(perfilNuevo.id, nombreTutor, telefonoTutor, parentescoTutor)
                                        perfilFinal = perfilNuevo.copy(autorizacionEstado = "pendiente")
                                    }
                                    borrador.borrar()
                                    cargando = false
                                    alRegistroExitoso(perfilFinal)
                                }.onFailure {
                                    cargando = false
                                    mensajeError = it.message ?: "Error al registrar la cuenta"
                                }
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TarjetaCambioACliente(alClick = alCambiarACliente)
    }
}
